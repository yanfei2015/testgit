/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package io.terminus.parana.category.cache;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.terminus.parana.category.dto.RichAttribute;
import io.terminus.parana.category.dto.RichCategory;
import io.terminus.parana.category.model.AttributeKey;
import io.terminus.parana.category.model.AttributeValue;
import io.terminus.parana.category.model.BackCategory;
import io.terminus.parana.category.model.FrontCategory;
import io.terminus.parana.category.model.Spu;
import io.terminus.zookeeper.pubsub.SubscribeCallback;
import io.terminus.zookeeper.pubsub.Subscriber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * 缓存器
 * Author: haolin
 * On: 8/31/14
 */
@Slf4j
public class CategoryCacher extends BaseCacher {

    @Autowired(required = false)
    private Subscriber cacheListener;

    /**
     * 后台类目Cache
     */
    private LoadingCache<Long, BackCategory> backCategoriesCache;

    /**
     * 后台类目及孩子类目Cache, {backCategoryId : RichCategory}
     */
    private LoadingCache<Long, RichCategory<BackCategory>> backCategoryChildrenCache;

    /**
     * 后台类目树Cache, {backCategoryId : 所在类目树}
     */
    private LoadingCache<Long, List<BackCategory>> backCategoryTreeCache;

    /**
     * 前台类目Cache
     */
    private LoadingCache<Long, FrontCategory> frontCategoriesCache;

    /**
     * 前台类目及孩子类目Cache, {frontCategoryId : RichCategory}
     */
    private LoadingCache<Long, RichCategory<FrontCategory>> frontCategoryChildrenCache;

    /**
     * 前台类目树Cache, {frontCategoryId : 所在类目树}
     */
    private LoadingCache<Long, List<FrontCategory>> frontCategoryTreeCache;

    /**
     * 前后叶子台类目绑定Cache
     */
    private LoadingCache<Long, List<BackCategory>> frontBindingsCache;

    /**
     * 前台类目绑定的类目id列表
     */
    private LoadingCache<Long, List<Long>> front2backCache;

    /**
     * 后台类目id与属性键列表Cache
     */
    private LoadingCache<Long, List<AttributeKey>> categoryKeysCache;

    /**
     * 后台类目某个属性键与其属性值列表Cache
     * KEY: categoryId +"."+ attributeKeyId
     * VALUE: attributeValues
     */
    private LoadingCache<String, List<AttributeValue>> categoryKeyValuesCache;

    /**
     * SPU的Cache
     */
    private LoadingCache<Long, Spu> spusCache;

    /**
     * SPU的属性Cache
     */
    private LoadingCache<Long, List<RichAttribute>> spuAttributesCache;

    /**
     * SKU键Cache
     */
    private LoadingCache<Long, List<AttributeKey>> skuKeysCache;

    /**
     * 类目的SPU缓存
     */
    private LoadingCache<Long, List<Spu>> categorySpusCache;

    private static final Splitter SPLITTER = Splitter.on(".");

    private static final Joiner JOINER = Joiner.on(".");

