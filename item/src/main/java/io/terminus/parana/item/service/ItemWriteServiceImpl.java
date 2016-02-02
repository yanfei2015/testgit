/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package io.terminus.parana.item.service;

import com.google.common.base.*;
import com.google.common.base.Objects;
import com.google.common.collect.*;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.pampas.common.BaseUser;
import io.terminus.pampas.common.Response;
import io.terminus.pampas.design.dao.ItemCustomRedisDao;
import io.terminus.parana.category.dao.mysql.SpuDao;
import io.terminus.parana.category.dao.redis.AttributeKeyDao;
import io.terminus.parana.category.dao.redis.AttributeValueDao;
import io.terminus.parana.category.dto.AttributeKeyValues;
import io.terminus.parana.category.dto.SpuImportData;
import io.terminus.parana.category.model.AttributeKey;
import io.terminus.parana.category.model.AttributeValue;
import io.terminus.parana.category.model.BackCategory;
import io.terminus.parana.category.model.Spu;
import io.terminus.parana.category.service.BrandReadService;
import io.terminus.parana.category.service.CategoryReadService;
import io.terminus.parana.category.service.CategoryWriteService;
import io.terminus.parana.common.event.CoreEventDispatcher;
import io.terminus.parana.common.util.*;
import io.terminus.parana.config.center.ConfigCenter;
import io.terminus.parana.config.constants.image.ImageSettings;
import io.terminus.parana.event.item.*;
import io.terminus.parana.item.dao.mysql.*;
import io.terminus.parana.item.dao.redis.ItemDescriptionRedisDao;
import io.terminus.parana.item.dao.redis.ItemRedisDao;
import io.terminus.parana.item.dao.redis.ItemTagDao;
import io.terminus.parana.item.dto.*;
import io.terminus.parana.item.event.ItemCountEvent;
import io.terminus.parana.item.event.ItemEventDispatcher;
import io.terminus.parana.item.ext.ItemTypeSorter;
import io.terminus.parana.item.manager.ItemManager;
import io.terminus.parana.item.manager.SkuManager;
import io.terminus.parana.item.model.*;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.model.UserImage;
import io.terminus.parana.user.service.UserImageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.terminus.parana.common.util.ServiceUtils.checkResult;

/**
 * 商品写服务实现
 * Author: haolin
 * On: 8/28/14
 */
@Service @Slf4j
public class ItemWriteServiceImpl implements ItemWriteService {

    @Autowired
    private BrandDao brandDao;

    @Autowired
    private SkuDao skuDao;

    @Autowired
    private SkuManager skuManager;

    @Autowired
    private ItemTagDao itemTagDao;

    @Autowired
    private ItemDao itemDao;

    @Autowired
    private ItemRedisDao itemRedisDao;

    @Autowired
    private ItemTemplateDao itemTemplateDao;

    @Autowired
    private ItemManager itemManager;

    @Autowired
    @Deprecated
    private ItemEventDispatcher itemEventDispatcher;

    @Autowired
    private CoreEventDispatcher coreEventDispatcher;

    @Autowired
    private AttributeKeyDao attributeKeyDao;

    @Autowired
    private AttributeValueDao attributeValueDao;

    @Autowired
    private ItemDescriptionRedisDao itemDescriptionRedisDao;

    @Autowired
    private ItemCustomRedisDao itemCustomRedisDao;
    
    @Autowired
    private ItemTypeSorter itemTypeSorter;

    @Autowired
    private FavoriteItemDao favoriteItemDao;

    @Autowired
    private CategoryWriteService categoryWriteService;

    @Autowired
    private SpuDao spuDao;

    @Autowired
    private CategoryReadService categoryReadService;

    @Autowired
    private BrandReadService brandReadService;

    @Autowired
    private ShopReadService shopReadService;

    @Autowired
    private UserImageService userImageService;

    @Autowired
    private ConfigCenter configCenter;

    private static final JsonMapper JSON_NON_DEFAULT_MAPPER = JsonMapper.nonDefaultMapper();

    @Override
    public Response<Boolean> addTags2Items(BaseUser user, List<String> tags, List<Long> itemIds) {
        Response<Boolean> resp = new Response<Boolean>();
        try {
            itemTagDao.addTags2Items(user.getId(), tags, itemIds);
            coreEventDispatcher.publish(new ItemInfoEvent(itemIds));
            resp.setResult(Boolean.TRUE);
        } catch (Exception e){
            log.error("failed to add tags({}) to items(ids={}), cause: {}",
                    tags, itemIds, Throwables.getStackTraceAsString(e));
            resp.setError("item.tags.add.fail");
        }
        return resp;
    }

    @Override
    public Response<Boolean> removeTagsOfItems(BaseUser user, List<String> tags, List<Long> itemIds) {
        Response<Boolean> resp = new Response<Boolean>();
        try {
            itemTagDao.removeTagsOfItems(user.getId(), tags, itemIds);
            coreEventDispatcher.publish(new ItemInfoEvent(itemIds));
            resp.setResult(Boolean.TRUE);
        } catch (Exception e){
            log.error("failed to remove tags({}) of items(id={}), cause: {}", tags, itemIds);
            resp.setError("item.tags.remove.fail");
        }
        return resp;
    }

    @Override
    public Response<Void> smashTags(long userId, List<String> tags) {
        try {
            Set<Long> itemIds = new HashSet<>();
            for (String tag : Iters.nullToEmpty(tags)) {
                itemIds.addAll(itemTagDao.removeTag(userId, tag));
            }
            if (itemIds.size() > 0) {
                coreEventDispatcher.publish(new ItemInfoEvent(itemIds));
            }
            return Response.ok();
        } catch (Exception e) {
            log.error("smash tags failed, shopId={}, tags={}, cause:{}",
                    userId, tags, Throwables.getStackTraceAsString(e));
            return Response.fail("item.tags.remove.fail");
        }
    }

    @Override
    public Response<Boolean> createSkus(long itemId, List<Sku> skus) {
        try {
            if (Iters.emptyToNull(skus) == null) {
                log.warn("skus empty or null, itemId={}, nothing to create", itemId);
                return Response.ok(Boolean.TRUE);
            }
            Item toUpdate = getItemToUpdateWithSkuWash(itemId, skus);
            skuManager.creates(toUpdate, skus);

            coreEventDispatcher.publish(new ItemInfoEvent(itemId));
            return Response.ok(Boolean.TRUE);
        } catch (ServiceException e) {
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("bulk create skus failed, itemId={}, skus={}, cause:{}",
                    itemId, skus, Throwables.getStackTraceAsString(e));
            return Response.fail("sku.create.fail");
        }
    }

    @Override
    public Response<Boolean> updateSkus(long itemId, List<Sku> skus) {
        if (Iters.emptyToNull(skus) == null) {
            return Response.ok(Boolean.TRUE);
        }
        // TODO(Effet): 目前先代理到以前接口
        for (Sku sku : skus) {
            if (sku != null) {
                sku.setItemId(itemId);
            }
        }
        return updateSkus(skus);
    }

    private Item getItemToUpdateWithSkuWash(long itemId, List<Sku> skus) throws ServiceException {
        Item item = itemDao.load(itemId);
        if (item == null) {
            log.warn("item not exist, id={}", itemId);
            throw new ServiceException("item.not.exist");
        }
        int price = -1;
        int stock = MoreObjects.firstNonNull(item.getStockQuantity(), 0);
        for (Sku sku : skus) {
            if (sku.getPrice() == null || sku.getPrice() <= 0) {
                log.warn("sku price illegal");
                throw new ServiceException("sku.price.invalid");
            }
            // FIXME(Effet): 更新 sku 的时候是否更新库存？
            if (MoreObjects.firstNonNull(sku.getStockQuantity(), -1) < 0) {
                log.warn("sku stock quantity illegal");
                throw new ServiceException("sku.stock.quantity.invalid");
            }
            if (price < 0) {
                price = sku.getPrice();
            } else {
                price = Math.min(price, sku.getPrice());
            }
            stock += sku.getStockQuantity();
            sku.setItemId(itemId);
            sku.setAttributeKeyId1(getAttrKeyIdIfPresent(sku.getAttributeKey1()));
            sku.setAttributeKeyId2(getAttrKeyIdIfPresent(sku.getAttributeKey2()));
        }
        Item toUpdate = new Item();
        toUpdate.setId(itemId);
        toUpdate.setPrice(price);
        toUpdate.setStockQuantity(stock);

        return toUpdate;
    }

