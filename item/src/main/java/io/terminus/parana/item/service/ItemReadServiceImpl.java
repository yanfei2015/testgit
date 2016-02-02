/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package io.terminus.parana.item.service;

import com.fasterxml.jackson.databind.JavaType;
import com.google.common.base.*;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.*;
import com.google.common.primitives.Longs;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.utils.*;
import io.terminus.pampas.common.BaseUser;
import io.terminus.pampas.common.Response;
import io.terminus.pampas.design.dao.ItemCustomRedisDao;
import io.terminus.parana.category.cache.BaseCacher;
import io.terminus.parana.category.dao.mysql.BackCategoryPermDao;
import io.terminus.parana.category.dao.mysql.CategoryAttributeValueDao;
import io.terminus.parana.category.dao.mysql.SpuDao;
import io.terminus.parana.category.dao.redis.AttributeKeyDao;
import io.terminus.parana.category.dto.*;
import io.terminus.parana.category.internal.BackendPerms;
import io.terminus.parana.category.model.*;
import io.terminus.parana.category.service.CategoryReadService;
import io.terminus.parana.common.util.*;
import io.terminus.parana.item.dao.mysql.*;
import io.terminus.parana.item.dao.redis.ItemCacheDao;
import io.terminus.parana.item.dao.redis.ItemDescriptionRedisDao;
import io.terminus.parana.item.dao.redis.ItemRedisDao;
import io.terminus.parana.item.dao.redis.ItemTagDao;
import io.terminus.parana.item.dto.*;
import io.terminus.parana.item.ext.ItemTypeSorter;
import io.terminus.parana.item.internal.SkuPriceSelector;
import io.terminus.parana.item.manager.ItemManager;
import io.terminus.parana.item.model.*;
import io.terminus.parana.user.service.UserReadService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.*;
import java.util.Objects;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkState;
import static io.terminus.common.utils.Arguments.*;

/**
 * 商品读服务
 * Author: haolin
 * On: 9/1/14
 */
@Service @Slf4j
public class ItemReadServiceImpl implements ItemReadService {

    @Autowired
    private BrandDao brandDao;

    @Autowired
    private ItemDao itemDao;

    @Autowired
    private ItemDetailDao itemDetailDao;

    @Autowired
    private ItemTemplateDao itemTemplateDao;

    @Autowired
    private ItemTagDao itemTagDao;

    @Autowired
    private ItemManager itemManager;

    @Autowired
    private SpuDao spuDao;

    @Autowired
    private SkuDao skuDao;

    @Autowired
    private ShipFeeDao shipFeeDao;

    @Autowired
    private CategoryAttributeValueDao categoryAttributeValueDao;

    @Autowired
    private BaseCacher categoryCacher;

    @Autowired
    private SkuExtraDao skuExtraDao;

    @Autowired
    private SkuPriceDao skuPriceDao;

    @Autowired
    private BackCategoryPermDao backCategoryPermDao;

    @Autowired
    private BackendPerms backendPerms;

    @Autowired
    private UserReadService userReadService;

    @Autowired
    private CategoryReadService categoryReadService;

    @Autowired
    private ItemCustomRedisDao itemCustomRedisDao;

    @Autowired
    private ItemCacheDao itemCacheDao;

    @Autowired
    private ItemDescriptionRedisDao itemDescriptionRedisDao;
    
    @Autowired
    private ItemTypeSorter itemTypeSorter;

    @Autowired
    private ItemRedisDao itemRedisDao;

    @Autowired
    private FavoriteItemDao favoriteItemDao;

    @Autowired
    private AttributeKeyDao attributeKeyDao;

    private static final JsonMapper JSON_NON_DEFAULT_MAPPER = JsonMapper.nonDefaultMapper();
    private static final JavaType SKU_LIST_TYPE = JSON_NON_DEFAULT_MAPPER.createCollectionType(ArrayList.class, Sku.class);
    private static final Splitter COMMA_SPLITTER = Splitters.COMMA;
    private static final DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
    public static final JsonMapper JSON_MAPPER = JsonMapper.nonEmptyMapper();
    private static final Integer MAX_SKU_ENTRY_SIZE = 1000000;

    /**
     * 商品缓存 {itemId : Item}
     */
    private final LoadingCache<Long, Item> itemsCache;

    /**
     * 商品SKU列表缓存 {itemId : List<Sku>}
     */
    private final LoadingCache<Long, List<Sku>> itemSkusCache;

    /**
     * Sku缓存 {skuId : Sku}
     */
    private final LoadingCache<Long, Sku> skuCache;

    public ItemReadServiceImpl() {
        itemsCache = CacheBuilder.newBuilder().maximumSize(MAX_SKU_ENTRY_SIZE).build(new CacheLoader<Long, Item>() {
            @Override
            public Item load(Long itemId) throws Exception {
                return itemDao.load(itemId);
            }
        });

        itemSkusCache = CacheBuilder.newBuilder().maximumSize(MAX_SKU_ENTRY_SIZE).build(new CacheLoader<Long, List<Sku>>() {
            @Override
            public List<Sku> load(Long itemId) throws Exception {
                return doLoadItemSkusByItemId(itemId);
            }
        });

        skuCache = CacheBuilder.newBuilder().maximumSize(MAX_SKU_ENTRY_SIZE).build(new CacheLoader<Long, Sku>() {
            @Override
            public Sku load(Long skuId) throws Exception {
                return skuDao.load(skuId);
            }
        });
    }

    private List<Sku> doLoadItemSkusByItemId(Long itemId) {
        Sku criteria = new Sku();
        criteria.setItemId(itemId);
        return skuDao.list(criteria);
    }

    /**
     * 插入默认一般商品类型查询
     *
     * @param q 查询条件
     */
    private void fillDefaultOrdinaryItemTypes(Map<String, Object> q) {
        if (q.get("type") == null && q.get("types") == null) {
            // 兼容以前版本，只查普通商品
            q.put("types", itemTypeSorter.ordinaryItemTypes());
        }
    }

    @Override
    public Response<Paging<Brand>> pagingBrands(Integer pageNo, Integer pageSize, Map<String, Object> criteria){
        Response<Paging<Brand>> resp = new Response<Paging<Brand>>();
        try {
            PageInfo page = new PageInfo(pageNo, pageSize);
            Map<String, Object> nonNullAndEmpty = Params.filterNullOrEmpty(criteria);
            resp.setResult(brandDao.paging(page.getOffset(), page.getLimit(), nonNullAndEmpty));
        } catch (Exception e){
            log.error("failed to paging brands(pageNo={}, pageSize={}, criteria={}), cause: {}",
                    pageNo, pageSize, criteria, Throwables.getStackTraceAsString(e));
            resp.setError("brand.find.fail");
        }
        return resp;
    }


    @Override
    public Response<Brand> findBrandBySpuId(Long id) {
        Response<Brand> resp = new Response<Brand>();
        try {
            Spu spu = spuDao.load(id);
            if (spu == null){
                log.warn("spu(id={}) isn't exist.", id);
                resp.setError("spu.not.exist");
                return resp;
            }
            Brand brand = brandDao.load(spu.getBrandId());
            if (brand == null){
                log.warn("brand(id={}) isn't exist.", id);
                resp.setError("brand.not.exist");
                return resp;
            }
            resp.setResult(brand);
        } catch (Exception e){
            log.error("failed to find brand by spu id({}), cause: {}",
                    id, Throwables.getStackTraceAsString(e));
            resp.setError("brand.find.fail");
        }
        return resp;
    }

    @Override
    public Response<Paging<Brand>> pagingBrands(String name, Integer pageNo, Integer pageSize) {
        Response<Paging<Brand>> resp = new Response<Paging<Brand>>();
        try {
            PageInfo page = new PageInfo(pageNo, pageSize);
            Brand criteria = new Brand();
            criteria.setName(name);
            resp.setResult(brandDao.paging(page.getOffset(), page.getLimit(), criteria));
        } catch (Exception e){
            log.error("failed to find brand(name={}, pageNo={}, pageSize={}), cause: {}",
                    name, pageNo, pageSize, Throwables.getStackTraceAsString(e));
            resp.setError("brand.find.fail");
        }
        return resp;
    }

    @Override
    public Response<Paging<ItemWithTags>> pagingTagOrParams(BaseUser loginer, String tag, Map<String, Object> criteria, Integer pageNo, Integer pageSize) {
        Response<Paging<ItemWithTags>> resp = new Response<Paging<ItemWithTags>>();
        try {
            PageInfo page = new PageInfo(pageNo, pageSize);
            Paging<Item> pagingItems;
            if (tag != null){
                // 按标签查询
                pagingItems = pagingItemsByTag(loginer, tag, page.getOffset(), page.getLimit());
            } else {
                // 按条件查询
                Response<Paging<Item>> findResp = findItemsAsSeller(loginer, pageNo, pageSize, criteria);
                if (!findResp.isSuccess()){
                    resp.setError(findResp.getError());
                    return resp;
                } else {
                    pagingItems = findResp.getResult();
                }
            }
            // render with tags
            if (pagingItems.getTotal() <= 0L){
                resp.setResult(new Paging<ItemWithTags>(0L, Collections.<ItemWithTags>emptyList()));
            } else {
                resp.setResult(renderTags(pagingItems));
            }
        } catch (Exception e){
            log.error("failed to paging(pageNo={}, pageSize={}) items by tag({}), cause: {}",
                    pageNo, pageSize, tag, Throwables.getStackTraceAsString(e));
            resp.setError("item.find.fail");
        }
        return resp;
    }