    public CategoryCacher(){

        backCategoriesCache = CacheBuilder.newBuilder().build(new CacheLoader<Long, BackCategory>() {
            @Override
            public BackCategory load(Long id) throws Exception {
                return backCategoryDao.load(id);
            }
        });

        backCategoryChildrenCache = CacheBuilder.newBuilder().build(new CacheLoader<Long, RichCategory<BackCategory>>() {
            @Override
            public RichCategory<BackCategory> load(Long id) throws Exception {
                return doLoadRichBackCategory(id);
            }
        });

        backCategoryTreeCache = CacheBuilder.newBuilder().build(new CacheLoader<Long, List<BackCategory>>() {
            @Override
            public List<BackCategory> load(Long id) throws Exception {
                return doLoadBackCategoryTree(id);
            }
        });

        frontCategoriesCache = CacheBuilder.newBuilder().build(new CacheLoader<Long, FrontCategory>() {
            @Override
            public FrontCategory load(Long id) throws Exception {
                return frontCategoryDao.load(id);
            }
        });

        frontCategoryChildrenCache = CacheBuilder.newBuilder().build(new CacheLoader<Long, RichCategory<FrontCategory>>() {
            @Override
            public RichCategory<FrontCategory> load(Long id) throws Exception {
                return doLoadRichFrontCategory(id);
            }
        });

        frontCategoryTreeCache = CacheBuilder.newBuilder().build(new CacheLoader<Long, List<FrontCategory>>() {
            @Override
            public List<FrontCategory> load(Long id) throws Exception {
                return doLoadFrontCategoryTree(id);
            }
        });

        frontBindingsCache = CacheBuilder.newBuilder().build(new CacheLoader<Long, List<BackCategory>>() {
            @Override
            public List<BackCategory> load(Long id) throws Exception {
                return doLoadBackCategoriesByFid(id);
            }
        });

        categoryKeysCache = CacheBuilder.newBuilder().build(
            new CacheLoader<Long, List<AttributeKey>>() {
                @Override
                public List<AttributeKey> load(Long key) throws Exception {
                    return attributeKeyDao.findByCategoryId(key);
                }
            });

        categoryKeyValuesCache = CacheBuilder.newBuilder().build(
            new CacheLoader<String, List<AttributeValue>>() {
                @Override
                public List<AttributeValue> load(String key) throws Exception {
                    List<String> parts = SPLITTER.splitToList(key);
                    Long categoryId = Long.parseLong(parts.get(0));
                    Long attributeKeyId = Long.parseLong(parts.get(1));
                    return attributeValueDao.findByCategoryIdAndKeyId(categoryId, attributeKeyId);
                }
            }
        );

        spusCache = CacheBuilder.newBuilder().build(new CacheLoader<Long, Spu>() {
            @Override
            public Spu load(Long id) throws Exception {
                return spuDao.load(id);
            }
        });

        spuAttributesCache = CacheBuilder.newBuilder().build(
            new CacheLoader<Long, List<RichAttribute>>() {
                @Override
                public List<RichAttribute> load(Long spuId) throws Exception {
                    return doLoadSpuAttributesBySpuId(spuId);
                }
            }
        );

        skuKeysCache = CacheBuilder.newBuilder().build(new CacheLoader<Long, List<AttributeKey>>() {
            @Override
            public List<AttributeKey> load(Long spuId) throws Exception {
                return doLoadSpuSkuKeysBySpuId(spuId);
            }
        });

        categorySpusCache = CacheBuilder.newBuilder().build(
                new CacheLoader<Long, List<Spu>>() {
                    @Override
                    public List<Spu> load(Long categoryId) throws Exception {
                        return doLoadBackCategorySpusByBid(categoryId);
                    }
                }
        );

        front2backCache = CacheBuilder.newBuilder().build(
                new CacheLoader<Long, List<Long>>() {
                    @Override
                    public List<Long> load(Long frontCategoryId) throws Exception {
                        return recursiveGetBackCategoryIds(frontCategoryId);
                    }
                }
        );
    }

    /**
     * 查询前台叶子类目绑定的后台类目列表
     * @param frontCategoryId 前台类目id
     * @return 前台类目绑定的后台类目列表
     */
    public List<BackCategory> findBackCategoriesByFrontCategoryId(Long frontCategoryId){
        return frontBindingsCache.getUnchecked(frontCategoryId);
    }

    /**
     * 查询SPU
     * @param spuId SPU ID
     * @return SPU
     */
    public Spu findSpuById(Long spuId){
        return spusCache.getUnchecked(spuId);
    }

    /**
     * 查询类目的属性键列表
     * @param categoryId 类目id
     * @return 属性键列表
     */
    public List<AttributeKey> findBackCategoryAttributeKeys(Long categoryId) {
        return categoryKeysCache.getUnchecked(categoryId);
    }

    /**
     * 查询类目某个属性键的属性值列表
     * @param categoryId 类目id
     * @param attributeKeyId 属性键id
     * @return 属性值列表
     */
    public List<AttributeValue> findAttributeValuesBidAndAttrKeyId(Long categoryId, Long attributeKeyId) {
        return categoryKeyValuesCache.getUnchecked(JOINER.join(categoryId, attributeKeyId));
    }

    /**
     * 查询SPU的属性值列表
     * @param spuId SPU.id
     * @return 属性值列表
     */
    public List<RichAttribute> findSpuRichAttributes(Long spuId) {
        return spuAttributesCache.getUnchecked(spuId);
    }


    /**
     * 查询类目的SPU列表
     * @param categoryId 类目id
     * @return SPU列表
     */
    public List<Spu> findSpusByBackCategoryId(Long categoryId) {
        return categorySpusCache.getUnchecked(categoryId);
    }

