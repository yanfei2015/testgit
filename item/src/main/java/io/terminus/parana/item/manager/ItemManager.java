/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package io.terminus.parana.item.manager;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import io.terminus.common.exception.ServiceException;
import io.terminus.parana.common.util.Iters;
import io.terminus.parana.item.dao.mysql.ItemDao;
import io.terminus.parana.item.dao.mysql.ItemDetailDao;
import io.terminus.parana.item.dao.mysql.ItemSnapshotDao;
import io.terminus.parana.item.dao.mysql.ShipFeeDao;
import io.terminus.parana.item.dao.mysql.SkuDao;
import io.terminus.parana.item.dao.mysql.SkuPriceDao;
import io.terminus.parana.item.dao.redis.ItemDescriptionRedisDao;
import io.terminus.parana.item.dao.redis.ItemTagDao;
import io.terminus.parana.item.dto.ItemDto;
import io.terminus.parana.item.dto.SkuWithLvPrice;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.ItemDetail;
import io.terminus.parana.item.model.ItemSnapshot;
import io.terminus.parana.item.model.ShipFee;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.item.model.SkuPrice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.terminus.common.utils.Arguments.notNull;
import static io.terminus.parana.common.util.ServiceUtils.checkResult;

/**
 * 商品管理
 * Author: haolin
 * On: 9/8/14
 */
@Component @Slf4j
public class ItemManager {

    @Autowired
    private ItemDao itemDao;

    @Autowired
    private ItemDetailDao itemDetailDao;

    @Autowired
    private ShipFeeDao shipFeeDao;

    @Autowired
    private SkuDao skuDao;

    @Autowired
    private SkuPriceDao skuPriceDao;

    @Autowired
    private ItemTagDao itemTagDao;

    @Autowired
    private ItemDescriptionRedisDao itemDescriptionRedisDao;

    @Autowired
    private ItemSnapshotDao itemSnapshotDao;

    /**
     * 创建商品详情
     * @param itemDto 商品详情
     */
    @Transactional
    public void create(ItemDto itemDto){
        Item item = itemDto.getItem();
        // create item
        itemDao.create(item);
        Long itemId = item.getId();
        // create item detail
        itemDto.getItemDetail().setItemId(itemId);
        itemDetailDao.create(itemDto.getItemDetail());
        // create ship fee
        itemDto.getShipFee().setItemId(itemId);
        shipFeeDao.create(itemDto.getShipFee());
        // create skus
        List<SkuWithLvPrice> skus = Iters.nullToEmpty(itemDto.getSkus());
        int cnt = 0;
        for (SkuWithLvPrice sku : skus){
            Sku toCreate = sku.getSku();
            toCreate.setItemId(itemId);
            toCreate.setShopId(item.getShopId());
            toCreate.setStatus(Item.Status.from(item.getStatus()));
            if (Strings.isNullOrEmpty(toCreate.getSkuCode())) {
                toCreate.setSkuCode("SKU-" + itemId + "-" + cnt);
                ++ cnt;
            }
//            toCreate.setImage(item.getMainImage());
            skuDao.create(toCreate);
            for (SkuPrice price : Iters.nullToEmpty(sku.getPrices())) {
                price.setItemId(itemId);
                price.setSkuId(toCreate.getId());
                skuPriceDao.create(price);
            }
        }
        postInsert(itemDto);
        if (Iters.emptyToNull(itemDto.getTags()) != null) {
            // 发商品时选定分类
            itemTagDao.addTags2Items(item.getUserId(), itemDto.getTags(), Lists.newArrayList(itemId));
        } else {
            // put item into unknown tag
            itemTagDao.addItem2unKnownTag(item.getUserId(), itemId);
        }
    }

    protected void postInsert(ItemDto itemDto) {
        // to be extended
    }

    /**
     * 更新商品详情
     * @param itemDto 商品详情
     */
    @Transactional
    public void update(ItemDto itemDto) {
        Item exist = itemDao.load(itemDto.getItem().getId());
        // 创建宝贝快照
        try {
            createSnapshot(exist);
        } catch (Exception e) {
            log.error("fail to create snapshot of item:{}, cause:{}",
                    itemDto.getItem(), Throwables.getStackTraceAsString(e));
        }

        // update item
        itemDao.update(itemDto.getItem());
        // update item detail
        ItemDetail existDetail = itemDetailDao.findByItemId(itemDto.getItem().getId());
        itemDto.getItemDetail().setId(checkNotNull(existDetail, "item detail not exist").getId());
        itemDto.getItemDetail().setItemId(null);
        itemDetailDao.update(itemDto.getItemDetail());
        // update ship fee
        ShipFee shipFee = shipFeeDao.findByItemId(itemDto.getItem().getId());
        if (shipFee != null && shipFee.getId() != null) {
            itemDto.getShipFee().setId(shipFee.getId());
            itemDto.getShipFee().setItemId(null);
            shipFeeDao.update(itemDto.getShipFee());
        }
        // update skus
        for (SkuWithLvPrice sku : Iters.nullToEmpty(itemDto.getSkus())){
            Sku toUpdate = sku.getSku();
            toUpdate.setItemId(null);
            toUpdate.setShopId(null);
            if (Strings.isNullOrEmpty(toUpdate.getSkuCode())) {
                toUpdate.setSkuCode(null);
            }
            skuDao.update(toUpdate);
            for (SkuPrice price : sku.getPrices()) {
                price.setItemId(itemDto.getItem().getId());
                price.setSkuId(toUpdate.getId());
                if (price.getLv() >= 0) {
                    SkuPrice priceExist = skuPriceDao.findBySkuIdWithLevel(price.getSkuId(), price.getLv());
                    if (priceExist == null) {
                        skuPriceDao.create(price);
                    } else {
                        price.setId(priceExist.getId());
                        boolean success = skuPriceDao.updateById(price);
                        if (!success) {
                            log.warn("price(={}) not update success", price);
                        }
                    }
                }
            }

        }
        exist = itemDao.load(itemDto.getItem().getId());
        if (Objects.equal(exist.getStockQuantity(), 0) && Objects.equal(exist.getStatus(), Item.Status.ON_SHELF.value())) {
            setStatuses(Arrays.asList(itemDto.getItem().getId()), Item.Status.OFF_SHELF.value());
        }
        postUpdate(itemDto);
        if (itemDto.getTags() != null) {
            // 清空所有分类
            itemTagDao.removeTagsOfItem(exist.getUserId(), exist.getId());
            if (itemDto.getTags().isEmpty()) {
                // put item into unkonwn tag
                itemTagDao.addItem2unKnownTag(exist.getUserId(), exist.getId());
            } else {
                // 指定分类
                itemTagDao.addTags2Items(exist.getUserId(), itemDto.getTags(), Lists.newArrayList(exist.getId()));
            }
        }
    }