    /**
     * 通过标签或查询上架的商品列表(买家调用, 无分页)
     *
     * @param sellerId 商家ID
     * @param tag      标签名
     * @return 分页商品
     */
    @Override
    public Response<List<Item>> listByTagsOfOnShelf(Long sellerId, String tag) {
        Response<List<Item>> resp = new Response<List<Item>>();
        try {
            List<Long> itemIds;
            if (Strings.isNullOrEmpty(tag)){
                // 未分类标签
                itemIds = itemTagDao.findUnKnownItems(sellerId, 0, Integer.MAX_VALUE).getData();
            } else {
                // 其他标签
                itemIds = itemTagDao.findItemsByTag2paging(sellerId, tag, 0, Integer.MAX_VALUE).getData();
            }
            if (itemIds == null || itemIds.isEmpty()){
                resp.setResult(Collections.EMPTY_LIST);
                return resp;
            }
            List<Item> items = itemDao.loadsBy(ImmutableMap.of("ids", itemIds, "userId", sellerId, "status", Item.Status.ON_SHELF.value()));
            resp.setResult(items);
        } catch (Exception e){
            log.error("failed to list items by (sellerId={}, tag={}), cause: {}",
                    sellerId, tag, Throwables.getStackTraceAsString(e));
            resp.setError("item.find.fail");
        }
        return resp;
    }


    /**
     * 通过标签或查询条件分页查询商品
     *
     * @param sellerId 卖家ID
     * @param tag      标签名
     * @param criteria 查询条件
     * @param pageNo   页号
     * @param pageSize 分页大小
     * @return 分页商品
     */
    @Override
    public Response<Paging<Item>> pagingTag(Long sellerId, String tag, Map<String, Object> criteria, Integer pageNo, Integer pageSize) {
        Response<Paging<Item>> resp = new Response<Paging<Item>>();
        try {
            PageInfo page = new PageInfo(pageNo, pageSize);
            Paging<Item> pagingItems= pagingItemsByTag(sellerId, tag, page.getOffset(), page.getLimit());
            resp.setResult(pagingItems);
        } catch (Exception e){
            log.error("failed to paging(pageNo={}, pageSize={}) items by tag({}), cause: {}",
                    pageNo, pageSize, tag, Throwables.getStackTraceAsString(e));
            resp.setError("item.find.fail");
        }
        return resp;
    }

    /**
     * 通过标签或查询条件分页查询商品(通过偏移量, 实用H5)
     *
     * @param sellerId 卖家ID
     * @param tag      标签名
     * @param criteria 查询条件
     * @param offset   数据起始偏移
     * @param pageSize 分页大小
     * @return 分页商品
     */
    @Override
    public Response<Paging<Item>> pagingTagByOffset(Long sellerId, String tag, Map<String, Object> criteria, Integer offset, Integer pageSize) {
        Response<Paging<Item>> resp = new Response<Paging<Item>>();
        try {
            Paging<Item> pagingItems= pagingItemsByTag(sellerId, tag, offset, pageSize);
            resp.setResult(pagingItems);
        } catch (Exception e){
            log.error("failed to paging(offset={}, pageSize={}) items by tag({}), cause: {}",
                    offset, pageSize, tag, Throwables.getStackTraceAsString(e));
            resp.setError("item.find.fail");
        }
        return resp;
    }

    /**
     * 获取各Item的Tags
     * @param pagingItems  商品分页信息
     * @return ItemWithTags分页对象
     */
    private Paging<ItemWithTags> renderTags(Paging<Item> pagingItems) {
        return new Paging<ItemWithTags>(pagingItems.getTotal(), totoItemWithTagses(pagingItems.getData()));
    }

    private List<ItemWithTags> totoItemWithTagses(List<Item> items){
        List<ItemWithTags> itemWithTagses = Lists.newArrayListWithCapacity(items.size());
        ItemWithTags itemWithTags;
        for (Item item :items){
            itemWithTags = toItemWithTags(item);
            itemWithTagses.add(itemWithTags);
        }
        return itemWithTagses;
    }

    private ItemWithTags toItemWithTags(Item item) {
        ItemWithTags itemWithTags = new ItemWithTags();
        itemWithTags.setItemId(item.getId());
        itemWithTags.setItemName(item.getName());
        itemWithTags.setImageUrl(item.getMainImage());
        itemWithTags.setBrandName(item.getBrandName());
        itemWithTags.setPrice(item.getPrice());
        itemWithTags.setOriginPrice(item.getOriginPrice());
        itemWithTags.setStatus(item.getStatus());
        itemWithTags.setTags(new ArrayList<String>(itemTagDao.findTagsByItemId(item.getId())));
        return itemWithTags;
    }

    /**
     * 由标签分页查询商品
     * @param loginer 当前用户
     * @param tag 标签名称
     * @param offset 起始偏移
     * @param limit 分页大小
     * @return 商品分页对象
     */
    private Paging<Item> pagingItemsByTag(BaseUser loginer, String tag, Integer offset, Integer limit) {
        return pagingItemsByTag(loginer.getId(), tag, offset, limit);
    }

    /**
     * 由标签分页查询商品
     * @param tag 标签名称
     * @param offset 起始偏移
     * @param limit 分页大小
     * @return 商品分页对象
     */
    private Paging<Item> pagingItemsByTag(Long sellerId, String tag, Integer offset, Integer limit) {
        Paging<Long> pagingItemIds;
        if (Strings.isNullOrEmpty(tag)){
            // 未分类标签
            pagingItemIds = itemTagDao.findUnKnownItems(sellerId, offset, limit);
        } else {
            // 其他标签
            pagingItemIds = itemTagDao.findItemsByTag2paging(sellerId, tag, offset, limit);
        }
        Paging<Item> pagingItems;
        if (pagingItemIds.getTotal() <= 0){
            pagingItems = new Paging<Item>(0L, Collections.<Item>emptyList());
        } else {
            List<Item> items = itemDao.loads(pagingItemIds.getData());
            pagingItems = new Paging<Item>(pagingItemIds.getTotal(), items);
        }
        return pagingItems;
    }

    @Override
    public Response<Sku> findSkuById(Long id) {
        Response<Sku> resp = new Response<Sku>();
        try {
            Sku sku = skuDao.load(id);
            if (sku == null){
                log.warn("sku(id={}) isn't exist.", id);
                resp.setError("sku.not.exist");
                return resp;
            }
            resp.setResult(sku);
        } catch (Exception e){
            log.error("failed to find sku by id({}), cause: {}", id, Throwables.getStackTraceAsString(e));
            resp.setError("sku.find.fail");
        }
        return resp;
    }

    @Override
    public Response<Sku> findSkuByIdWithCache(Long id) {
        Response<Sku> result = new Response<Sku>();

        try {
            result.setResult(skuCache.get(id));
            return result;
        }catch (Exception e) {
            log.error("fail to find sku by id {} with cache, cause:{}", id, Throwables.getStackTraceAsString(e));
            result.setError("sku.find.fail");
            return result;
        }
    }

    @Override
    public Response<List<Sku>> findSkusByIds(List<Long> skuIds) {
        Response<List<Sku>> resp = new Response<List<Sku>>();
        try {
            resp.setResult(skuDao.loads(skuIds));
        } catch (Exception e){
            log.error("failed to find sku by ids({}), cause: {}", skuIds, Throwables.getStackTraceAsString(e));
            resp.setError("sku.find.fail");
        }
        return resp;
    }

    @Override
    public Response<List<Sku>> findSkusByItemId(Long id) {
        Response<List<Sku>> resp = new Response<List<Sku>>();
        try {
            resp.setResult(doLoadItemSkusByItemId(id));
        } catch (Exception e){
            log.error("failed to find skus by item id({}), cause: {}", id, Throwables.getStackTraceAsString(e));
            resp.setError("sku.find.fail");
        }
        return resp;
    }

    /**
     * 根据商品id找sku(缓存5分钟)
     *
     * @param id 商品id
     * @return 商品的SKU
     */
    @Override
    public Response<List<Sku>> findSkusByItemIdWithCache(Long id) {
        Response<List<Sku>> resp = new Response<List<Sku>>();
        try {
            resp.setResult(itemSkusCache.getUnchecked(id));
        } catch (Exception e){
            log.error("failed to find skus by item id({}), cause: {}", id, Throwables.getStackTraceAsString(e));
            resp.setError("sku.find.fail");
        }
        return resp;
    }