    /**
     * 查询前台类目绑定的所有后台类目id
     * @param frontCategoryId 前台类目id
     * @return 前台类目绑定的所有后台类目id
     */
    public List<Long> findBackCategoryIdsByFrontCategoryId(Long frontCategoryId) {
        return front2backCache.getUnchecked(frontCategoryId);
    }

    /**
     * 查询后台类目
     * @param backCategoryId 后台类目id
     * @return 后台类目
     */
    public BackCategory findBackCategoryById(Long backCategoryId){
        return backCategoriesCache.getUnchecked(backCategoryId);
    }

    /**
     * 查询前台类目
     * @param frontCategoryId 前台类目id
     * @return 前台类目
     */
    public FrontCategory findFrontCategoryById(Long frontCategoryId) {
        return frontCategoriesCache.getUnchecked(frontCategoryId);
    }

    /**
     * 查询后台类目及其孩子类目列表
     * @param backCategoryId 后台类目ID
     * @return 后台类目及其孩子类目列表
     */
    public RichCategory<BackCategory> findChildrenByBackCategoryId(Long backCategoryId){
        return backCategoryChildrenCache.getUnchecked(backCategoryId);
    }

    /**
     * 查询前台类目及其孩子类目列表
     * @param frontCategoryId 前台台类目ID
     * @return 前台类目及其孩子类目列表
     */
    public RichCategory<FrontCategory> findChildrenByFrontCategoryId(Long frontCategoryId){
        return frontCategoryChildrenCache.getUnchecked(frontCategoryId);
    }

    /**
     * 查询后台类目所在类目树
     * @param id 后台类目ID
     * @return 后台类目所在树
     */
    public List<BackCategory> findBackCategoryTreeByBid(Long id){
        return backCategoryTreeCache.getUnchecked(id);
    }

    /**
     * 查询后台类目所在类目树
     * @param id 后台类目ID
     * @return 后台类目所在树
     */
    public List<FrontCategory> findFrontCategoryTreeByBid(Long id){
        return frontCategoryTreeCache.getUnchecked(id);
    }

    @PostConstruct
    public void listenCache(){
        if (cacheListener == null) {
            return;
        }
        try {
            cacheListener.subscribe(new SubscribeCallback() {
                @Override
                public void fire(byte[] data) {
                    CacheMessage m = CacheMessage.from(data);
                    if (Objects.equal(CacheMessage.SYNC_BACKEND, m)){
                        clearBackend();
                    } else if (Objects.equal(CacheMessage.SYNC_FRONTEND, m)){
                        clearFrontend();
                    }
                }
            });
        } catch (Exception e) {
            log.error("failed to subscribe cache event, cause: {}", Throwables.getStackTraceAsString(e));
        }
    }

    /**
     * 清理前台类目缓存
     */
    @Override
    public void clearFrontend() {
        log.info("begin to invalidate frontend category caches");
        frontCategoriesCache.invalidateAll();
        frontCategoryChildrenCache.invalidateAll();
        frontCategoryTreeCache.invalidateAll();
        frontBindingsCache.invalidateAll();
        front2backCache.invalidateAll();
        log.info("end to invalidate frontend category caches");
    }

    /**
     * 清理后台类目缓存
     */
    @Override
    public void clearBackend() {
        log.info("begin to invalidate backend category caches");
        backCategoriesCache.invalidateAll();
        backCategoryChildrenCache.invalidateAll();
        backCategoryTreeCache.invalidateAll();
        categoryKeysCache.invalidateAll();
        categoryKeyValuesCache.invalidateAll();
        spuAttributesCache.invalidateAll();
        spusCache.invalidateAll();
        skuKeysCache.invalidateAll();
        categorySpusCache.invalidateAll();
        log.info("end to invalidate backend category caches");
    }

    /**
     * 清理所有缓存
     */
    @Override
    public void clear(){
        log.info("begin to invalidate all category caches");
        backCategoriesCache.invalidateAll();
        backCategoryChildrenCache.invalidateAll();
        backCategoryTreeCache.invalidateAll();
        frontCategoriesCache.invalidateAll();
        frontCategoryChildrenCache.invalidateAll();
        frontCategoryTreeCache.invalidateAll();
        frontBindingsCache.invalidateAll();
        categoryKeysCache.invalidateAll();
        categoryKeyValuesCache.invalidateAll();
        spuAttributesCache.invalidateAll();
        spusCache.invalidateAll();
        skuKeysCache.invalidateAll();
        categorySpusCache.invalidateAll();
        front2backCache.invalidateAll();
        log.info("end to invalidate all category caches");
    }
}