    @Override
    public Response<Boolean> updateSkus(List<Sku> skus) {
        Response<Boolean> resp = new Response<>();
        try {
            if (Iters.emptyToNull(skus) == null) {
                log.warn("skus empty or null, nothing to create");
                return Response.ok(Boolean.TRUE);
            }
            Set<Long> itemIds = new HashSet<>();
            for (Sku sku : skus) {
                if (sku.getId() == null) {
                    log.warn("sku id invalid");
                    return Response.fail("sku.id.invalid");
                }
                Sku exist = skuDao.load(sku.getId());
                if (exist == null) {
                    log.warn("sku not exist");
                    return Response.fail("sku.not.exist");
                }
                if (exist.getItemId() == null) {
                    log.warn("sku item id null");
                    return Response.fail("item.id.invalid");
                }
                itemIds.add(exist.getItemId());

                // could not update
                sku.setAttributeKey1(null);
                sku.setAttributeKeyId1(null);
                sku.setAttributeName1(null);
                sku.setAttributeValue1(null);

                sku.setAttributeKey2(null);
                sku.setAttributeKeyId2(null);
                sku.setAttributeName2(null);
                sku.setAttributeValue2(null);
            }
            if (itemIds.size() != 1) {
                log.warn("only one item's sku could updated together");
                return Response.fail("item.update.fail");
            }
            long itemId = itemIds.iterator().next();

            Item toUpdate = getItemToUpdateWithSkuWash(itemId, skus);

            skuManager.updates(toUpdate, skus);

            coreEventDispatcher.publish(itemId);
            resp.setResult(Boolean.TRUE);
        } catch (ServiceException e) {
            resp.setError(e.getMessage());
        } catch (Exception e){
            log.error("failed to update skus({}), cause: {}", skus, Throwables.getStackTraceAsString(e));
            resp.setError("sku.update.fail");
        }
        return resp;
    }

    private boolean isSkuFullExist(ItemDto itemDto) {
        if (itemDto == null || itemDto.getSkus() == null) {
            return false;
        }
        List<Sku> skus = new ArrayList<>();
        for (SkuWithLvPrice skuWithLvPrice : itemDto.getSkus()) {
            if (skuWithLvPrice == null || skuWithLvPrice.getSku() == null) {
                return false;
            }
            skus.add(skuWithLvPrice.getSku());
        }

        Spu spu = spuDao.load(itemDto.getItem().getSpuId());
        checkResult(spu != null, "spu.not.exist");

        List<AttributeKeyValues> kvs = RespHelper.orServEx(categoryReadService.findSkuAttributes(spu.getId()));
        checkResult(kvs.size() == 1 || kvs.size() == 2, "spu.sku.key.find.fail"); // TODO(Effet): fit error message
        if (kvs.size() == 1) {
            AttributeKey key = kvs.get(0).getAttributeKey();
            List<AttributeValue> values = kvs.get(0).getAttributeValues();

            List<AttributeValue> tryValues = new ArrayList<>();
            for (Sku sku : skus) {
                if (!key.getName().equals(sku.getAttributeKey1())) {
                    return false;
                }
                if (Strings.isNullOrEmpty(sku.getAttributeName1()) || Strings.isNullOrEmpty(sku.getAttributeValue1())) {
                    return false;
                }
                AttributeValue v = new AttributeValue();
                v.setId(Strs.parseLong(sku.getAttributeValue1()).orNull());
                v.setValue(sku.getAttributeName1());
                tryValues.add(v);
            }

            Set<Long> exists = new HashSet<>();
            for (AttributeValue tryValue : tryValues) {
                if (exists.contains(tryValue.getId())) return false;
                exists.add(tryValue.getId());
                if (!checkAttrValueExist(values, tryValue.getId(), tryValue.getValue())) return false;
            }
        }

        if (kvs.size() == 2) {
            if (!richCheck(kvs, skus)) return false;
        }

        return true;
    }

    private boolean richCheck(List<AttributeKeyValues> kvs, List<Sku> skus) {
        Map<Long, List<Long>> rows = new HashMap<>(), columns = new HashMap<>();
        for (Sku sku : skus) {
            if (kvs.get(0).getAttributeKey().getName().equals(sku.getAttributeKey1())
                    && kvs.get(1).getAttributeKey().getName().equals(sku.getAttributeKey2())) {
                Long row = Strs.parseLong(sku.getAttributeValue1()).orNull();
                Long column = Strs.parseLong(sku.getAttributeValue2()).orNull();
                if (!checkAttrValueExist(kvs.get(0).getAttributeValues(), row, sku.getAttributeName1())) return false;
                if (!checkAttrValueExist(kvs.get(1).getAttributeValues(), column, sku.getAttributeName2())) return false;
                putInto(rows, row, column);
                putInto(columns, column, row);
            } else if (kvs.get(1).getAttributeKey().getName().equals(sku.getAttributeKey1())
                    && kvs.get(0).getAttributeKey().getName().equals(sku.getAttributeKey2())) {
                Long column = Strs.parseLong(sku.getAttributeValue1()).orNull();
                Long row = Strs.parseLong(sku.getAttributeValue2()).orNull();
                if (!checkAttrValueExist(kvs.get(1).getAttributeValues(), column, sku.getAttributeName1())) return false;
                if (!checkAttrValueExist(kvs.get(0).getAttributeValues(), row, sku.getAttributeName2())) return false;
                putInto(rows, row, column);
                putInto(columns, column, row);
            } else {
                return false;
            }
        }

        if (rows.keySet().isEmpty()) return false;
        if (columns.keySet().isEmpty()) return false;
        for (List<Long> columnValues : rows.values()) if (!checkSame(columns.keySet(), columnValues)) return false;
        for (List<Long> rowValues : columns.values()) if (!checkSame(rows.keySet(), rowValues)) return false;

        return true;
    }

    private boolean checkSame(Set<Long> set, List<Long> list) {
        if (set.size() != list.size()) return false;
        return Sets.symmetricDifference(set, ImmutableSet.copyOf(list)).isEmpty();
    }

    private void putInto(Map<Long, List<Long>> m, Long k, Long v) {
        List<Long> vs = m.get(k);
        if (vs == null) {
            vs = new ArrayList<>();
            m.put(k, vs);
        }
        vs.add(v);
    }

    private boolean checkAttrValueExist(List<AttributeValue> values, Long valueId, String value) {
        for (AttributeValue v : values) {
            if (v.getId().equals(valueId) && v.getValue().equals(value)) {
                return true;
            }
        }
        return false;
    }

    private void checkSku(ItemDto itemDto) throws ServiceException {
        checkResult(isSkuFullExist(itemDto), "skus.invalid");
    }

    private boolean isPriceLegal(ItemDto itemDto) {
        Integer originPrice = itemDto.getItem().getOriginPrice();
        if (originPrice == null) return false;
        Integer lowestPrice = null;
        for (SkuWithLvPrice skuWithLvPrice : itemDto.getSkus()) {
            Sku sku = skuWithLvPrice.getSku();
            if (sku == null || sku.getPrice() == null) return false;
            if (!checkLvPrices(sku.getPrice(), skuWithLvPrice.getPrices())) return false;
            Integer skuOriginPrice = MoreObjects.firstNonNull(sku.getOriginPrice(), originPrice);
            if (skuOriginPrice == null || skuOriginPrice < sku.getPrice()) return false;
            if (lowestPrice == null) lowestPrice = sku.getPrice(); else if (lowestPrice > sku.getPrice()) lowestPrice = sku.getPrice();
        }
        if (lowestPrice == null || originPrice < lowestPrice) return false;
        return true;
    }

    private boolean checkLvPrices(Integer price, List<SkuPrice> skuPrices) {
        boolean zeroExist = false;
        for (SkuPrice skuPrice : skuPrices) {
            if (skuPrice.getLv().equals(0)) {
                zeroExist = true;
                if (!price.equals(skuPrice.getPrice())) {
                    return false;
                }
            }
        }
        if (!zeroExist) {
            return false;
        }
        for (SkuPrice p1 : skuPrices) {
            for (SkuPrice p2 : skuPrices) {
                if (p1.getLv() > p2.getLv() && p1.getPrice() > p2.getPrice()) {
                    return false;
                }
            }
        }
        return true;
    }

    private void checkLegalPrice(ItemDto itemDto) throws ServiceException {
        checkResult(isPriceLegal(itemDto), "item.price.illegal");
    }