    @Override
    public Response<Sku> findSkuByAttributeValuesAndItemId(Long itemId, String attributeValue1, String attributeValue2) {
        Response<Sku> resp = new Response<Sku>();
        try {
            Sku criteria = new Sku();
            criteria.setItemId(itemId);
            criteria.setAttributeValue1(attributeValue1);
            criteria.setAttributeValue2(attributeValue2);
            Sku sku = skuDao.findSkuByAttributeValuesAndItemId(criteria);
            if (sku == null){
                log.warn("sku(itemId={}, attributeValue1={}, attributeValue2={}), cause: {}",
                        itemId, attributeValue1, attributeValue2);
                resp.setError("sku.not.exist");
                return resp;
            }
            resp.setResult(sku);
        } catch (Exception e){
            log.error("failed to find sku by item id({}) and attribute values(value1={}, value2={}), cause: {}",
                    itemId, attributeValue1, attributeValue2, Throwables.getStackTraceAsString(e));
            resp.setError("sku.find.fail");
        }
        return resp;
    }

    @Override
    public Response<Item> findItemById(Long id) {
        Response<Item> resp = new Response<Item>();
        try {
            Item item = itemDao.load(id);
            if (item == null){
                log.warn("item(id={}) isn't exist", id);
                resp.setError("item.not.exist");
                return resp;
            }
            resp.setResult(item);
        } catch (Exception e){
            log.error("failed to find item(id={}), cause: {}", id, Throwables.getStackTraceAsString(e));
            resp.setError("item.find.fail");
        }
        return resp;
    }

    /**
     * 根据id查找商品(缓存5分钟)
     *
     * @param id 商品id
     * @return 商品
     */
    @Override
    public Response<Item> findItemByIdWithCache(Long id) {
        Response<Item> resp = new Response<Item>();
        try {
            resp.setResult(itemsCache.getUnchecked(id));
        } catch (Exception e){
            log.error("failed to find item(id={}), cause: {}", id, Throwables.getStackTraceAsString(e));
            resp.setError("item.find.fail");
        }
        return resp;
    }

    @Override
    public Response<Optional<Item>> findItemByOuterItemId(String outerItemId) {
        try {
            return Response.ok(itemDao.findByOuterItemId(outerItemId));
        } catch (Exception e) {
            log.error("find item by outerItemId={} failed, cause:{}",
                    outerItemId, Throwables.getStackTraceAsString(e));
            return Response.fail("item.find.fail");
        }
    }

    @Override
    public Response<List<Item>> findItemsByIds(List<Long> ids) {
        Response<List<Item>> resp = new Response<List<Item>>();
        try {
            resp.setResult(itemDao.loads(ids));
        } catch (Exception e){
            log.error("failed to find items by ids({}), cause: {}", ids, Throwables.getStackTraceAsString(e));
            resp.setError("item.find.fail");
        }
        return resp;
    }

    @Override
    public Response<List<Item>> findItemsByIdsStr(String ids) {
        try {
            ids = MoreObjects.firstNonNull(ids, "");
            List<Long> itemIds = FluentIterable.from(Splitters.COMMA.split(ids)).transform(Longs.stringConverter()).toSet().asList();
            if (Iters.emptyToNull(itemIds) == null) return Response.ok(Collections.<Item>emptyList());
            return findItemsByIds(itemIds);
        } catch (Exception e) {
            log.error("convert idsStr to ids failed, ids={}", ids);
            return Response.fail("item.id.invalid");
        }
    }

    @Override
    public Response<ItemDetail> findItemDetailByItemId(Long itemId) {
        Response<ItemDetail> resp = new Response<ItemDetail>();
        try {
            ItemDetail detail = itemDetailDao.findByItemId(itemId);
            if (detail == null){
                log.warn("item detail(itemId={}) isn't exist.", itemId);
                resp.setError("item.detail.not.exist");
                return resp;
            }
            resp.setResult(detail);
        } catch (Exception e){
            log.error("failed to find item details by item id({}), cause: {}", itemId, Throwables.getStackTraceAsString(e));
            resp.setError("item.detail.find.fail");
        }
        return resp;
    }

    @Override
    public Response<ItemFullDetail> findItemFullDetailById(@Nullable Long itemId) {
        if (itemId == null) {
            // 渲染创建商品组件会到这里
            return Response.ok();
        }
        try {
            Item item = itemDao.load(itemId);
            if (item == null){
                itemCacheDao.setFullDetail(itemId, null);
                log.warn("item(id={}) isn't exist.", itemId);
                return Response.fail("item.not.exist");
            }
            List<Sku> skus = skuDao.findByItemId(itemId);
            List<SkuWithLvPrice> skuWithLvPrices = Lists.newArrayList();
            for (Sku sku : skus) {
                SkuWithLvPrice sp = new SkuWithLvPrice();
                sp.setSku(sku);
                sp.setPrices(skuPriceDao.findBySkuId(sku.getId()));
                skuWithLvPrices.add(sp);
            }

            ItemDetail itemDetail = itemDetailDao.findByItemId(itemId);

            FullItem fullItem = new FullItem();
            // item
            fullItem.setItem(item);
            // 计算折扣
            String discount = NumberUtils.divide(item.getPrice(), item.getOriginPrice(), 1, 10);
            fullItem.setDiscount(discount);
            // item detail
            fullItem.setItemDetail(itemDetail);
            // ship fee
            // TODO maybe should be computed later
            fullItem.setShipFee(shipFeeDao.findByItemId(item.getId()));
            // sku
            fullItem.setSkus(skuWithLvPrices);
            // skuGroup / skuGroupWithImage
            SkuGroup skuGroup = new SkuGroup(skus);
            fullItem.setSkuGroup(skuGroup.getAttributes());
            fullItem.setSkuAttrs(genSkuAttrs(item.getSpuId(), skus));
            fullItem.setSkuAttributes(genSkuAttributes(itemDetail, skus));
            fullItem.setAttributes(categoryCacher.findSpuRichAttributes(item.getSpuId()));
            // 是否进索引
            fullItem.setIndexable(item.getBitMark() == null || (item.getBitMark() & Item.INDEXABLE) != 0);

            ItemFullDetail fullDetail = new ItemFullDetail();
            fullDetail.setFullItem(fullItem);
            // 模板商品
            ItemTemplate itemTemplate = itemTemplateDao.findBySpuId(item.getSpuId());
            if (itemTemplate != null){
                fullDetail.setItemTemplate(itemTemplate);
            }
            return Response.ok(fullDetail);
        } catch (Exception e){
            log.error("failed to find item dto(itemId={}), cause: {}", itemId, Throwables.getStackTraceAsString(e));
            return Response.fail("item.find.fail");
        }
    }