    protected void postUpdate(ItemDto itemDto) {
        // to be extended
    }

    public ItemSnapshot loadItemSnapshot(Long itemId, Date date) {
        ItemSnapshot snapshot = itemSnapshotDao.loadByDate(itemId, date);
        if (snapshot == null) {
            Item item = itemDao.load(itemId);
            checkState(notNull(item), "item.not.found");

            snapshot = new ItemSnapshot();
            snapshot.setItemId(item.getId());
            snapshot.setItemName(item.getName());
            snapshot.setSellerId(item.getUserId());
            snapshot.setMainImage(item.getMainImage());
        }

        return snapshot;
    }

    private void createSnapshot(Item item) {
        ItemSnapshot snapshot = new ItemSnapshot();
        snapshot.setItemId(item.getId());
        snapshot.setItemName(item.getName());
        snapshot.setSellerId(item.getUserId());
        snapshot.setMainImage(item.getMainImage());
        // todo 塞入图片
        snapshot.setDetail(null);

        itemSnapshotDao.create(snapshot);
    }

    /**
     * 更新商品销量及SKU库存
     * @param itemId 商品id
     * @param skuId SKU.id
     * @param quantity 更新量, 可正可负
     * @return 是否售罄下架
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public boolean updateItemSaleAndStock(long itemId, long skuId, int quantity) throws ServiceException {
        if (quantity == 0) {
            return false;
        }
        Sku sku = skuDao.load(skuId);
        checkResult(sku != null, "sku.not.exist");
        checkResult(sku.getItemId() == itemId, "sku.item.id.invalid");
        if (quantity > 0) {
            checkResult(sku.getStockQuantity() >= quantity, "sku.stock.quantity.not.enough");
        }
        boolean shouldOffShelf = false;
        Item item = itemDao.load(itemId);
        checkResult(item != null, "item.not.exist");
        if (quantity > 0) {
            checkResult(item.getStockQuantity() >= quantity, "sku.stock.quantity.not.enough");
            if (item.getStockQuantity() == quantity) {
                shouldOffShelf = true;
            }
        }
        skuDao.updateStockQuantity(skuId, quantity);
        itemDao.updateSaleQuantity(itemId, quantity);
        if (shouldOffShelf && Objects.equal(item.getStatus(), Item.Status.ON_SHELF.value())) {
            setStatuses(Lists.newArrayList(itemId), Item.Status.OFF_SHELF.value());
        }
        return shouldOffShelf;
    }

    /**
     * 删除商品
     * @param userId 当前用户id
     * @param itemId 商品id
     */
    @Transactional
    public Item delete(Long userId, Long itemId) {
        Item item = itemDao.load(itemId);
        if (item == null){
            log.warn("item(id={}) isn't exist.", itemId);
            throw new ServiceException("item.not.exist");
        }
        if (!Objects.equal(userId, item.getUserId())) {
            log.warn("user(id={}) isn't owner of item({})", userId, item);
            throw new ServiceException("user.not.owner");
        }
        boolean phz = Objects.equal(0, item.getSaleQuantity());
        if (phz) {
            // 没有发生过交易, 物理删除
            itemDao.delete(itemId);
            itemDetailDao.deleteByItemId(itemId);
            shipFeeDao.deleteByItemId(itemId);
            skuDao.deleteByItemId(itemId);
            itemDescriptionRedisDao.del(itemId);
        } else {
            // 有过交易, 逻辑删除
            setStatuses(Lists.newArrayList(itemId), Item.Status.DELETED.value());
        }
        postDelete(itemId, phz);
        // 清除tag
        itemTagDao.removeTagsOfItem(userId, itemId);
        return item;
    }

    protected void postDelete(Long itemId, boolean phz) {
        // to be extended
    }

    /**
     * 批量删除商品
     * @param userId 商家id
     * @param itemIds 商品id列表
     */
    @Transactional
    public void deletes(Long userId, List<Long> itemIds){
        for (Long itemId : itemIds){
            delete(userId, itemId);
        }
    }

    @Transactional
    public void setStatuses(List<Long> itemIds, Integer status) {
        setStatues(itemIds, Item.Status.from(status));
    }

    @Transactional
    public void setStatues(List<Long> itemIds, Item.Status status) {
        if (Iters.emptyToNull(itemIds) == null || status == null) return;

        itemDao.setStatuses(itemIds, status.value());
        skuDao.setStatus(itemIds, status);
    }

    public void frozenByShopIds(List<Long> shopIds) {
        itemDao.frozenByShopIds(shopIds);
    }

    public void unFrozenByShopIds(List<Long> shopIds) {
        itemDao.unFrozenByShopIds(shopIds);
    }
}