    /**
     * 创建普通商品,商品的图片信息,以及sku信息
     * @param loginer 当前用户
     * @param itemDto 商品详情
     * @return 新创建商品的id
     */
    @Override
    public Response<Long> createItem(BaseUser loginer, ItemDto itemDto) {
        Response<Long> resp = new Response<Long>();
        try {
            Item item = itemDto.getItem();
            checkResult(TextValidator.ITEM_TITLE.boolCheck(item.getName()), "item.name.1to200");
            if (Params.trimToNull(item.getMainImage()) == null) {
                log.warn("main image not allow null");
                return Response.fail("item.main.image.null");
            }
            Spu spu = spuDao.load(itemDto.getItem().getSpuId());
            checkResult(spu != null, "spu.not.exist");
            // 设置类目
            item.setCategoryId(spu.getCategoryId());

            item.setUserId(loginer.getId());
            checkResult(itemDao.checkItemDup(loginer.getId(), item.getName()) <= 0, "item.already.exist");
            if (itemDto.getAttrImages() == null) {
                itemDto.getItemDetail().setAttrImages(null);
            } else if (itemDto.getAttrImages().isEmpty()) {
                itemDto.getItemDetail().setAttrImages("[]"); // empty list
            } else {
                if (!judgeAttrImage(itemDto.getAttrImages())) {
                    return Response.fail("item.attr.image.illegal");
                }
                itemDto.getItemDetail().setAttrImages(JsonMapper.nonDefaultMapper().toJson(itemDto.getAttrImages()));
            }
            checkSku(itemDto);
            checkLegalPrice(itemDto);
            // 计算SKU中最低价
            int lowestPrice = extractLowestPrice(itemDto);
            item.setPrice(lowestPrice);
            Brand brand = brandDao.load(item.getBrandId());
            if (brand == null) {
                log.warn("brand(id={}) isn't exist.", item.getBrandId());
                resp.setError("brand.not.exist");
                return resp;
            }
            item.setBrandName(brand.getName());
            if (MoreObjects.firstNonNull(itemDto.getOnShelf(), Boolean.FALSE)) {
                item.setStatus(Item.Status.ON_SHELF.value());
                item.setOnShelfAt(new Date());
            } else {
                item.setStatus(Item.Status.INIT.value());
            }
            Integer stockQuantity = getItemStockQuantity(itemDto.getSkus());
            item.setStockQuantity(stockQuantity);
            item.setSaleQuantity(0);
            if (item.getType() != null) {
                checkState(ArrayUtils.contains(itemTypeSorter.ordinaryItemTypes(), item.getType()), "item.type.not.found");
            } else {
                item.setType(itemTypeSorter.defaultOrdinaryItemType());
            }
            int idxBit = 0;
            if (itemDto.getIndexable() == null || itemDto.getIndexable()) {
                idxBit |= Item.INDEXABLE;
            }
            item.setBitMark(idxBit | Item.VISIBLE);
            item.setDistributable(itemDto.getDistributable());
            // 设一下 attribute key 的 id
            for (SkuWithLvPrice skuWithLvPrice : Iters.nullToEmpty(itemDto.getSkus())) {
                Sku sku = skuWithLvPrice.getSku();
                sku.setAttributeKeyId1(getAttrKeyIdIfPresent(sku.getAttributeKey1()));
                sku.setAttributeKeyId2(getAttrKeyIdIfPresent(sku.getAttributeKey2()));
            }
            fillAttrImagesIntoSku(itemDto);
            itemManager.create(itemDto);

            Long itemId = itemDto.getItem().getId();

            // 商品描述图
            /*
            if (itemId != null && itemDto.getItemPictures() != null) {
                ItemDescription jsonCustom = new ItemDescription();
                jsonCustom.setImages(itemDto.getItemPictures().toArray(new String[itemDto.getItemPictures().size()]));
                try {
                    String json = JSON_NON_DEFAULT_MAPPER.toJson(jsonCustom);
                    itemDescriptionRedisDao.set(itemId, json);
                } catch (Exception e) {
                    log.warn("item json custom create failed, itemId={}, jsonCustom={}, cause:{}",
                            itemId, jsonCustom, e.getMessage());
                }
            }
            */

            // 商品创建事件
            coreEventDispatcher.publish(new ItemCreateEvent(itemId));

            if (MoreObjects.firstNonNull(itemDto.getOnShelf(), Boolean.FALSE)) {
                coreEventDispatcher.publish(new ItemUpEvent(itemId));
            }

            resp.setResult(item.getId());
        } catch (IllegalStateException e) {
            log.warn("failed create item({}), error:{}", itemDto, e.getMessage());
            resp.setError(e.getMessage());
        } catch (ServiceException e) {
            log.warn("failed create item({}), error:{}", itemDto, e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e){
            log.error("failed create item({}), cause: {}", itemDto, Throwables.getStackTraceAsString(e));
            resp.setError("item.create.fail");
        }
        return resp;
    }

    private void fillAttrImagesIntoSku(ItemDto itemDto) {
        for (SkuWithLvPrice skuWithLvPrice : Iters.nullToEmpty(itemDto.getSkus())) {
            Sku sku = skuWithLvPrice.getSku();
            for (ItemAttrImage image : Iters.nullToEmpty(itemDto.getAttrImages())) {
                if (Objects.equal(sku.getAttributeKeyId1(), image.getKeyId()) && Objects.equal(Strs.parseLong(sku.getAttributeValue1()).orNull(), image.getValueId())) {
                    sku.setImage(image.getImage());
                    break;
                }
                if (Objects.equal(sku.getAttributeKeyId2(), image.getKeyId()) && Objects.equal(Strs.parseLong(sku.getAttributeValue2()).orNull(), image.getValueId())) {
                    sku.setImage(image.getImage());
                    break;
                }
            }
            if (sku.getImage() == null) {
                sku.setImage(itemDto.getItem().getMainImage());
            }
        }
    }

    private Long getAttrKeyIdIfPresent(String key) {
        if (Strings.isNullOrEmpty(key))
            return null;
        return checkNotNull(attributeKeyDao.findByName(key), "attr key=%s not found", key).getId();
    }

    /**
     * 合计所有SKU的库存量为商品的库存量
     * @param skus SKU
     * @return 商品的库存量
     */
    private Integer getItemStockQuantity(List<SkuWithLvPrice> skus) {
        int stock = 0;
        for (SkuWithLvPrice sku : skus){
            stock += sku.getSku().getStockQuantity();
        }
        return stock;
    }

    /**
     * 获取SKU中最低的价格作为商品价格
     * @param itemDto 商品详情
     * @return SKU中最低价
     */
    private int extractLowestPrice(ItemDto itemDto) {
        List<SkuWithLvPrice> skus = itemDto.getSkus();
        int lowPrice = skus.get(0).getSku().getPrice();
        for (SkuWithLvPrice sku : skus) {
            //获取sku最低价
            lowPrice = lowPrice < sku.getSku().getPrice() ? lowPrice : sku.getSku().getPrice();
        }
        return lowPrice;
    }

    /**
     * 更新商品,商品的图片信息,以及sku信息
     * @param itemDto 商品详情
     * @return 更新成功返回true, 反之false
     */
    @Override
    public Response<Boolean> updateItem(BaseUser loginer, ItemDto itemDto) {
        Response<Boolean> resp = new Response<Boolean>();
        try {
            checkBeforeUpdateItem(loginer.getId(), itemDto);
            Spu spu = spuDao.load(itemDto.getItem().getSpuId());
            checkResult(spu != null, "spu.not.exist");
            itemDto.getItem().setCategoryId(spu.getCategoryId());

            if (itemDto.getAttrImages() == null) {
                itemDto.getItemDetail().setAttrImages(null);
            } else if (itemDto.getAttrImages().isEmpty()) {
                itemDto.getItemDetail().setAttrImages("[]"); // empty list
            } else {
                if (!judgeAttrImage(itemDto.getAttrImages())) {
                    return Response.fail("item.attr.image.illegal");
                }
                itemDto.getItemDetail().setAttrImages(JsonMapper.nonDefaultMapper().toJson(itemDto.getAttrImages()));
            }
            checkLegalPrice(itemDto);
            int lowestPrice = extractLowestPrice(itemDto);
            Item item = itemDto.getItem();
            if (item.getName() != null) {
                checkResult(TextValidator.ITEM_TITLE.boolCheck(item.getName()), "item.name.1to200");

                Item exist = itemDao.load(item.getId());
                if (item.getName().equals(exist.getName())) {
                    item.setName(null);
                } else {
                    checkResult(itemDao.checkItemDup(loginer.getId(), item.getName()) <= 0, "item.already.exist");
                }
            }
            item.setPrice(lowestPrice);
            // 库存量
            Integer stockQuantity = getItemStockQuantity(itemDto.getSkus());
            item.setStockQuantity(stockQuantity);
            item.setType(null); // can not change type
            item.setStatus(null);
            // 进否搜索
            item.setBitMark(null);
            if (itemDto.getIndexable() != null) {
                Item existed = itemDao.load(itemDto.getItem().getId());
                int bit = MoreObjects.firstNonNull(existed.getBitMark(), 0 | Item.INDEXABLE | Item.VISIBLE);
                if (itemDto.getIndexable()) {
                    item.setBitMark(bit | Item.INDEXABLE);
                } else {
                    if ((bit & Item.INDEXABLE) != 0) {
                        item.setBitMark(bit ^ Item.INDEXABLE);
                    }
                }
            }
            item.setDistributable(itemDto.getDistributable());
            // 设一下 attribute key 的 id
            for (SkuWithLvPrice skuWithLvPrice : Iters.nullToEmpty(itemDto.getSkus())) {
                Sku sku = skuWithLvPrice.getSku();
                sku.setAttributeKeyId1(getAttrKeyIdIfPresent(sku.getAttributeKey1()));
                sku.setAttributeKeyId2(getAttrKeyIdIfPresent(sku.getAttributeKey2()));
            }
            fillAttrImagesIntoSku(itemDto);
            itemManager.update(itemDto);

            coreEventDispatcher.publish(new ItemInfoEvent(itemDto.getItem().getId()));
            resp.setResult(Boolean.TRUE);
        } catch (ServiceException e){
            resp.setError(e.getMessage());
        } catch (Exception e){
            log.error("failed to update item({}), cause: {}", itemDto, Throwables.getStackTraceAsString(e));
            resp.setError("item.update.fail");
        }
        return resp;
    }

    @Override
    public Response<Boolean> updateItemDescription(long itemId, ItemDescription itemDescription) {
        try {
            Item item = itemDao.load(itemId);
            if (item == null) {
                return Response.fail("item.not.exist");
            }
            if (itemDescription == null) {
                return Response.ok(Boolean.FALSE);
            }
            itemDescriptionRedisDao.set(itemId, JsonMapper.nonDefaultMapper().toJson(itemDescription));
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("update item json custom failed, itemId={}, itemDescription={}, cause:{}",
                    itemId, itemDescription, Throwables.getStackTraceAsString(e));
            return Response.fail("item.update.fail");
        }
    }

    private boolean judgeAttrImage(List<ItemAttrImage> attrImages) {
        Long keyId = attrImages.get(0).getKeyId();
        if (keyId == null) {
            return false;
        }
        Set<Long> valueSet = new HashSet<>();
        for (ItemAttrImage attrImage : attrImages) {
            if (attrImage.getKeyId() == null || attrImage.getValueId() == null) {
                return false;
            }
            if (!Objects.equal(attrImage.getKeyId(), keyId)) {
                return false;
            }
            if (valueSet.contains(attrImage.getValueId())) {
                return false;
            }
            valueSet.add(attrImage.getValueId());
        }
        return true;
    }

    /**
     * 更新商品前作验证
     * @param loginerId 当前用户id
     * @param itemDto 商品详情
     */
    private void checkBeforeUpdateItem(Long loginerId, ItemDto itemDto) {
        Long itemId = itemDto.getItem().getId();
        Item item = itemDao.load(itemId);
        if (item == null){
            log.warn("item(id={}) isn't exist.", itemId);
            throw new ServiceException("item.not.exist");
        }
        if (!Objects.equal(loginerId, item.getUserId())){
            log.warn("user(id={}) isn't the owner of item({}).", loginerId, item);
            throw new ServiceException("user.not.owner");
        }
    }

    @Override
    public Response<Boolean> updateItemSaleAndStock(long itemId, long skuId, int quantity) {
        try {
            if (quantity == 0) {
                return Response.ok(Boolean.FALSE);
            }
            // internal invoke, ignore check
            boolean outOfStock = itemManager.updateItemSaleAndStock(itemId, skuId, quantity);

            if (outOfStock) {
                coreEventDispatcher.publish(new ItemDownEvent(itemId, ItemDownEvent.ReasonType.OUT_OF_STOCK));
            } else {
                coreEventDispatcher.publish(new ItemInfoEvent(itemId));
            }

            // update shop item count if item's stock quantity <= 0
            Item item = itemDao.load(itemId);
            if(item.getStockQuantity() <= 0) {
                Long shopId = item.getShopId();
                ItemCountEvent event = new ItemCountEvent();
                event.setData(Lists.newArrayList(shopId));
                itemEventDispatcher.publish(event);
            }
            return Response.ok(Boolean.TRUE);
        } catch (ServiceException e) {
            log.warn("update item sale and stock failed, itemId={}, skuId={}, quantity={}, error:{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e){
            log.error("failed to update item(id={})'s sku(id={})'s sale and stock quantity({}), cause: {}",
                    itemId, skuId, quantity, Throwables.getStackTraceAsString(e));
            return Response.fail("item.update.quantity.fail");
        }
    }

    @Override
    public Response<Integer> updateItemStatus(Integer status, Collection<Long> ids) {
        return updateItemStatus(safeBuildStatusEnum(status), ids);
    }

    @Override
    public Response<Integer> updateItemStatus(Integer status, Long... ids) {
        return updateItemStatus(safeBuildStatusEnum(status), ids);
    }

    @Override
    public Response<Integer> updateItemStatus(Item.Status status, Collection<Long> ids) {
        if (status == null) {
            log.warn("unknown item status, ignore update");
            return Response.ok(0);
        }
        ids = safeUniqueIds(ids);
        if (Iters.emptyToNull(ids) == null) {
            log.warn("itemIds is null or empty");
            return Response.ok(0);
        }
        // TODO: to be completed
        return null;
    }

    @Override
    public Response<Integer> updateItemStatus(Item.Status status, Long... ids) {
        return updateItemStatus(status, Arrays.asList(ids));
    }

    private @Nullable List<Long> safeUniqueIds(@Nullable Collection<Long> ids) {
        return ids == null ? null : FluentIterable.from(ids).filter(Predicates.notNull()).toSet().asList();
    }

    private @Nullable Item.Status safeBuildStatusEnum(@Nullable Integer status) {
        return status == null ? null : Item.Status.parse(status).orNull();
    }

    /**
     * 更新前验证
     * @param itemId 商品id
     * @param skuId 商品SKU编号
     * @param quantity 更新量
     */
    private void checkBeforeUpdateItemSaleAndStock(Long itemId, Long skuId, Integer quantity) {
        Item item = itemDao.load(itemId);
        if (item == null){
            log.warn("item(id={}) isn't exist", itemId);
            throw new ServiceException("item.not.exist");
        }
        Sku sku = skuDao.load(skuId);
        if (sku == null){
            log.warn("sku(id={}) isn't exist", skuId);
            throw new ServiceException("sku.not.exist");
        }
        if (!Objects.equal(item.getId(), sku.getItemId())){
            log.warn("sku({})'s itemId != itemId", sku, item.getId());
            throw new ServiceException("sku.not.match.item");
        }
        if (quantity < 0 && (sku.getStockQuantity() + quantity) < 0){
            log.warn("quantity({}) is > sku(stockQuantity={})", quantity, sku.getStockQuantity());
            throw new ServiceException("sku.stock.quantity.overflow");
        }
    }

    /**
     * 批量更新商品状态,这个接口给卖家后台用
     * @param user   当前登录用户
     * @param status 状态
     * @param ids    商品id列表
     * @return 更新成功返回true, 反之false
     */
    @Override
    public Response<Boolean> updateItemsStatus(BaseUser user, Integer status, List<Long> ids) {
        Response<Boolean> resp = new Response<Boolean>();
        try {
            if (Iters.emptyToNull(ids) == null) {
                return Response.ok(Boolean.TRUE);
            }
            List<Long> filterIds = filterItemStatus(user, ids, status);
            if (!Iterables.isEmpty(filterIds)){
                itemManager.setStatuses(filterIds, status);
                Item item = itemDao.load(ids.get(0));
                Long shopId = item.getShopId();
                ItemCountEvent event = new ItemCountEvent(Lists.newArrayList(shopId));
                itemEventDispatcher.publish(event);

                Item.Status itemStatus = Item.Status.from(status);
                if (itemStatus != null) {
                    switch (itemStatus) {
                        case ON_SHELF:
                            coreEventDispatcher.publish(new ItemUpEvent(filterIds));
                            break;
                        case OFF_SHELF:
                            ItemDownEvent.ReasonType reason;
                            if (Objects.equal(User.TYPE.ADMIN.toNumber(), user.getType())
                                    || Objects.equal(User.TYPE.OPERATOR.toNumber(), user.getType())) {
                                reason = ItemDownEvent.ReasonType.ADMIN;
                            } else {
                                reason = ItemDownEvent.ReasonType.SELF;
                            }
                            coreEventDispatcher.publish(new ItemDownEvent(filterIds, reason));
                            break;
                        case FROZEN:
                            coreEventDispatcher.publish(new ItemFrozenEvent(filterIds));
                            break;
                        case DELETED:
                            coreEventDispatcher.publish(new ItemDelEvent(filterIds));
                            break;
                        default:
                            break;
                    }
                }
            }
            resp.setResult(Boolean.TRUE);
        } catch (ServiceException e) {
          resp.setError(e.getMessage());
        } catch (Exception e) {
            log.error("user({}) failed to update items(ids={})'s status({}), cause: {}",
                    user, ids, status, Throwables.getStackTraceAsString(e));
            resp.setError("item.update.status.fail");
        }
        return resp;
    }

    /**
     * 批量更新商品状态,这个接口给卖家后台用
     *
     * @param user   当前登录用户
     * @param status 状态
     * @param id     商品id
     * @return 更新成功返回true, 反之false
     */
    @Override
    public Response<Boolean> updateItemStatus(BaseUser user, Integer status, Long id) {
        return updateItemsStatus(user, status, Arrays.asList(id));
    }

    /**
     * 更新商品的营销活动标志
     *
     * @param user      当前用户
     * @param id        商品ID
     * @param marketTag 商品的营销活动标志, 二进制字符串, 0表示无该活动，1表示有该活动
     * @return 更新成功返回true，反之false
     */
    @Override
    public Response<Boolean> updateItemMarketTag(BaseUser user, Long id, String marketTag) {
        Response<Boolean> resp = new Response<Boolean>();
        try {
            checkBeforeUpdateItemMarketTag(user, id);
            itemDao.updateMarketTag(id, marketTag);
            coreEventDispatcher.publish(new ItemInfoEvent(id));
            resp.setResult(Boolean.TRUE);
        } catch (ServiceException e){
            resp.setError(e.getMessage());
        } catch (Exception e){
            log.error("failed to update item(id={})'s market tag to ({}), cause: {}",
                    id, marketTag, Throwables.getStackTraceAsString(e));
            resp.setError("item.update.fail");
        }
        return resp;
    }

    private void checkBeforeUpdateItemMarketTag(BaseUser user, Long id) {
        Item item = itemDao.load(id);
        if (item == null){
            log.warn("item(id={}) isn't exist.", id);
            throw new ServiceException("item.not.exist");
        }
        if (user != null && // for job
                !Objects.equal(User.TYPE.ADMIN.toNumber(), user.getType().intValue())
                && !Objects.equal(user.getId(), item.getUserId())){
            log.warn("user({}) isn't the owner of item({})", user, item);
            throw new ServiceException("user.not.owner");
        }
    }

    /**
     * 这个需要处理过滤一些无法批量更改状态的商品编号（这个是针对与商家的过滤机制）
     * @param loginer 当前用户
     * @param itemIds 商品编号
     * @param status 需要更改的状态
     */
    private List<Long> filterItemStatus(BaseUser loginer, List<Long> itemIds, Integer status){
        List<Long> filterList = new ArrayList<Long>();
        Item item;
        for(Long itemId : itemIds){
            item = itemDao.load(itemId);
            if (loginer != null
                    && !Objects.equal(User.TYPE.ADMIN.toNumber(),loginer.getType()) //运营
                    && !Objects.equal(User.TYPE.OPERATOR.toNumber(),loginer.getType()) //运营
                    && !Objects.equal(loginer.getId(), item.getUserId())) {
                log.warn("item({})'s userId != loginer(id={}).", item, loginer.getId());
                throw new ServiceException("user.not.owner");
            }
            if(!filterFunction(item, status)){
                filterList.add(itemId);
            }
        }

        return filterList;
    }

    /**
     * 判断商品是否过滤
     * @param item 商品信息
     * @param newStatusVal 更改后的状态(当前批量处理的只存在上架和下架操作)
     * @return 返回是否被过滤
     */
    private Boolean filterFunction(Item item, Integer newStatusVal){
        Boolean filterRes = true;
        final Item.Status oldStatus = Item.Status.from(item.getStatus());
        final Item.Status newStatus = Item.Status.from(newStatusVal);
        switch(newStatus){
            case INIT:
                filterRes = true;
                break;
            case ON_SHELF:
                //只有下架或者初始状态才能迁移到上架状态(并且库存大于0)
                if((oldStatus == Item.Status.OFF_SHELF
                        || oldStatus == Item.Status.INIT )
                        && item.getStockQuantity() > 0){
                    filterRes = false;
                }
                break;
            case OFF_SHELF:
                //只有上架状态或未上架才能迁移到下架状态
                if(oldStatus == Item.Status.ON_SHELF
                        || oldStatus == Item.Status.FROZEN
                        || oldStatus == Item.Status.INIT ){
                    filterRes = false;
                }
                break;
            case FROZEN:
                filterRes = false;
                break;
            default:
                break;
        }

        return filterRes;
    }

    /**
     * 如果商品未售出,则物理删除商品及相关信息,如果商品已经有了交易,则逻辑删除
     * @param user 当前用户
     * @param id   商品id
     * @return 删除成功返回true, 反之false
     */
    @Override
    public Response<Boolean> deleteItemById(BaseUser user, Long id) {
        Response<Boolean> resp = new Response<Boolean>();
        try {
            Item deleted = itemManager.delete(user.getId(), id);
            itemEventDispatcher.publish(new ItemCountEvent(Lists.newArrayList(deleted.getShopId())));
            coreEventDispatcher.publish(new ItemDelEvent(id));
            resp.setResult(Boolean.TRUE);
        } catch (ServiceException e){
          resp.setError(e.getMessage());
        } catch (Exception e){
            log.error("failed to delete item(id={}), cause: {}", Throwables.getStackTraceAsString(e));
            resp.setError("item.delete.fail");
        }
        return resp;
    }

    /**
     * 批量删除商品，如果商品未售出,则物理删除商品及相关信息,如果商品已经有了交易,则逻辑删除
     * @param user 当前用户
     * @param ids  商品id列表
     * @return 批量删除成功返回true, 反之false
     */
    @Override
    public Response<Boolean> deleteItemsByIds(BaseUser user, List<Long> ids) {
        Response<Boolean> resp = new Response<Boolean>();
        try {
            Long userId = user.getId();
            itemManager.deletes(userId, ids);
            List<Long> shopIds = itemDao.findShopIdsByItemIds(ids);
            ItemCountEvent ice = new ItemCountEvent();
            ice.setData(shopIds);
            itemEventDispatcher.publish(ice);
            if (Iters.emptyToNull(ids) != null) {
                coreEventDispatcher.publish(new ItemDelEvent(ids));
            }
            resp.setResult(Boolean.TRUE);
        } catch (Exception e){
            log.error("user({}) failed to delete items(ids={}), cause: {}", user, ids, Throwables.getStackTraceAsString(e));
            resp.setError("item.delete.fail");
        }
        return resp;
    }

    /**
     * 创建商品模板
     * @param itemTemplateDto 商品模板那DTO
     * @return 创建成功返回true, 反之false
     */
    @Override
    public Response<Long> createItemTemplate(ItemTemplateDto itemTemplateDto) {
        Response<Long> resp = new Response<Long>();
        try {
            ItemTemplate template = itemTemplateDto.getTemplate();
            template.setJsonSkus(JSON_NON_DEFAULT_MAPPER.toJson(itemTemplateDto.getSkus()));
            itemTemplateDao.create(template);
            resp.setResult(template.getId());
        } catch (Exception e){
            log.error("failed to create item template({}), cause: {}", itemTemplateDto, Throwables.getStackTraceAsString(e));
            resp.setError("item.template.create.fail");
        }
        return resp;
    }

    /**
     * 更新商品模板
     * @param itemTemplateDto 商品模板那DTO
     * @return 更新成功返回true, 反之false
     */
    @Override
    public Response<Boolean> updateItemTemplate(ItemTemplateDto itemTemplateDto) {
        Response<Boolean> resp = new Response<Boolean>();
        try {
            ItemTemplate newTemplate = itemTemplateDto.getTemplate();
            ItemTemplate oldTemplate = itemTemplateDao.findBySpuId(newTemplate.getSpuId());
            if (oldTemplate == null){
                log.warn("item template({}) isn't exist.", newTemplate);
                resp.setError("item.template.not.exist");
                return resp;
            }
            newTemplate.setJsonSkus(JSON_NON_DEFAULT_MAPPER.toJson(itemTemplateDto.getSkus()));
            newTemplate.setId(oldTemplate.getId());
            itemTemplateDao.update(newTemplate);
            resp.setResult(Boolean.TRUE);
        } catch (Exception e){
            log.error("failed to update item template({}), cause: {}", itemTemplateDto, Throwables.getStackTraceAsString(e));
            resp.setError("item.template.update.fail");
        }
        return resp;
    }

    /**
     * 将店铺所有的商品(非逻辑删除的)改为冻结状态
     * @param shopIds 店铺id列表
     * @return 下架成功返回true, 反之false
     */
    @Override
    public Response<Boolean> frozenItems(List<Long> shopIds) {
        Response<Boolean> resp = new Response<Boolean>();
        try {
            itemManager.frozenByShopIds(shopIds);
            List<Long> itemIds = itemDao.findItemIdsByShopIds(shopIds, itemTypeSorter.ordinaryItemTypes());
            if (Iters.emptyToNull(itemIds) != null) {
                coreEventDispatcher.publish(new ItemFrozenEvent(itemIds));
            }
            resp.setResult(Boolean.TRUE);
        } catch (Exception e){
            log.error("failed to frozen shop(ids={})'s items, cause: {}", shopIds, Throwables.getStackTraceAsString(e));
            resp.setError("item.frozen.fail");
        }
        return resp;
    }

    /**
     * 将店铺所有冻结的商品改为下架状态
     * @param shopIds 店铺id列表
     * @return 成功返回true, 反之false
     */
    @Override
    public Response<Boolean> unFrozenItems(List<Long> shopIds) {
        Response<Boolean> resp = new Response<Boolean>();
        try {
            itemManager.unFrozenByShopIds(shopIds);
            List<Long> itemIds = itemDao.findItemIdsByShopIds(shopIds, itemTypeSorter.ordinaryItemTypes());
            if (Iters.emptyToNull(itemIds) != null) {
                coreEventDispatcher.publish(new ItemFrozenEvent(itemIds));
            }
            resp.setResult(Boolean.TRUE);
        } catch (Exception e){
            log.error("failed to unfrozen shop(ids={})'s items, cause: {}", shopIds, Throwables.getStackTraceAsString(e));
            resp.setError("item.unfrozen.fail");
        }
        return resp;
    }

    /**
     * 更新店铺上架商品数
     * @param shopId 店铺id
     * @return 更新成功返回true, 反之false
     */
    @Override
    public Response<Boolean> updateShopOnShelfItemCount(Long shopId) {
        Response<Boolean> resp = new Response<Boolean>();
        try {
            Long shopOnShelfItemCount = itemDao.countOnShelfByShopId(shopId, itemTypeSorter.ordinaryItemTypes());
            itemRedisDao.setShopItemCount(shopId, shopOnShelfItemCount);
            resp.setResult(Boolean.TRUE);
        } catch (Exception e){
            log.error("failed to update shop(id={}) on shelf item count, cause: {}", shopId, Throwables.getStackTraceAsString(e));
            resp.setError("shop.items.on.shelf.count.fail");
        }
        return resp;
    }

    /**
     * 批量更新店铺上架商品数
     *
     * @param shopIds 店铺id列表
     * @return 更新成功返回true, 反之false
     */
    @Override
    public Response<Boolean> updateShopsOnShelfItemCount(List<Long> shopIds) {
        Response<Boolean> resp = new Response<Boolean>();
        try {
            Map<Long, Long> shopItemCountMap = itemDao.countOnShelfByShopIds(shopIds, itemTypeSorter.ordinaryItemTypes());
            if (shopItemCountMap != null &&
                    shopItemCountMap.size() > 0){
                itemRedisDao.setShopsItemCount(shopIds, shopItemCountMap);
            }
            resp.setResult(Boolean.TRUE);
        } catch (Exception e){
            log.error("failed to update shop(ids={}) on shelf item count, cause: {}", shopIds, Throwables.getStackTraceAsString(e));
            resp.setError("shop.items.on.shelf.count.fail");
        }
        return resp;
    }


    @Override
    public Response<Boolean> updateSkuExtra(Long skuId, String extra) {
        Response<Boolean> result = new Response<>();

        try {
            Sku sku = skuDao.load(skuId);
            if(Arguments.isNull(sku)) {
                result.setError("sku.not.found");
                return result;
            }

            Sku toUpdate = new Sku();
            toUpdate.setId(skuId);
            toUpdate.setExtra(extra);
            skuDao.update(toUpdate);

            coreEventDispatcher.publish(new ItemInfoEvent(sku.getItemId()));

            result.setResult(Boolean.TRUE);
        }catch (Exception e) {
            log.error("fail to update sku extra by sku id {}, extra {}, cause:{}",
                    skuId, extra, Throwables.getStackTraceAsString(e));
            result.setError("update.sku.extra.fail");
        }
        return result;
    }

    @Override
    public Response<List<ImportResult<ProductUploadData>>> importItemData(long shopId, List<ProductUploadData> data, ProductVolumeListingOption option, boolean byAdmin) {
        try {
            Shop shop = RespHelper.orServEx(shopReadService.findById(shopId));
            List<ImportResult<ProductUploadData>> results = Lists.newArrayList();
            for (ProductUploadData row : Iters.nullToEmpty(data)) {
                results.add(singleSafeImport(shop.getUserId(), shop.getId(), shop.getName(), row, option, byAdmin));
            }
            return Response.ok(results);
        } catch (ServiceException e) {
            log.warn("import product data failed, data(count={}), error:{}",
                    safeSize(data), e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("import product data failed, data(count={}), cause:{}",
                    safeSize(data), Throwables.getStackTraceAsString(e));
            return Response.fail("item.import.fail");
        }
    }

    private int safeSize(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private void checkCategoryPerm(long sellerId, String[] categories) throws ServiceException {
        checkResult(RespHelper.orServEx(categoryReadService.checkCategoryPermByPath(sellerId, categories)), "category.find.no.perm");
    }

    private void checkBrandPerm(long sellerId, String brandName) throws ServiceException {
        checkResult(RespHelper.orServEx(brandReadService.checkBrandPermByName(sellerId, brandName)), "brand.no.perm");
    }

    private Response<Long> tryToGetLeafCat(long userId, String... categories) {
        if (categories == null) {
            return Response.fail("category.path.invalid");
        }
        Response<BackCategory> bcResp = categoryReadService.getLeafByBackCategoryPathWithPerm(userId, Arrays.asList(categories));
        if (!bcResp.isSuccess()) {
            return Response.fail(bcResp.getError());
        }
        return Response.ok(bcResp.getResult().getId());
    }

    private ImportResult<ProductUploadData> singleSafeImport(long userId, long shopId, String shopName, ProductUploadData row, ProductVolumeListingOption option, boolean byAdmin) {
        try {
            if (Strings.isNullOrEmpty(row.getSpu())) {
                return ImportResult.fail(row, "spu.name.blank");
            }
            // 检查类目和品牌权限
            checkCategoryPerm(userId, row.getCategories());
            checkBrandPerm(userId, row.getBrand());

            // 商品是否已存在
            boolean isItemExists = itemDao.checkItemDup(userId, row.getProductName()) > 0;
            if (isItemExists) {
                switch (option) {
                    case NONE: return ImportResult.fail(row, "item.already.exist");
                    case EXIST_SKIP: return ImportResult.ok(row);
                    case EXIST_OVERWRITE:
                        // code in later
                        break;
                    default:
                        throw new ServiceException("product.volume.listing.option.invalid");
                }
            }

            // SPU 是否已存在
            long categoryId = RespHelper.orServEx(tryToGetLeafCat(userId, row.getCategories()));
            Spu criteria = new Spu();
            criteria.setCategoryId(categoryId);
            criteria.setName(row.getSpu());
            Spu spu = spuDao.findBy(criteria);

            int count; // 笛卡尔积维度

            // sku cartesian-product error
            List<String> variantMissing = new ArrayList<>();
            List<String> variantDuplicated = new ArrayList<>();
            if (spu == null) {
                // 不存在自动创建, 创建前检查销售属性是否合法
                SpuImportData spuData = new SpuImportData();
                spuData.setCategories(row.getCategories());
                spuData.setBrand(row.getBrand());
                spuData.setSpu(row.getSpu());
                spuData.setAttributes(generateVariants(row));
                count = spuData.getAttributes().length;
                buildVariantsError(spuData.getAttributes(), row.getSkus(), variantMissing, variantDuplicated);
                if (hasVariantError(variantMissing, variantDuplicated)) {
                    return ImportResult.fail(row, genVariantError(variantMissing, variantDuplicated));
                }
                List<ImportResult<SpuImportData>> spuResult = RespHelper.orServEx(categoryWriteService.importSpu(userId, Lists.newArrayList(spuData)));
                if (!spuResult.get(0).isSuccess()) {
                    return ImportResult.fail(row, spuResult.get(0).getErrors());
                }
                spu = spuDao.findBy(criteria);
                if (spu == null) {
                    return ImportResult.fail(row, "spu.not.exist");
                }
            } else {
                // 存在时检查销售属性是否与 SPU 中匹配
                List<AttributeKeyValues> kvs = RespHelper.orServEx(categoryReadService.findSkuAttributes(spu.getId()));
                if (kvs.size() == 0) {
                    throw new ServiceException("spu.sku.size.0");
                }
                if (kvs.size() > 2) {
                    throw new ServiceException("spu.sku.size.gt2");
                }
                count = kvs.size();
                SpuImportData.Attr[] variants = new SpuImportData.Attr[kvs.size()];
                for (int i = 0; i<variants.length; ++i) {
                    variants[i] = new SpuImportData.Attr();
                    if (kvs.get(i) == null || kvs.get(i).getAttributeKey() == null || Strings.isNullOrEmpty(kvs.get(i).getAttributeKey().getName())) {
                        throw new ServiceException("item.spu.invalid");
                    }
                    variants[i].setKeyName(kvs.get(i).getAttributeKey().getName());
                    variants[i].setKeyType("S");
                    if (kvs.get(i).getAttributeValues() == null || kvs.get(i).getAttributeValues().isEmpty()) {
                        throw new ServiceException("item.spu.invalid");
                    }
                    Set<String> values = new HashSet<>();
                    for (AttributeValue attributeValue : kvs.get(i).getAttributeValues()) {
                        if (attributeValue == null || Strings.isNullOrEmpty(attributeValue.getValue())) {
                            throw new ServiceException("item.spu.invalid");
                        }
                        values.add(attributeValue.getValue());
                    }
                    if (values.isEmpty()) {
                        throw new ServiceException("item.spu.invalid");
                    }
                    variants[i].setValues(values.toArray(new String[values.size()]));
                }
                buildVariantsError(variants, row.getSkus(), variantMissing, variantDuplicated);
                if (hasVariantError(variantMissing, variantDuplicated)) {
                    return ImportResult.fail(row, genVariantError(variantMissing, variantDuplicated));
                }
            }

            long spuId = spu.getId();

            Item item = new Item();
            item.setName(row.getProductName());
            item.setOuterItemId(row.getProductNumber());
            item.setUserId(userId);
            item.setShopId(shopId);
            item.setShopName(shopName);
            item.setSpuId(spuId);
            item.setType(ItemType.NORMAL.value());
            item.setOriginPrice(Strs.parseInt(row.getOriginPrice()).orNull());
            item.setRemark(row.getRemark());
            item.setBrandId((long) spu.getBrandId());
            item.setBrandName(spu.getBrandName());

            // to collect image error
            List<String> imageErrorCollector = new ArrayList<>();

            item.setMainImage(tryRefineImage(getCoord(row.getImages(), 0), imageErrorCollector, byAdmin, userId));

            ItemDetail itemDetail = new ItemDetail();
            itemDetail.setImage1(tryRefineImage(getCoord(row.getImages(), 1), imageErrorCollector, byAdmin, userId));
            itemDetail.setImage2(tryRefineImage(getCoord(row.getImages(), 2), imageErrorCollector, byAdmin, userId));
            itemDetail.setImage3(tryRefineImage(getCoord(row.getImages(), 3), imageErrorCollector, byAdmin, userId));
            itemDetail.setImage4(tryRefineImage(getCoord(row.getImages(), 4), imageErrorCollector, byAdmin, userId));

            ItemDto itemDto = new ItemDto();
            itemDto.setItem(item);
            itemDto.setItemDetail(itemDetail);
            itemDto.setShipFee(new ShipFee());
            itemDto.setSkus(RespHelper.orServEx(genSkus(row)));
            itemDto.setAttrImages(RespHelper.or500(genAttrImageJson(row, imageErrorCollector, byAdmin, userId)));
            User user = new User();
            user.setId(userId);
            user.setType(User.TYPE.SELLER.toNumber());

            List<String> detailImages = new ArrayList<>();
            if (row.getDetailImages() != null) {
                for (int i=0; i<row.getDetailImages().length; ++i) {
                    String image = tryRefineImage(row.getDetailImages()[i], imageErrorCollector, byAdmin, userId);
                    if (image != null) {
                        detailImages.add(image);
                    }
                }
            }

            if (!imageErrorCollector.isEmpty()) {
                ImportResult.Err err = new ImportResult.Err();
                err.setError("user.image.find.fail");
                String[] detail = new String[imageErrorCollector.size()];
                for (int i=0; i<detail.length; ++i) {
                    detail[i] = appendTail("user.image.find.fail.detail", imageErrorCollector.get(i));
                }
                err.setDetail(detail);
                return ImportResult.fail(row, err);
            }

            Long itemId;
            if (!isItemExists) {
                // later to create item
                itemId = RespHelper.orServEx(createItem(user, itemDto));
            } else {
                itemId = itemDao.getDupItemId(userId, row.getProductName());
                itemDto.getItem().setId(itemId);
                buildSkuId(itemDto, count);
                RespHelper.orServEx(updateItem(user, itemDto));
            }

            row.setDetailImages(detailImages.toArray(new String[detailImages.size()]));
            String json = JsonMapper.nonDefaultMapper().toJson(new ItemDescription(row.getFeatures(), row.getDescription(), row.getDetailImages()));
            itemDescriptionRedisDao.set(itemId, json);
            return ImportResult.ok(row);
        } catch (ServiceException e) {
            log.warn("import single item failed, error={}", e.getMessage());
            return ImportResult.fail(row, e.getMessage());
        } catch (Exception e){
            log.warn("import single item failed, cause:{}", Throwables.getStackTraceAsString(e));
            return ImportResult.fail(row, "item.create.fail");
        }
    }

    private void buildSkuId(ItemDto itemDto, int count) throws ServiceException {
        long itemId = itemDto.getItem().getId();
        List<Sku> skuExistList = skuDao.findByItemId(itemId);
        for (SkuWithLvPrice skuWithLvPrice : itemDto.getSkus()) {
            Sku sku = skuWithLvPrice.getSku();
            boolean exist = false;
            for (Sku skuExist : skuExistList) {
                if (localEqual(sku, skuExist, count)) {
                    sku.setId(skuExist.getId());
                    exist = true;
                    break;
                }
            }
            if (!exist) {
                throw new ServiceException("sku.not.exist");
            }
        }
    }

    private boolean localEqual(Sku a, Sku b, int count) {
        if (count == 1) {
            // 正向
            if (localEqual(a.getAttributeKey1(), b.getAttributeKey1())
                    && localEqual(a.getAttributeName1(), b.getAttributeName1())) {
                return true;
            }
            if (localEqual(a.getAttributeKey2(), b.getAttributeKey2())
                    && localEqual(a.getAttributeName2(), b.getAttributeName2())) {
                return true;
            }
            // 交叉
            if (localEqual(a.getAttributeKey1(), b.getAttributeKey2())
                    && localEqual(a.getAttributeName1(), b.getAttributeName2())) {
                return true;
            }
            if (localEqual(a.getAttributeKey2(), b.getAttributeKey1())
                    && localEqual(a.getAttributeName2(), b.getAttributeName1())) {
                return true;
            }
            return false;
        } else if (count == 2) {
            // 正向
            if (localEqual(a.getAttributeKey1(), b.getAttributeKey1())
                    && localEqual(a.getAttributeName1(), b.getAttributeName1())
                    && localEqual(a.getAttributeKey2(), b.getAttributeKey2())
                    && localEqual(a.getAttributeName2(), b.getAttributeName2())) {
                return true;
            }
            // 交叉
            if (localEqual(a.getAttributeKey1(), b.getAttributeKey2())
                    && localEqual(a.getAttributeName1(), b.getAttributeName2())
                    && localEqual(a.getAttributeKey2(), b.getAttributeKey1())
                    && localEqual(a.getAttributeName2(), b.getAttributeName1())) {
                return true;
            }
            return false;
        } else {
            return false;
        }
    }

    private boolean localEqual(String a, String b) {
        if (Strings.isNullOrEmpty(a) || Strings.isNullOrEmpty(b)) {
            return false;
        }
        return a.equals(b);
    }

    private boolean hasVariantError(List<String> variantMissing, List<String> variantDuplicated) {
        return !(variantMissing.isEmpty() && variantDuplicated.isEmpty());
    }

    /**
     * 生成种类错误
     *
     * @param variantMissing    缺少的种类
     * @param variantDuplicated 重复的种类
     * @return 错误
     */
    private ImportResult.Err genVariantError(List<String> variantMissing, List<String> variantDuplicated) {
        ImportResult.Err err = new ImportResult.Err();
        err.setError("skus.invalid");
        String[] detail = new String[variantMissing.size() + variantDuplicated.size()];
        for (int i=0; i<detail.length; ++i) {
            if (i < variantMissing.size()) {
                detail[i] = appendTail("skus.invalid.variant.missing", variantMissing.get(i));
            } else {
                detail[i] = appendTail("skus.invalid.variant.duplicate", variantDuplicated.get(i - variantMissing.size()));
            }
        }
        err.setDetail(detail);
        return err;
    }

    private String appendTail(String tmpl, String... args) {
        return tmpl + '\n' + Joiner.on('\n').useForNull("").join(args);
    }

    private String tryRefineImage(String image, List<String> errorCollector, boolean byAdmin, Long userId) {
        image = Params.trimToNull(image);
        if (image == null) {
            return null;
        }
        if (image.startsWith("http")) {
            return image;
        }
        String fixImage = image.startsWith("/") ? image.substring(1) : image;
        if (!byAdmin) {
            fixImage = "users/" + userId + "/" + fixImage;
        }
        Response<UserImage> imageResp = userImageService.fileRealPath(byAdmin ? null : userId, fixImage);
        if (!imageResp.isSuccess()) {
            errorCollector.add(image);
            return null;
        }
        UserImage userImage = imageResp.getResult();
        String domain = Strings.nullToEmpty(configCenter.get(ImageSettings.BASE_URL));
        String path = Strings.nullToEmpty(userImage.getPath());
        return domain + path;
    }

    private String getCoord(String[] images, int coord) {
        return (coord < 0 || images == null || images.length <= coord) ? null : images[coord];
    }

    private void buildVariantsError(SpuImportData.Attr[] variants, ProductUploadData.SkuData[] skus, List<String> variantMissing, List<String> variantDuplicated) {
        // check quantity of variant is same
        for (ProductUploadData.SkuData skuData : skus) {
            if (variants.length != skuData.getAttributes().length) {
                throw new ServiceException("skus.invalid.variant.quantity.no.match");
            }
        }

        if (variants.length == 1) {
            Multiset<String> valueCount = HashMultiset.create();
            Set<String> valueSet = new HashSet<>();
            for (ProductUploadData.SkuData.Attr attr : skus[0].getAttributes()) {
                valueCount.add(attr.getValue());
                valueSet.add(attr.getValue());
            }
            for (String value : variants[0].getValues()) {
                if (!valueSet.contains(value)) {
                    continue;
                }
                int count = valueCount.count(value);
                if (count == 0) {
                    variantMissing.add(variants[0].getKeyName() + ":" + value);
                }
                if (count > 1) {
                    variantDuplicated.add(variants[0].getKeyName() + ":" + value);
                }
            }
        }

        if (variants.length == 2) {
            Multiset<String> kvCount = HashMultiset.create();
            Set<String> valueSet0 = new HashSet<>(), valueSet1 = new HashSet<>();
            for (ProductUploadData.SkuData sku : skus) {
                String hash;
                if (variants[0].getKeyName().equals(sku.getAttributes()[0].getKey())) {
                    hash = buildAttr2Hash(sku.getAttributes()[0], sku.getAttributes()[1]);
                    valueSet0.add(sku.getAttributes()[0].getValue());
                    valueSet1.add(sku.getAttributes()[1].getValue());
                } else {
                    hash = buildAttr2Hash(sku.getAttributes()[1], sku.getAttributes()[0]);
                    valueSet0.add(sku.getAttributes()[1].getValue());
                    valueSet1.add(sku.getAttributes()[0].getValue());
                }
                kvCount.add(hash);
            }
            for (String v0 : variants[0].getValues()) {
                if (!valueSet0.contains(v0)) {
                    continue;
                }
                for (String v1 : variants[1].getValues()) {
                    if (!valueSet1.contains(v1)) {
                        continue;
                    }
                    String hash = variants[0].getKeyName() + ":" + v0 + "," + variants[1].getKeyName() + ":" + v1;
                    int count = kvCount.count(hash);
                    if (count == 0) {
                        variantMissing.add(hash);
                    }
                    if (count > 1) {
                        variantDuplicated.add(hash);
                    }
                }
            }
        }
    }

    private String buildAttr2Hash(ProductUploadData.SkuData.Attr a, ProductUploadData.SkuData.Attr b) {
        return a.getKey() + ":" + a.getValue() + "," + b.getKey() + ":" + b.getValue();
    }

    private SpuImportData.Attr[] generateVariants(ProductUploadData data) throws ServiceException {
        if (Iters.emptyToNull(data.getSkus()) == null) {
            throw new ServiceException("sku.empty");
        }

        Map<String, SpuImportData.Attr> map = new HashMap<>();
        for (ProductUploadData.SkuData skuData : data.getSkus()) {
            if (Iters.emptyToNull(skuData.getAttributes()) == null) {
                throw new ServiceException("sku.attribute.empty");
            }
            for (ProductUploadData.SkuData.Attr attr : skuData.getAttributes()) {
                String keyName = attr.getKey();
                String value = attr.getValue();

                SpuImportData.Attr catAttr = map.get(keyName);
                if (catAttr == null) {
                    catAttr = new SpuImportData.Attr();
                    catAttr.setKeyName(keyName);
                    catAttr.setKeyType("S");
                    catAttr.setValues(new String[0]);
                    map.put(keyName, catAttr);
                }
                Set<String> sb = new HashSet<>();
                for (String s : catAttr.getValues()) {
                    if (s != null) {
                        sb.add(s);
                    }
                }
                if (value != null) {
                    sb.add(value);
                }
                catAttr.setValues(sb.toArray(new String[sb.size()]));
            }
        }
        SpuImportData.Attr[] variants = FluentIterable.from(map.values()).toArray(SpuImportData.Attr.class);
        if (variants.length < 0) {
            throw new ServiceException("spu.sku.size.0");
        }
        if (variants.length > 2) {
            throw new ServiceException("spu.sku.size.gt2");
        }
        return variants;
    }

    private Response<List<ItemAttrImage>> genAttrImageJson(ProductUploadData data, List<String> imageErrorCollector, boolean byAdmin, Long userId) {
        try {
            List<ItemAttrImage> images = Lists.newArrayList();
            for (ProductUploadData.SkuData skuData : data.getSkus()) {
                if (Iters.emptyToNull(skuData.getAttributes()) == null) {
                    // do nothing
                } else {
                    for (ProductUploadData.SkuData.Attr attr : skuData.getAttributes()) {
                        if (attr == null) continue;
                        String url = Params.trimToNull(attr.getImageUrl());
                        if (url != null) {
                            Long keyId = attributeKeyDao.findByName(attr.getKey()).getId();
                            Long valueId = attributeValueDao.findByValue(attr.getValue()).getId();

                            boolean alreadyExist = false;
                            for (ItemAttrImage image : images) {
                                if (Objects.equal(keyId, image.getKeyId()) && Objects.equal(valueId, image.getValueId())) {
                                    alreadyExist = true;
                                    break;
                                }
                            }
                            if (!alreadyExist) {
                                ItemAttrImage image = new ItemAttrImage();
                                image.setKeyId(keyId);
                                image.setValueId(valueId);
                                image.setImage(tryRefineImage(url, imageErrorCollector, byAdmin, userId));
                                images.add(image);
                            }
                        }
                    }
                }
            }
            return Response.ok(images);
        } catch (Exception e) {
            log.warn("gen attr image json failed, cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("sku.gen.attr.image.json.fail");
        }
    }

    private Response<List<SkuWithLvPrice>> genSkus(ProductUploadData data) {
        List<SkuWithLvPrice> skuWithLvPrices = new ArrayList<>();

        try {
            for (ProductUploadData.SkuData skuData : data.getSkus()) {

                String outerId = skuData.getSku();
                Sku sku = new Sku();
                sku.setSkuCode(outerId);

                if (Iters.emptyToNull(skuData.getAttributes()) == null) {
                    // do nothing
                } else {
                    if (skuData.getAttributes().length >= 1) {
                        sku.setAttributeKey1(skuData.getAttributes()[0].getKey());
                        sku.setAttributeName1(skuData.getAttributes()[0].getValue());
                        AttributeValue av = attributeValueDao.findByValue(sku.getAttributeName1());
                        if (av == null) {
                            log.warn("attribute value not found, {}", sku.getAttributeName1());
                            return Response.fail("attribute.value.not.found");
                        }
                        sku.setAttributeValue1(av.getId().toString());
                    }
                    if (skuData.getAttributes().length >= 2) {
                        sku.setAttributeKey2(skuData.getAttributes()[1].getKey());
                        sku.setAttributeName2(skuData.getAttributes()[1].getValue());
                        AttributeValue av = attributeValueDao.findByValue(sku.getAttributeName2());
                        if (av == null) {
                            log.warn("attribute value not found, {}", sku.getAttributeName2());
                            return Response.fail("attribute.value.not.found");
                        }
                        sku.setAttributeValue2(av.getId().toString());
                    }
                }

                if (Iters.emptyToNull(skuData.getPrices()) == null) {
                    return Response.fail("sku.prices.empty");
                }
                sku.setOriginPrice(skuData.getOriginPrice());
                sku.setPrice(skuData.getPrices()[0]);
                sku.setStockQuantity(skuData.getQuantity());
                sku.setModel(skuData.getModel());

                SkuWithLvPrice skuWithLvPrice = new SkuWithLvPrice();
                skuWithLvPrice.setSku(sku);
                List<SkuPrice> skuPrices = new ArrayList<>();
                for (int l = 0; l < skuData.getPrices().length; ++l) {
                    SkuPrice skuPrice = new SkuPrice();
                    skuPrice.setPrice(skuData.getPrices()[l]);
                    skuPrice.setLv(l);
                    skuPrices.add(skuPrice);
                }
                skuWithLvPrice.setPrices(skuPrices);
                skuWithLvPrices.add(skuWithLvPrice);
            }
            return Response.ok(skuWithLvPrices);
        } catch (Exception e) {
            log.warn("gen sku failed, cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("sku.gen.fail");
        }
    }

    @Override
    public Response<Void> setItemDiscount(long id, int discount, int expireSec, String extra) {
        try {
            ItemDiscountDto discountDto = new ItemDiscountDto();
            discountDto.setDiscount(discount);
            discountDto.setExtra(Params.trimToNull(extra));
            String data = JsonMapper.nonDefaultMapper().toJson(discountDto);
            itemRedisDao.setItemDiscount(id, expireSec, data);
            return Response.ok();
        } catch (Exception e) {
            log.error("set item discount failed, id={}, discount={}, cause:{}",
                    id, discount, Throwables.getStackTraceAsString(e));
            return Response.fail("item.update.fail");
        }
    }

    @Override
    public Response<Void> delItemDiscount(long id) {
        try {
            itemRedisDao.delItemDiscount(id);
            return Response.ok();
        } catch (Exception e) {
            log.error("del item discount failed, id={}, cause:{}",
                    id, Throwables.getStackTraceAsString(e));
            return Response.fail("item.update.fail");
        }
    }

    @Override
    public Response<Boolean> createFavItem(FavoriteItem favoriteItem) {
        Response<Boolean> response=new Response<>();
        try {
            checkNotNull(favoriteItem);
            checkNotNull(favoriteItem.getItemId());
            checkNotNull(favoriteItem.getUserId());

            Long exists=favoriteItemDao.count(favoriteItem.getItemId(),favoriteItem.getUserId());
            if(exists>0) {
                log.debug("fav item exists, favitem={}", favoriteItem);
            }else{
                favoriteItemDao.create(favoriteItem);
            }
            response.setResult(Boolean.TRUE);
        }catch (Exception ex){
            log.error("create fav item fail, cause={}",ex.getMessage());
            response.setError("create.favorite.item.fail");
        }
        return response;
    }

    @Override
    public  Response<Boolean> deleteFavItem(Long itemId, Long userId){
        Response<Boolean> response=new Response<>();
        try{
            checkNotNull(itemId);
            checkNotNull(userId);
            favoriteItemDao.deleteByItemIdAndUserId(itemId,userId);
            response.setResult(Boolean.TRUE);
        }catch (Exception ex){
            log.error("delete fav item fail, cause={}",ex.getMessage());
            response.setError("delete.favorite.item.fail");
        }
        return response;
    }
}