    @Override
    public Response<ItemFullDetail> findItemFullDetailByIdWithCache(long itemId) {
        try {
            ItemFullDetail fullDetail = itemCacheDao.getFullDetail(itemId);
            if (fullDetail != null) {
                if (fullDetail.getFullItem() == null || fullDetail.getFullItem().getItem() == null ||
                        fullDetail.getFullItem().getItem().getId() == null) {
                    log.warn("item(id={}) isn't exist.", itemId);
                    return Response.fail("item.not.exist");
                }
                return Response.ok(fullDetail);
            }
            fullDetail = RespHelper.orServEx(findItemFullDetailById(itemId));
            itemCacheDao.setFullDetail(itemId, fullDetail);
            return Response.ok(fullDetail);
        } catch (ServiceException e) {
            log.warn("find item full detail by id with cache, itemId={}, error:{}",
                    itemId, e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("find item full detail by id with cache, itemId={}, cause:{}",
                    itemId, Throwables.getStackTraceAsString(e));
            return Response.fail("item.find.fail");
        }
    }

    @Override
    public Response<ItemDescription> findItemDescriptionById(long itemId) {
        return RespHelper.unwrap(findItemDescriptionByIdAllowNotFound(itemId), "item.description.not.found");
    }

    @Override
    public Response<Optional<ItemDescription>> findItemDescriptionByIdAllowNotFound(long itemId) {
        try {
            String json = itemDescriptionRedisDao.get(itemId);
            if (json == null) {
                return Response.ok(Optional.<ItemDescription>absent());
            }
            ItemDescription desc = JsonMapper.nonDefaultMapper().fromJson(json, ItemDescription.class);
            return Response.ok(Optional.of(desc));
        } catch (Exception e) {
            log.error("find item description by id={} failed, cause:{}",
                    itemId, Throwables.getStackTraceAsString(e));
            return Response.fail("item.find.fail");
        }
    }

    @Override
    public Response<ItemDescription> findItemDescriptionByIdAllowNotFoundNullSafe(@Nullable Long itemId) {
        if (itemId == null) {
            return Response.ok(null);
        }
        Response<Optional<ItemDescription>> resp = findItemDescriptionByIdAllowNotFound(itemId);
        if (!resp.isSuccess()) {
            return Response.fail(resp.getError());
        }
        return Response.ok(resp.getResult().orNull());
    }

    private List<AttributeKeyValues2<AttributeKey, ItemAttrValue>> genSkuAttributes(ItemDetail itemDetail, @Nullable List<Sku> skus) {
        if (Iters.emptyToNull(skus) == null) {
            return Collections.emptyList();
        }

        List<ItemAttrImage> images;
        if (itemDetail == null || Strings.isNullOrEmpty(itemDetail.getAttrImages())) {
            images = Collections.emptyList();
        } else {
            images = JsonMapper.nonDefaultMapper().fromJson(itemDetail.getAttrImages(),
                    JsonMapper.nonDefaultMapper().createCollectionType(List.class, ItemAttrImage.class));
        }

        Map<Long, AttributeKey> keys = new HashMap<>();
        Multimap<Long, ItemAttrValue> values = ArrayListMultimap.create();
        for (Sku sku : skus) {
            for (Optional<AttributeKeyValues2<AttributeKey, ItemAttrValue>> kvOpt : Arrays.asList(
                    refineKey2(sku.getAttributeKey1(), sku.getAttributeKeyId1(), sku.getAttributeName1(), sku.getAttributeValue1(), images),
                    refineKey2(sku.getAttributeKey2(), sku.getAttributeKeyId2(), sku.getAttributeName2(), sku.getAttributeValue2(), images))) {
                AttributeKeyValues2<AttributeKey, ItemAttrValue> kv = kvOpt.orNull();
                if (kv == null) continue;
                AttributeKey key = kv.getKey();
                keys.put(key.getId(), key);
                for (final ItemAttrValue value : Iters.nullToEmpty(kv.getValues())) {
                    if (FluentIterable.from(Iters.nullToEmpty(values.get(key.getId())))
                            .anyMatch(new Predicate<ItemAttrValue>() {
                                @Override
                                public boolean apply(ItemAttrValue input) {
                                    return Objects.equals(input.getId(), value.getId());
                                }
                            })) {
                        continue;
                    }
                    values.put(key.getId(), value);
                }
            }
        }
        List<AttributeKeyValues2<AttributeKey, ItemAttrValue>> kvs = new ArrayList<>();
        for (AttributeKey attributeKey : keys.values()) {
            AttributeKeyValues2<AttributeKey, ItemAttrValue> kv = new AttributeKeyValues2<>();
            kv.setKey(attributeKey);
            kv.setValues(ImmutableList.copyOf(values.get(attributeKey.getId())));
            kvs.add(kv);
        }
        return kvs;
    }

    private Optional<AttributeKeyValues2<AttributeKey, ItemAttrValue>> refineKey2(String key, Long keyId, String value, String valueIdStr, List<ItemAttrImage> images) {
        if (keyId == null || valueIdStr == null) {
            log.warn("key invalid, refine failed, keyId={}, key={}, valueId={}, value={}");
            return Optional.absent();
        }
        AttributeKey attrKey = new AttributeKey();
        attrKey.setId(keyId);
        attrKey.setName(key);
        attrKey.setValueType(CategoryAttributeKey.KeyType.ENUM.value());
        ItemAttrValue attrValue = new ItemAttrValue();
        long valueId;
        if (CharMatcher.DIGIT.matchesAllOf(valueIdStr)) {
            valueId = Long.valueOf(valueIdStr);
        } else {
            return Optional.absent();
        }
        attrValue.setId(valueId);
        attrValue.setValue(value);
        for (ItemAttrImage image : images) {
            if (Objects.equals(keyId, image.getKeyId()) && Objects.equals(valueId, image.getValueId())) {
                attrValue.setImage(image.getImage());
                break;
            }
        }

        AttributeKeyValues2<AttributeKey, ItemAttrValue> kv = new AttributeKeyValues2<>();
        kv.setKey(attrKey);
        kv.setValues(ImmutableList.of(attrValue));
        return Optional.of(kv);
    }

    @Deprecated
    private List<AttributeKeyValues> genSkuAttrs(Long spuId, @Nullable List<Sku> skus) {
        if (spuId == null || Iters.emptyToNull(skus) == null) {
            return Collections.emptyList();
        }
        Spu spu = spuDao.load(spuId);
        if (spu == null || spu.getCategoryId() == null) {
            return Collections.emptyList();
        }
        long categoryId = spu.getCategoryId();
        Map<Long, AttributeKey> keys = new HashMap<>();
        Multimap<Long, AttributeValue> values = ArrayListMultimap.create();
        for (Sku sku : skus) {
            for (Optional<AttributeKeyValues> kvOpt : Arrays.asList(
                    refineKey(categoryId, sku.getAttributeKey1(), sku.getAttributeKeyId1(), sku.getAttributeName1(), sku.getAttributeValue1()),
                    refineKey(categoryId, sku.getAttributeKey2(), sku.getAttributeKeyId2(), sku.getAttributeName2(), sku.getAttributeValue2()))) {
                AttributeKeyValues kv = kvOpt.orNull();
                if (kv == null) continue;
                AttributeKey key = kv.getAttributeKey();
                keys.put(key.getId(), key);
                for (final AttributeValue value : Iters.nullToEmpty(kv.getAttributeValues())) {
                    if (FluentIterable.from(Iters.nullToEmpty(values.get(key.getId())))
                            .anyMatch(new Predicate<AttributeValue>() {
                                @Override
                                public boolean apply(AttributeValue input) {
                                    return Objects.equals(input.getId(), value.getId());
                                }
                            })) {
                        continue;
                    }
                    values.put(key.getId(), value);
                }
            }
        }
        List<AttributeKeyValues> kvs = new ArrayList<>();
        for (AttributeKey attributeKey : keys.values()) {
            AttributeKeyValues kv = new AttributeKeyValues();
            kv.setAttributeKey(attributeKey);
            kv.setAttributeValues(ImmutableList.copyOf(values.get(attributeKey.getId())));
            kvs.add(kv);
        }
        return kvs;
    }

    @Deprecated
    private Optional<AttributeKeyValues> refineKey(long categoryId, String key, Long keyId, String value, String valueIdStr) {
        if (keyId == null || valueIdStr == null) {
            log.warn("key invalid, refine failed, keyId={}, key={}, valueId={}, value={}");
            return Optional.absent();
        }
        AttributeKey attrKey = new AttributeKey();
        attrKey.setId(keyId);
        attrKey.setName(key);
        attrKey.setValueType(CategoryAttributeKey.KeyType.ENUM.value());
        AttributeValue attrValue = new AttributeValue();
        long valueId;
        if (CharMatcher.DIGIT.matchesAllOf(valueIdStr)) {
            valueId = Long.valueOf(valueIdStr);
        } else {
            return Optional.absent();
        }
        attrValue.setId(valueId);
        attrValue.setValue(value);
        CategoryAttributeValue catValue = categoryAttributeValueDao.findBy(categoryId, keyId, valueId).orNull();
        if (catValue != null) {
            attrValue.setLogo(catValue.getLogo());
        }
        AttributeKeyValues kv = new AttributeKeyValues();
        kv.setAttributeKey(attrKey);
        kv.setAttributeValues(ImmutableList.of(attrValue));
        return Optional.of(kv);
    }

    @Override
    public Response<List<RichAttribute>> findItemAttributes(Long itemId) {
        Response<List<RichAttribute>> resp = new Response<List<RichAttribute>>();
        try {
            Item item = itemDao.load(itemId);
            if (item == null){
                log.warn("item(id={}) isn't exist.", itemId);
                resp.setError("item.not.exist");
                return resp;
            }
            resp.setResult(categoryCacher.findSpuRichAttributes(item.getSpuId()));
        } catch (Exception e){
            log.error("failed to find item attributes(itemId={}), cause: {}", itemId, Throwables.getStackTraceAsString(e));
            resp.setError("item.attributes.find.fail");
        }
        return resp;
    }

    @Override
    public Response<Paging<Item>> findItemsAsSeller(BaseUser loginer, Integer pageNo, Integer pageSize, Map<String, Object> criteria) {
        Response<Paging<Item>> resp = new Response<Paging<Item>>();
        try {
            PageInfo page = new PageInfo(pageNo, pageSize);
            if (criteria == null) {
                criteria = Maps.newHashMap();
            }
            fillDefaultOrdinaryItemTypes(criteria);
            criteria.put("userId", loginer.getId());
            criteria.put("offset", page.getOffset());
            criteria.put("limit", page.getLimit());
            Map<String, Object> nonNullAndEmpty = prepareCriteria(criteria);
            resp.setResult(itemDao.findItemsAsSeller(nonNullAndEmpty));
        } catch (Exception e){
            log.error("failed to find items as seller(id={}), pageNo={}, pageSize={}, criteria={}, cause: {}",
                    loginer.getId(), pageNo, pageSize, criteria, Throwables.getStackTraceAsString(e));
            resp.setError("item.find.fail");
        }
        return resp;
    }

    private int yuanToFen(double yuan) {
        return (int) Math.ceil(yuan * 100);
    }

    private Map<String, Object> prepareCriteria(Map<String, Object> criteria) {
        if (criteria.get("priceFrom") != null){
            String priceFrom = String.valueOf(criteria.get("priceFrom"));
            if (!Strings.isNullOrEmpty(priceFrom)){
                criteria.put("priceFrom", yuanToFen(Double.valueOf(priceFrom)));
            }
        }
        if (criteria.get("priceTo") != null){
            String priceTo = String.valueOf(criteria.get("priceTo"));
            if (!Strings.isNullOrEmpty(priceTo)){
                criteria.put("priceTo", yuanToFen(Double.valueOf(priceTo)));
            }
        }

        criteria.putAll(DayRange.from(
                Strings.emptyToNull((String) criteria.get("onShelfFrom")),
                Strings.emptyToNull((String) criteria.get("onShelfTo"))
        ).toMap("onShelfFrom", "onShelfTo"));

        criteria.putAll(DayRange.from(
                Strings.emptyToNull((String) criteria.get("offShelfFrom")),
                Strings.emptyToNull((String) criteria.get("offShelfTo"))
        ).toMap("offShelfFrom", "offShelfTo"));

        // 多状态
        String statuses = (String)criteria.get("statuses");
        if (!Strings.isNullOrEmpty(statuses)){
            List<String> statusesList = COMMA_SPLITTER.splitToList(statuses);
            criteria.put("statuses", statusesList);
        }
        return Params.filterNullOrEmpty(criteria);
    }

    /**
     * 分页获取商品，运营后台使用
     * @param pageNo   起始页码
     * @param pageSize 分页大小
     * @param criteria 查询条件
     * @return 商品分页列表
     */
    @Override
    public Response<Paging<Item>> pagingItems(Integer pageNo, Integer pageSize, Map<String, Object> criteria) {
        Response<Paging<Item>> resp = new Response<Paging<Item>>();
        try {
            PageInfo page = new PageInfo(pageNo, pageSize);
            if (criteria == null) {
                criteria = new HashMap<>();
            }
            fillDefaultOrdinaryItemTypes(criteria);
            criteria.putAll(DayRange.from(
                    Strings.emptyToNull((String) criteria.get("updatedFrom")),
                    Strings.emptyToNull((String) criteria.get("updatedTo"))
            ).toMap("updatedFrom", "updatedTo"));

            Long userId = filterOutUserId(criteria);
            String tag = (String)criteria.get("tag");
            if (userId != null && tag != null) {
                tag = Params.trimToNull(tag);
                List<Long> itemIds;
                if (tag == null) {
                    // 未分类标签
                    itemIds = itemTagDao.findUnKnownItems(userId, 0, Integer.MAX_VALUE).getData();
                } else {
                    // 其他标签
                    itemIds = itemTagDao.findItemsByTag2paging(userId, tag, 0, Integer.MAX_VALUE).getData();
                }
                if (Iters.emptyToNull(itemIds) == null) {
                    return Response.ok(Paging.<Item>empty());
                }
                criteria.put("ids", itemIds);
            }

            resp.setResult(itemDao.paging(page.getOffset(), page.getLimit(), Params.filterNullOrEmpty(criteria)));
        } catch (Exception e){
            log.error("failed to find items(pageNo={}, pageSize={}, criteria={}), cause: {}",
                    pageNo, pageSize, criteria, Throwables.getStackTraceAsString(e));
            resp.setError("item.find.fail");
        }
        return resp;
    }

    private @Nullable Long nullParse(@Nullable Object value) {
        String str = Params.trimToNull(value);
        return str != null ? Long.valueOf(str) : null;
    }

    private @Nullable Long filterOutUserId(Map<String, Object> criteria) {
        Long userId = nullParse(criteria.remove("userId"));
        Long shopId = nullParse(criteria.remove("shopId"));
        if (userId == null && shopId != null) {
            Paging<Item> pg = itemDao.paging(0, 1, MapBuilder.<String, Object>newHashMap().put("shopId", shopId).map());
            if (pg.getTotal() <= 0 || Iters.emptyToNull(pg.getData()) == null) {
                return null;
            }
            userId = pg.getData().get(0).getUserId();
        }
        if (userId != null) criteria.put("userId", userId);
        return userId;
    }

    @Override
    public Response<Long> countOnShelfItemBySpuId(Long spuId) {
        Response<Long> resp = new Response<Long>();
        try {
            resp.setResult(itemDao.countBySpuId(spuId));
        } catch (Exception e){
            log.error("failed to count item by spu id({}), cause: {}", spuId, Throwables.getStackTraceAsString(e));
            resp.setError("item.count.fail");
        }
        return resp;
    }

    @Override
    public Response<Long> countOnShelfItemByShopId(Long shopId) {
        Response<Long> resp = new Response<Long>();
        try {
            resp.setResult(itemDao.countOnShelfByShopId(shopId, itemTypeSorter.ordinaryItemTypes()));
        } catch (Exception e){
            log.error("failed to count on shelf item by shop id({}), cause: {}", shopId, Throwables.getStackTraceAsString(e));
            resp.setError("item.count.fail");
        }
        return resp;
    }

    @Override
    public Response<Long> countOnShelfItemByUserId(long userId) {
        try {
            return Response.ok(itemDao.countOnShelfByUserId(userId, itemTypeSorter.ordinaryItemTypes()));
        } catch (Exception e) {
            log.error("count on shelf item by userId={} failed, cause:{}", userId, Throwables.getStackTraceAsString(e));
            return Response.fail("item.count.fail");
        }
    }

    @Override
    public Response<Long> countOffShelfItemByShopId(long shopId) {
        try {
            return Response.ok(itemDao.countOffShelfByShopId(shopId, itemTypeSorter.ordinaryItemTypes()));
        } catch (Exception e) {
            log.error("count off shelf item by shopId={} failed, cause:{}", shopId, Throwables.getStackTraceAsString(e));
            return Response.fail("item.count.fail");
        }
    }

    @Override
    public Response<Long> countOffShelfItemByUserId(long userId) {
        try {
            return Response.ok(itemDao.countOffShelfByUserId(userId, itemTypeSorter.ordinaryItemTypes()));
        } catch (Exception e) {
            log.error("count off shelf item by userId={} failed, cause:{}", userId, Throwables.getStackTraceAsString(e));
            return Response.fail("item.count.fail");
        }
    }

    @Override
    public Response<List<Item>> findItemsBySpuId(Long spuId) {
        Response<List<Item>> resp = new Response<List<Item>>();
        try {
            Item criteria = new Item();
            criteria.setSpuId(spuId);
            List<Item> items = itemDao.list(JSON_NON_DEFAULT_MAPPER.getMapper().convertValue(criteria, Map.class));
            if (Iterables.isEmpty(items)){
                log.warn("items(spuId={}) isn't exist.", spuId);
                resp.setError("item.find.fail");
                return resp;
            }
            resp.setResult(items);
        } catch (Exception e){
            log.error("failed to find items by spu id({}), cause: {}", spuId, Throwables.getStackTraceAsString(e));
            resp.setError("item.find.fail");
        }
        return resp;
    }

    @Override
    public Response<List<Item>> findOnShelfItemsBySpuId(Long spuId) {
        Response<List<Item>> resp = new Response<List<Item>>();
        try {
            Item criteria = new Item();
            criteria.setSpuId(spuId);
            criteria.setStatus(Item.Status.ON_SHELF.value());
            List<Item> items = itemDao.list(JSON_NON_DEFAULT_MAPPER.getMapper().convertValue(criteria, Map.class));
            if (Iterables.isEmpty(items)){
                log.warn("items(spuId={}) isn't exist.", spuId);
                resp.setError("item.find.fail");
                return resp;
            }
            resp.setResult(items);
        } catch (Exception e){
            log.error("failed to find items by spu id({}), cause: {}", spuId, Throwables.getStackTraceAsString(e));
            resp.setError("item.find.fail");
        }
        return resp;
    }

    @Override
    public Response<RichFullSpu> findRichFullSpuByItemId(Long id) {
        Response<RichFullSpu> resp = new Response<RichFullSpu>();
        try {
            Item item = itemDao.load(id);
            if (item == null){
                log.warn("item(id={}) isn't exist.", id);
                resp.setError("item.not.exist");
                return resp;
            }
            Spu spu = spuDao.load(item.getSpuId());
            if (spu == null){
                log.warn("spu(id={}) isn't exist.", item.getSpuId());
                resp.setError("spu.not.exist");
                return resp;
            }
            Brand brand = brandDao.load(spu.getBrandId());
            if (brand == null){
                log.warn("brand(id={}) isn't exist.", spu.getBrandId());
                resp.setError("brand.not.exist");
                return resp;
            }
            List<RichAttribute> richAttributes = categoryCacher.findSpuRichAttributes(spu.getId());
            Map<String, String> attrsMap = Maps.newHashMap();
            //refine spu attribute map, not-include sku
            for(RichAttribute ra : richAttributes) {
                if(!attrsMap.containsKey(ra.getAttributeKey())) {
                    attrsMap.put(ra.getAttributeKey(), ra.getAttributeValue());
                }else {
                    String update = attrsMap.get(ra.getAttributeKey())+", "+ra.getAttributeValue();
                    attrsMap.put(ra.getAttributeKey(), update);
                }
            }
            RichFullSpu richFullSpu = new RichFullSpu();
            richFullSpu.setSpu(spu);
            richFullSpu.setBrandName(brand.getName());
            richFullSpu.setAttributes(attrsMap);
            resp.setResult(richFullSpu);
        } catch (Exception e){
            log.error("failed to find rich spu by item id({}), cause: ", id, Throwables.getStackTraceAsString(e));
            resp.setError("spu.find.fail");
        }
        return resp;
    }

    /**
     * 根据SPU.id获取商品模板
     * @param spuId SPU.id
     * @return 商品模板
     */
    @Override
    public Response<ItemTemplate> findItemTemplateBySpuId(Long spuId) {
        Response<ItemTemplate> resp = new Response<ItemTemplate>();
        try {
            ItemTemplate template = itemTemplateDao.findBySpuId(spuId);
            if (template == null){
                log.warn("failed to find item template by spu id({})", spuId);
                resp.setError("item.template.not.exist");
                return resp;
            }
            resp.setResult(template);
        } catch (Exception e){
            log.error("failed to find item template by spu id({}), cause: {}", spuId, Throwables.getStackTraceAsString(e));
            resp.setError("item.template.find.fail");
        }
        return resp;
    }

    /**
     * 根据SPU.id获取商品模板详情: 商品模板, SKU, SkuGroup
     * @param spuId 标品id
     * @return 商品模板详情
     */
    @Override
    public Response<ItemTemplateFullDetail> findItemTemplateFullDetailBySpuId(Long spuId) {
        Response<ItemTemplateFullDetail> resp = new Response<ItemTemplateFullDetail>();
        try {
            ItemTemplateFullDetail templateFullDetail = new ItemTemplateFullDetail();
            ItemTemplate template = itemTemplateDao.findBySpuId(spuId);
            if (template == null){
                log.info("item template(id={}) isn't exist.", spuId);
                templateFullDetail.setTemplate(new ItemTemplate());
            } else {
                templateFullDetail.setTemplate(template);
                List<Sku> skus = JSON_NON_DEFAULT_MAPPER.fromJson(template.getJsonSkus(), SKU_LIST_TYPE);
                if (skus != null && !Iterables.isEmpty(skus)){
                    templateFullDetail.setSkus(skus);
                    SkuGroup skuGroup = new SkuGroup(skus);
                    templateFullDetail.setSkuGroup(skuGroup);
                    templateFullDetail.setSkuAttrs(genSkuAttrs(spuId, skus));
                }
            }
            resp.setResult(templateFullDetail);
        } catch (Exception e){
            log.error("failed to find item template by spu id({}), cause: {}", spuId, Throwables.getStackTraceAsString(e));
            resp.setError("item.template.find.fail");
        }
        return resp;
    }

    /**
     * 根据SPU.id获取dFullDetaiISpu
     * @param spuId SPU.id
     * @return FullDetaiISpu
     */
    @Override
    public Response<FullDetailSpu> findFullDetaiISpuBySpuId(Long spuId) {
        Response<FullDetailSpu> resp = new Response<FullDetailSpu>();
        try {
            FullDetailSpu fullDetaiISpu = new FullDetailSpu();

            // spu
            Spu spu = spuDao.load(spuId);
            if (spu == null){
                log.warn("spu(id={}) isn't exist.", spuId);
                resp.setError("spu.not.exist");
                return resp;
            }
            fullDetaiISpu.setSpu(spu);

            // rich attributes
            Response<List<RichAttribute>> richAttributesResp = categoryReadService.findSpuAttributesNoCache(spuId);
            if (!richAttributesResp.isSuccess()){
                resp.setError(richAttributesResp.getError());
                return resp;
            }
            List<RichAttribute> richAttributes = richAttributesResp.getResult();
            fullDetaiISpu.setSpuAttributes(richAttributes);

            // item template
            ItemTemplate template = itemTemplateDao.findBySpuId(spuId);
            if (template == null){
                log.info("item template(spuId={}) isn't exist.");
                fullDetaiISpu.setTemplate(new ItemTemplate());
            } else {
                fullDetaiISpu.setTemplate(template);
            }

            // attribute key values
            Response<List<AttributeKeyValues>> skuKeyValuesResp = categoryReadService.findSkuAttributes(spuId);
            if (!skuKeyValuesResp.isSuccess()) {
                resp.setError(skuKeyValuesResp.getError());
                return resp;
            }
            fullDetaiISpu.setSkuAttributes(skuKeyValuesResp.getResult());
            resp.setResult(fullDetaiISpu);
        } catch (Exception e){
            log.error("failed to find full detail spu by spu id({}), cause: {}",
                    spuId, Throwables.getStackTraceAsString(e));
            resp.setError("spu.full.detail.find.fail");
        }
        return resp;
    }

    private int getDiscountFromData(@Nullable String data) {
        data = Params.trimToNull(data);
        if (data != null) {
            ItemDiscountDto discountDto = JsonMapper.nonDefaultMapper().fromJson(data, ItemDiscountDto.class);
            if (discountDto != null && discountDto.getDiscount() != null) {
                return discountDto.getDiscount();
            }
        }
        // default 0
        return 0;
    }

    @Override
    public Response<Map<Long, ItemPriceDto>> bulkGetItemPrices(Set<Long> itemIds) {
        if (Iters.emptyToNull(itemIds) == null) {
            return RespHelper.ok(ImmutableMap.<Long, ItemPriceDto>of());
        }
        try {
            List<Item> items = RespHelper.orServEx(findItemsByIds(ImmutableList.copyOf(itemIds)));
            Map<Long, ItemPriceDto> result = FluentIterable.from(items)
                    .transform(new Function<Item, ItemPriceDto>() {
                        @Nullable
                        @Override
                        public ItemPriceDto apply(@Nullable Item item) {
                            if (item == null || item.getId() == null) return null;
                            long itemId = item.getId();
                            ItemPriceDto price = new ItemPriceDto();
                            price.setItemId(itemId);
                            price.setOriginPrice(MoreObjects.firstNonNull(item.getPrice(), 0));
                            String data = itemRedisDao.getItemDiscount(itemId).orNull();
                            price.setDiscount(getDiscountFromData(data));
                            price.setPrice(Math.max(0, price.getOriginPrice() - price.getDiscount()));
                            return price;
                        }
                    })
                    .filter(Predicates.notNull())
                    .uniqueIndex(new Function<ItemPriceDto, Long>() {
                        @Override
                        public Long apply(ItemPriceDto input) {
                            return input.getItemId();
                        }
                    });
            return Response.ok(result);
        } catch (ServiceException e) {
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("bulkGetItemPrices failed, itemIds={}, cause:{}",
                    itemIds, Throwables.getStackTraceAsString(e));
            return Response.fail("item.find.fail");
        }
    }

    @Override
    public Response<ItemPriceDto> getItemPrices(long itemId) {
        Response<Map<Long, ItemPriceDto>> resp = bulkGetItemPrices(ImmutableSet.of(itemId));
        if (!resp.isSuccess()) {
            return Response.fail(resp.getError());
        }

        ItemPriceDto price = resp.getResult().get(itemId);
        if (price == null) {
            return Response.fail("item.price.not.exist");
        }
        return Response.ok(price);
    }

    /**
     * 根据SPU编号列表查询多个商品模板
     *
     * @param spuIds SPU编号列表
     * @return 商品模板列表
     */
    @Override
    public Response<List<ItemTemplate>> findItemTemplatesBySpuIds(List<Long> spuIds) {
        Response<List<ItemTemplate>> resp = new Response<List<ItemTemplate>>();
        try {
            resp.setResult(itemTemplateDao.findBySpuIds(spuIds));
        } catch (Exception e){
            log.error("failed to find item templates by spu ids({}), cause: {}", spuIds, Throwables.getStackTraceAsString(e));
            resp.setError("item.template.find.fail");
        }
        return resp;
    }

    /**
     * 根据商品id和日期获取商品快照
     *
     * @param itemId 商品id
     * @param date   日期
     * @return 交易快照
     */
    @Override
    public Response<RichItemSnapshot> findRichItemSnapshot(Long itemId, Long skuId, Date date) {
        Response<RichItemSnapshot> res = new Response<RichItemSnapshot>();

        try {
            Item item = itemDao.load(itemId);
            checkState(notNull(item), "item.not.found");
            ItemSnapshot itemSnapshot = itemManager.loadItemSnapshot(itemId, date);
            checkState(notNull(itemSnapshot), "item.not.found");
            Sku sku = skuDao.load(skuId);

            RichItemSnapshot richItemSnapshot = new RichItemSnapshot();
            BeanMapper.copy(itemSnapshot, richItemSnapshot);


            Map<String, String> skuAttributes = Maps.newTreeMap();
            skuAttributes.put("attributeKey1", sku.getAttributeKey1());
            skuAttributes.put("attributeName1", sku.getAttributeName1());
            skuAttributes.put("attributeKey2", sku.getAttributeKey2());
            skuAttributes.put("attributeName2", sku.getAttributeName2());
            richItemSnapshot.setSkuAttributes(JSON_MAPPER.toJson(skuAttributes));


            checkState(notNull(sku), "sku.not.found");
            List<RichAttribute> attributes = categoryCacher.findSpuRichAttributes(item.getSpuId());
            richItemSnapshot.setAttributes(JSON_MAPPER.toJson(attributes));

            res.setResult(richItemSnapshot);

        } catch (IllegalStateException e) {
            log.warn("failed to find itemSnapshot with itemId:{}, skuId:{}, date:{}, error:{}",
                    itemId, skuId, DFT.print(new DateTime(date)), e.getMessage());
        } catch (Exception e) {
            log.error("failed to find itemSnapshot with itemId:{}, skuId:{}, date:{}, cause:{}",
                    itemId, skuId, DFT.print(new DateTime(date)), Throwables.getStackTraceAsString(e));
        }

        return res;
    }

    @Override
    public Response<RichSpu> findRichSpuByItemId(Long itemId) {
        Response<RichSpu> resp = new Response<RichSpu>();
        try {
            Item item = itemDao.load(itemId);
            if (item == null){
                log.warn("item(id={}) isn't exist.", itemId);
                throw new ServiceException("item.not.exist");
            }
            return categoryReadService.findRichSpuById(item.getSpuId());
        } catch (ServiceException e){
            resp.setError(e.getMessage());
        } catch (Exception e){
            log.error("failed to find rich spu by item id({}), cause: {}",
                    itemId, Throwables.getStackTraceAsString(e));
            resp.setError("spu.find.fail");
        }
        return resp;
    }

    @Override
    public Response<Long> getItemLastId() {
        Response<Long> result = new Response<Long>();

        try {

            Long lastId = itemDao.lastId();
            result.setResult(lastId);

        }catch (Exception e){
            log.error("fail to get item last id,cause:{}",Throwables.getStackTraceAsString(e));
            result.setError("get.item.last.id.fail");
        }

        return result;
    }

    @Override
    public Response<List<Item>> listItem(Long lastItemId, Integer size) {
        Response<List<Item>> result = new Response<List<Item>>();

        try {

            List<Item> items = itemDao.listTo(lastItemId, size);
            result.setResult(items);

        }catch (Exception e){
            log.error("fail to list item with params(lastItemId={},size={}),cause:{}",lastItemId,size,Throwables.getStackTraceAsString(e));
            result.setError("list.item.fail");
        }

        return result;
    }

    @Override
    public Response<List<Item>> findShopNewestItems(Long shopId, Integer limit) {
        Response<List<Item>> result = new Response<List<Item>>();

        try {

            limit = limit != null ? limit : 20;

            List<Item> items = itemDao.findShopNewestItems(shopId, limit, itemTypeSorter.ordinaryItemTypes());

            result.setResult(items);
            return result;
        }catch (Exception e) {
            log.error("fail to find shop newest items by shop id {}, limit {}, cause:{}",
                    shopId, limit, Throwables.getStackTraceAsString(e));
            result.setError("shop.newest.item.query.fail");
            return result;
        }
    }

    @Override
    public Response<Integer> dailyItemIncrement(String startAt, String endAt) {
        Response<Integer> result = new Response<Integer>();

        try {
            startAt = getValidStartAt(startAt);
            endAt = getValidEndAt(endAt);

            result.setResult(itemDao.dailyItemIncrement(startAt, endAt, itemTypeSorter.ordinaryItemTypes()));
            return result;
        }catch (Exception e) {
            log.error("fail to find daily increase item, cause:{}", e);
            result.setError("daily.item.increment.query.fail");
            return result;
        }
    }

    @Override
    public Response<Integer> dailyOnShelfItem(String startAt, String endAt) {
        Response<Integer> result = new Response<Integer>();

        try {
            startAt = getValidStartAt(startAt);
            endAt = getValidEndAt(endAt);

            result.setResult(itemDao.dailyOnShelfItem(startAt, endAt, itemTypeSorter.ordinaryItemTypes()));
            return result;
        }catch (Exception e) {
            log.error("fail to find daily on shelf item cause:{}", e);
            result.setError("daily.on.shelf.item.query.fail");
            return result;
        }
    }

    @Override
    public Response<Integer> dailyBrandIncrement(String startAt, String endAt) {
        Response<Integer> result = new Response<Integer>();

        try {
            startAt = getValidStartAt(startAt);
            endAt = getValidEndAt(endAt);

            result.setResult(brandDao.dailyBrandIncrement(startAt, endAt));
            return result;
        }catch (Exception e) {
            log.error("fail to find daily brand increment, cause:{}", e);
            result.setError("daily.brand.increment.query.fail");
            return result;
        }
    }

    @Override
    public Response<List<ItemWithCompanyBuy>> shopRecommendItems(String itemIdStr, Integer totalNum,
                                                                 Integer sortType, Long shopId) {
        Response<List<ItemWithCompanyBuy>> result = new Response<List<ItemWithCompanyBuy>>();

        if(isNull(totalNum)) {
            result.setResult(new ArrayList<ItemWithCompanyBuy>());
            return result;
        }

        try {

            List<Item> sortedItems = new ArrayList<Item>();
            totalNum = firstNonNull(totalNum, 8);
            List<Long> itemIds = Lists.newArrayList();
            int limit;

            if(!Strings.isNullOrEmpty(itemIdStr)) {
                itemIds = Splitters.splitToLong(itemIdStr, Splitters.COMMA);

                limit = totalNum - itemIds.size();

                List<Item> items = itemDao.loads(itemIds);
                Map<Long, Item> itemIdAndItem = Maps.uniqueIndex(items, new Function<Item, Long>() {
                    @Override
                    public Long apply(Item input) {
                        return input.getId();
                    }
                });

                for (Long itemId : itemIds) {
                    sortedItems.add(itemIdAndItem.get(itemId));
                }
            }else {
                limit = totalNum;
            }

            sortType = firstNonNull(sortType, 1);

            List<Item> saleOrNewest;
            if(equalWith(sortType, 1)) {
                if(itemIds.isEmpty()) {
                    saleOrNewest = itemDao.findShopSaleItem(shopId, limit, itemTypeSorter.ordinaryItemTypes());
                }else {
                    saleOrNewest = itemDao.findShopSaleItemWithoutItemIds(shopId, limit, itemIds, itemTypeSorter.ordinaryItemTypes());
                }
            }else {
                if(itemIds.isEmpty()) {
                    saleOrNewest = itemDao.findShopNewestItems(shopId, limit, itemTypeSorter.ordinaryItemTypes());
                }else {
                    saleOrNewest = itemDao.findShopNewestItemsWithoutItemIds(shopId, limit, itemIds, itemTypeSorter.ordinaryItemTypes());
                }
            }

            sortedItems.addAll(saleOrNewest);

            result.setResult(getItemWithCompanyBuy(sortedItems));
            return result;
        }catch (Exception e) {
            log.error("fail to find shop recommend item by shop id {}, itemIdStr {}, sortType {}, totalNum {}, cause:{}",
                    shopId, itemIdStr, sortType, totalNum, Throwables.getStackTraceAsString(e));
            result.setError("shop.recommend.item.query.fail");
            return result;
        }
    }

    private List<ItemWithCompanyBuy> getItemWithCompanyBuy(List<Item> items) {
        List<ItemWithCompanyBuy> result = new ArrayList<ItemWithCompanyBuy>();

        for(Item i : items) {
            ItemWithCompanyBuy itemWithCompanyBuy = new ItemWithCompanyBuy();
            itemWithCompanyBuy.setItem(i);

            result.add(itemWithCompanyBuy);
        }

        return result;
    }

    @Override
    public Response<List<SkuExtra>> getSkuExtrasBySkuIds(List<Long> skuIds) {
        try {
            if (skuIds == null || skuIds.size() < 1) {
                log.warn("skuIds is null or empty");
                return Response.fail("getSkuExtrasBySkuIds");
            }
            return Response.ok(skuExtraDao.findBySkuIds(skuIds));
        } catch (Exception e) {
            log.error("fail to find sku extras by sku ids {}, cause:{}", skuIds, Throwables.getStackTraceAsString(e));
            return Response.fail("sku.extra.find.fail");
        }
    }

    public String getValidStartAt(String startAt) {
        if(Strings.isNullOrEmpty(startAt)) {
            startAt = DFT.print(DateTime.now().minusDays(1).withTimeAtStartOfDay());
        }

        return startAt;
    }

    public String getValidEndAt(String endAt) {
        if(Strings.isNullOrEmpty(endAt)) {
            endAt = DFT.print(DateTime.now().withTimeAtStartOfDay());
        }

        return endAt;
    }

    @Override
    public Response<Integer> getSkuPrice(Long skuId, SkuPrice.MODE mode, @Nullable Integer lv) {
        try {
            List<SkuPrice> prices = skuPriceDao.findBySkuId(skuId);
            Optional<Integer> priceOpt = SkuPriceSelector.select(prices, mode, lv);
            if (!priceOpt.isPresent()) {
                log.warn("select price of sku failed, skuId={}, mode={}, lv={}", skuId, mode, lv);
                return Response.fail("sku.price.no.exist");
            }
            return Response.ok(priceOpt.get());
        } catch (Exception e) {
            log.error("get sku price failed, skuId={}, mode={}, lv={}, cause:{}",
                    skuId, mode, lv, Throwables.getStackTraceAsString(e));
            return Response.fail("sku.get.price.fail");
        }
    }

    @Override
    public Response<Map<Long, Integer>> getSkuPrices(List<Long> skuIds, final SkuPrice.MODE mode, @Nullable final Integer lv) {
        try {
            final List<SkuPrice> prices = skuPriceDao.findInSkuIds(skuIds);
            Map<Long, Collection<SkuPrice>> cache = Multimaps.index(prices, new Function<SkuPrice, Long>() {
                @Override
                public Long apply(SkuPrice input) {
                    return input.getSkuId();
                }
            }).asMap();
            Map<Long, Integer> result = Maps.transformValues(cache, new Function<Collection<SkuPrice>, Integer>() {
                @Nullable
                @Override
                public Integer apply(Collection<SkuPrice> input) {
                    return SkuPriceSelector.select(ImmutableList.copyOf(input), mode, lv).orNull();
                }
            });
            return Response.ok(Maps.filterValues(result, Predicates.notNull()));
        } catch (Exception e) {
            log.error("get sku prices failed, skuIds={}, cause:{}",
                    skuIds, Throwables.getStackTraceAsString(e));
            return Response.fail("sku.get.price.fail");
        }
    }

    @Override
    public Response<Set<String>> findTagsByItemId(Long itemId) {
        Response<Set<String>> res = new Response<>();

        try {
            Set<String> tags = itemTagDao.findTagsByItemId(itemId);
            res.setResult(tags);
        } catch (Exception e) {
            log.error("get tags of item(id:{}) failed, cause:{}", itemId, Throwables.getStackTraceAsString(e));
            res.setError("item.tags.query.fail");
        }

        return res;
    }

    /**
     * 获取最后一个商品的id
     * @return  商品id
     */
    @Override
    public Response<Long> lastId() {
        Response<Long> res = new Response<>();
        try {

            Long lastId = itemDao.lastId();
            res.setResult(lastId);
        } catch (Exception e) {
            log.error("fail to query lastId of items, cause:{}", Throwables.getStackTraceAsString(e));
            res.setError("item.last.query.fail");
        }

        return res;
    }

    /**
     * 查询id小于lastId内的limit个商品
     * @param lastId 最大的商品id
     * @param limit 商品个数
     * @return id小于lastId内的pageSize个商品
     */
    @Override
    public Response<List<Item>> listTo(Long lastId, int limit) {
        Response<List<Item>> res = new Response<>();
        try {

            List<Item> items = itemDao.listTo(lastId, limit);
            res.setResult(items);
        } catch (Exception e) {
            log.error("fail to query items with lastId:{}, limit:{}, cause:{}",lastId, limit,
                    Throwables.getStackTraceAsString(e));
            res.setError("item.query.fail");
        }
        return res;
    }


    /**
     * 查询id小于lastId内且更新时间大于since的limit个商品
     * @param lastId lastId 最大的商品id
     * @param since 起始更新时间
     * @param limit 商品个数
     * @return id小于lastId内且更新时间大于since的limit个商品
     */
    @Override
    public Response<List<Item>> listSince(Long lastId, String since, int limit) {
        Response<List<Item>> res = new Response<>();
        try {
            List<Item> items = itemDao.listSince(lastId, since, limit);
            res.setResult(items);
        } catch (Exception e) {
            log.error("fail to query items with lastId:{}, since:{}, limit:{}", lastId, since, limit);
            res.setError("item.query.fail");
        }
        return res;
    }

    @Override
    public Response<FavoriteItem> getFavItemById(Long id) {
        Response<FavoriteItem> response=new Response<FavoriteItem>();
        try {
            FavoriteItem item=favoriteItemDao.load(id);
            response.setResult(item);
        }catch (Exception ex){
            log.error("get fav item fail, id={}, cause={}", id, ex.getMessage());
            response.setError("get.fav.item.by.id.fail");
        }
        return response;
    }

    @Override
    public Response<List<ItemAddedToFavoriteDto>> hasAddedToFavorites(Long userId, List<Long> itemIds){
        Response<List<ItemAddedToFavoriteDto>> response=new Response<>();

        if(itemIds==null || itemIds.size()==0){
            response.setResult(null);
            return response;
        }
        try{
            Map<String, Object> criteria=new HashMap<>();
            criteria.put("userId", userId);
            criteria.put("itemIds", itemIds);
            List<FavoriteItem> items=favoriteItemDao.list(criteria);

            Set<Long> favItemIds=new HashSet<>();
            for(FavoriteItem item : items){
               favItemIds.add(item.getItemId());
            }

            List<ItemAddedToFavoriteDto> dtoList=new ArrayList<>();
            for(Long id: itemIds){
                if(favItemIds.contains(id)){
                    dtoList.add(new ItemAddedToFavoriteDto(id, Boolean.TRUE));
                }else{
                    dtoList.add(new ItemAddedToFavoriteDto(id, Boolean.FALSE));
                }
            }
            response.setResult(dtoList);

        }catch (Exception ex){
            log.error("hasAddToFavorites failed, userId={}, itemIds={}, cause={}",userId,itemIds,ex.getMessage());
            response.setError("get.item.fav.status.error");
        }
        return response;
    }

    @Override
    public Response<Paging<FavoriteItem>> pagingFavItems(BaseUser user, Integer pageNo, Integer pageSize, Map<String,Object> criteria) {
        Response<Paging<FavoriteItem>> response=new Response<>();
        Long userId=user.getId();
        if(criteria==null){
            criteria=new HashMap<>();
        }
        try{
            PageInfo page = new PageInfo(pageNo, pageSize);
            Map<String, Object> nonNullAndEmpty = Params.filterNullOrEmpty(criteria);
            nonNullAndEmpty.put("userId", userId);
            response.setResult(favoriteItemDao.paging(page.getOffset(), page.getLimit(), nonNullAndEmpty));
        }catch (Exception ex){
            log.error("paging fav item fail, criteria={}, cause={}", criteria, ex.getMessage());
            response.setError("paging.fav.item.fail");
        }

        return response;
    }

    @Override
    public Response<List<RichAttribute>> findSkuAttributes(long itemId) {
        try {
            Item item = itemDao.load(itemId);
            if (item == null) {
                return Response.fail("item.not.exist");
            }
            List<RichAttribute> result = new ArrayList<>();
            List<Sku> skus = skuDao.findByItemId(itemId);
            for (Sku sku : Iters.nullToEmpty(skus)) {
                if (!Strings.isNullOrEmpty(sku.getAttributeKey1())) {
                    RichAttribute attribute = new RichAttribute();
                    attribute.setBelongId(item.getSpuId());
                    attribute.setAttributeKeyId(sku.getAttributeKeyId1());
                    attribute.setAttributeKey(sku.getAttributeKey1());
                    attribute.setAttributeValue(sku.getAttributeName1());
                    attribute.setAttributeValueId(Strs.parseLong(sku.getAttributeValue1()).orNull());
                    boolean exist = false;
                    for (RichAttribute richAttribute : result) {
                        if (Objects.equals(richAttribute.getAttributeKey(), attribute.getAttributeKey())
                                && Objects.equals(richAttribute.getAttributeValue(), attribute.getAttributeValue())) {
                            exist = true;
                            break;
                        }
                    }
                    if (!exist) {
                        result.add(attribute);
                    }
                }
                if (!Strings.isNullOrEmpty(sku.getAttributeKey2())) {
                    RichAttribute attribute = new RichAttribute();
                    attribute.setBelongId(item.getSpuId());
                    attribute.setAttributeKeyId(sku.getAttributeKeyId2());
                    attribute.setAttributeKey(sku.getAttributeKey2());
                    attribute.setAttributeValue(sku.getAttributeName2());
                    attribute.setAttributeValueId(Strs.parseLong(sku.getAttributeValue2()).orNull());
                    boolean exist = false;
                    for (RichAttribute richAttribute : result) {
                        if (Objects.equals(richAttribute.getAttributeKey(), attribute.getAttributeKey())
                                && Objects.equals(richAttribute.getAttributeValue(), attribute.getAttributeValue())) {
                            exist = true;
                            break;
                        }
                    }
                    if (!exist) {
                        result.add(attribute);
                    }
                }
            }
            return Response.ok(result);
        } catch (Exception e) {
            log.error("find sku attributes failed, itemId={}, cause:{}", itemId, Throwables.getStackTraceAsString(e));
            return Response.fail("sku.attribute.find.fail");
        }
    }
}
