/*
 *
 *  * Copyright (c) 2014 杭州端点网络科技有限公司
 *
 */

package io.terminus.parana.category.cache;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import io.terminus.common.exception.ServiceException;
import io.terminus.parana.category.dao.mysql.BackCategoryDao;
import io.terminus.parana.category.dao.mysql.CategoryAttributeKeyDao;
import io.terminus.parana.category.dao.mysql.CategoryAttributeValueDao;
import io.terminus.parana.category.dao.mysql.CategoryBindingDao;
import io.terminus.parana.category.dao.mysql.FrontCategoryDao;
import io.terminus.parana.category.dao.mysql.SpuDao;
import io.terminus.parana.category.dao.redis.AttributeKeyDao;
import io.terminus.parana.category.dao.redis.AttributeValueDao;
import io.terminus.parana.category.dao.redis.SpuAttributeDao;
import io.terminus.parana.category.dto.RichAttribute;
import io.terminus.parana.category.dto.RichCategory;
import io.terminus.parana.category.model.AttributeKey;
import io.terminus.parana.category.model.AttributeValue;
import io.terminus.parana.category.model.BackCategory;
import io.terminus.parana.category.model.CategoryBinding;
import io.terminus.parana.category.model.FrontCategory;
import io.terminus.parana.category.model.Spu;
import io.terminus.parana.category.model.SpuAttribute;
import io.terminus.zookeeper.pubsub.SubscribeCallback;
import io.terminus.zookeeper.pubsub.Subscriber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;

/**
 * Author: haolin
 * On: 12/5/14
 */
@Slf4j
public abstract class BaseCacher {

    @Autowired
    protected FrontCategoryDao frontCategoryDao;

    @Autowired
    protected BackCategoryDao backCategoryDao;

    @Autowired
    protected CategoryBindingDao categoryBindingDao;

    @Autowired
    protected SpuDao spuDao;

    @Autowired
    protected AttributeKeyDao attributeKeyDao;

    @Autowired
    protected AttributeValueDao attributeValueDao;

    @Autowired
    protected SpuAttributeDao spuAttributeDao;

    @Autowired(required = false)
    protected Subscriber cacheListener;

    @Autowired
    protected CategoryAttributeKeyDao categoryAttributeKeyDao;

    @Autowired
    protected CategoryAttributeValueDao categoryAttributeValueDao;

    /**
     * 从DB加载后台类目的SPU列表
     * @param categoryId 后台类目ID
     * @return 后台类目的SPU列表
     */
    public List<Spu> doLoadBackCategorySpusByBid(Long categoryId) {
        Spu criteria = new Spu();
        criteria.setCategoryId(categoryId);
        return spuDao.list(criteria);
    }

    /**
     * 从DB加载后台类目所在类目树
     * @param id 后台类目ID
     * @return 后台类目所在类目树
     */
    public List<BackCategory> doLoadBackCategoryTree(Long id) {
        BackCategory category = backCategoryDao.load(id);
        if (category == null){
            log.warn("back category(id={}) isn't existed.", id);
            throw new ServiceException("category.not.exist");
        }
        List<BackCategory> tree = Lists.newArrayList();
        tree.add(category);
        Long pid = category.getPid();
        while (pid != null && pid > 0L){
            category = backCategoryDao.load(pid);
            tree.add(category);
            pid = category.getPid();
        }
        return ImmutableList.copyOf(Lists.reverse(tree));
    }

    /**
     * 从DB加载前台类目所在类目树
     * @param id 前台类目ID
     * @return 前台类目所在类目树
     */
    public List<FrontCategory> doLoadFrontCategoryTree(Long id) {
        FrontCategory category = frontCategoryDao.load(id);
        if (category == null){
            log.warn("front category(id={}) isn't existed.", id);
            return Collections.emptyList();
        }
        List<FrontCategory> tree = Lists.newArrayList();
        tree.add(category);
        Long pid = category.getPid();
        while (pid != null && pid > 0L){
            category = frontCategoryDao.load(pid);
            tree.add(category);
            pid = category.getPid();
        }
        return ImmutableList.copyOf(Lists.reverse(tree));
    }

    /**
     * 从DB加载后台类目及其孩子类目列表
     * @param id 后台类目ID
     * @return 后台类目及其孩子类目列表
     */
    public RichCategory<BackCategory> doLoadRichBackCategory(Long id) {
        RichCategory<BackCategory> richCategory = new RichCategory<BackCategory>();
        BackCategory parent;
        if (Objects.equal(id, 0L)){
            parent = new BackCategory();
            parent.setPid(0L);
            parent.setLevel(0);
        } else {
            parent = backCategoryDao.load(id);
        }
        richCategory.setParent(parent);
        richCategory.setChildren(backCategoryDao.findByPid(id));
        return richCategory;
    }

    /**
     * 从DB加载前台类目及其孩子类目列表
     * @param id 前台类目ID
     * @return 前台类目及其孩子类目列表
     */
    public RichCategory<FrontCategory> doLoadRichFrontCategory(Long id) {
        RichCategory<FrontCategory> richCategory = new RichCategory<FrontCategory>();
        FrontCategory parent;
        if (Objects.equal(id, 0L)){
            parent = new FrontCategory();
            parent.setPid(0L);
            parent.setLevel(0);
        } else {
            parent = frontCategoryDao.load(id);
        }
        richCategory.setParent(parent);
        richCategory.setChildren(frontCategoryDao.findByPid(id));
        return richCategory;
    }

    /**
     * 从DB加载前台类目绑定的后台类目列表
     * @param frontCategoryId 前台类目ID
     * @return 前台类目绑定的后台类目列表
     */
    public List<BackCategory> doLoadBackCategoriesByFid(Long frontCategoryId){
        List<CategoryBinding> bindings = categoryBindingDao.findByFrontCategoryId(frontCategoryId);
        if (Iterables.isEmpty(bindings)){
            return Collections.emptyList();
        }
        List<Long> backCategoryIds = Lists.transform(bindings, new Function<CategoryBinding, Long>() {
            @Override
            public Long apply(CategoryBinding binding) {
                return binding.getBackCategoryId();
            }
        });
        return backCategoryDao.loads(backCategoryIds);
    }

    /**
     * 递归获取后台叶子类目id列表
     * @param frontCategoryId 前台类目id
     * @return 后台叶子类目id列表
     */
    protected List<Long> recursiveGetBackCategoryIds(Long frontCategoryId) {
        List<Long> backCategoryIds = Lists.newArrayList();
        FrontCategory frontCategory = frontCategoryDao.load(frontCategoryId);
        if (!frontCategory.getHasChildren()) {
            // 前台叶子类目
            List<CategoryBinding> bindings = categoryBindingDao.findByFrontCategoryId(frontCategoryId);
            if (!Iterables.isEmpty(bindings)) {
                backCategoryIds.addAll(Lists.transform(bindings, new Function<CategoryBinding, Long>() {
                    @Override
                    public Long apply(CategoryBinding binding) {
                        return binding.getBackCategoryId();
                    }
                }));
            }
        } else {
            // 前台非叶子类目
            List<FrontCategory> children = frontCategoryDao.findByPid(frontCategoryId);
            for (FrontCategory child : children){
                backCategoryIds.addAll(recursiveGetBackCategoryIds(child.getId()));
            }
        }
        return backCategoryIds;
    }

    /**
     * 将SpuAttribute对象转换为RichAttribute对象
     * @param spuId SPU.id
     * @param spuAttribute SpuAttribute
     * @return RichAttribute
     */
    protected RichAttribute toRichAttribute(Long spuId, SpuAttribute spuAttribute) {
        Long keyId = spuAttribute.getAttributeKeyId();
        Long valueId = spuAttribute.getAttributeValueId();
        String keyName = attributeKeyDao.findById(keyId).getName();
        String valueName = attributeValueDao.findById(valueId).getValue();
        RichAttribute richAttribute = new RichAttribute();
        richAttribute.setBelongId(spuId);
        richAttribute.setAttributeKeyId(keyId);
        richAttribute.setAttributeKey(keyName);
        richAttribute.setAttributeValueId(valueId);
        richAttribute.setAttributeValue(valueName);
        return richAttribute;
    }

    /**
     * 查询前台叶子类目绑定的后台类目列表
     * @param frontCategoryId 前台类目id
     * @return 前台类目绑定的后台类目列表
     */
    public List<BackCategory> findBackCategoriesByFrontCategoryId(Long frontCategoryId){
        return doLoadBackCategoriesByFid(frontCategoryId);
    }

    /**
     * 查询SPU
     * @param spuId SPU ID
     * @return SPU
     */
    public Spu findSpuById(Long spuId){
        return spuDao.load(spuId);
    }

    /**
     * 查询SPU的属性列表
     * @param spuId SPU.id
     * @return 属性列表
     */
    public List<RichAttribute> doLoadSpuAttributesBySpuId(Long spuId) {
        List<SpuAttribute> spuAttributes = spuAttributeDao.findBySpuId(spuId);
        List<RichAttribute> richAttributes = Lists.newArrayListWithCapacity(spuAttributes.size());
        for (SpuAttribute spuAttribute : spuAttributes) {
            richAttributes.add(toRichAttribute(spuId, spuAttribute));
        }
        return richAttributes;
    }

    /**
     * 查询类目的属性键列表
     * @param categoryId 类目id
     * @return 属性键列表
     */
    public List<AttributeKey> findBackCategoryAttributeKeys(Long categoryId) {
        return attributeKeyDao.findByCategoryId(categoryId);
    }

    /**
     * 查询类目某个属性键的属性值列表
     * @param categoryId 类目id
     * @param attributeKeyId 属性键id
     * @return 属性值列表
     */
    public List<AttributeValue> findAttributeValuesBidAndAttrKeyId(Long categoryId, Long attributeKeyId) {
        return attributeValueDao.findByCategoryIdAndKeyId(categoryId, attributeKeyId);
    }

    /**
     * 查询SPU的属性值列表
     * @param spuId SPU.id
     * @return 属性值列表
     */
    public List<RichAttribute> findSpuRichAttributes(Long spuId) {
        return doLoadSpuAttributesBySpuId(spuId);
    }

    /**
     * 查询SPU的SKU键列表
     * @param spuId SPU.id
     * @return 属性键列表
     */
    public List<AttributeKey> doLoadSpuSkuKeysBySpuId(final Long spuId) {
        // TODO(Effet): test
        List<AttributeKey> keys = attributeKeyDao.findSkuKeysBySpuId(spuId);
        return FluentIterable.from(keys)
                .transform(new Function<AttributeKey, AttributeKey>() {
                    @Override
                    public AttributeKey apply(AttributeKey input) {
                        AttributeKey key = new AttributeKey();
                        key.setId(input.getId());
                        key.setName(input.getName());
                        // FIXME(Effet): dirty hack
                        Spu spu = spuDao.load(spuId);
                        // real type
                        key.setValueType(categoryAttributeKeyDao.findBy(spu.getCategoryId(), input.getId()).get().getKeyType());
                        return key;
                    }
                })
                .toList();
    }

    /**
     * 查询类目的SPU列表
     * @param categoryId 类目id
     * @return SPU列表
     */
    public List<Spu> findSpusByBackCategoryId(Long categoryId) {
        return doLoadBackCategorySpusByBid(categoryId);
    }

    /**
     * 查询前台类目绑定的所有后台类目id
     * @param frontCategoryId 前台类目id
     * @return 前台类目绑定的所有后台类目id
     */
    public List<Long> findBackCategoryIdsByFrontCategoryId(Long frontCategoryId) {
        return recursiveGetBackCategoryIds(frontCategoryId);
    }

    /**
     * 查询后台类目
     * @param backCategoryId 后台类目id
     * @return 后台类目
     */
    public BackCategory findBackCategoryById(Long backCategoryId){
        return backCategoryDao.load(backCategoryId);
    }

    /**
     * 查询前台类目
     * @param frontCategoryId 前台类目id
     * @return 前台类目
     */
    public FrontCategory findFrontCategoryById(Long frontCategoryId) {
        return frontCategoryDao.load(frontCategoryId);
    }

    /**
     * 查询后台类目及其孩子类目列表
     * @param backCategoryId 后台类目ID
     * @return 后台类目及其孩子类目列表
     */
    public RichCategory<BackCategory> findChildrenByBackCategoryId(Long backCategoryId){
        return doLoadRichBackCategory(backCategoryId);
    }

    /**
     * 查询前台类目及其孩子类目列表
     * @param frontCategoryId 前台台类目ID
     * @return 前台类目及其孩子类目列表
     */
    public RichCategory<FrontCategory> findChildrenByFrontCategoryId(Long frontCategoryId){
        return doLoadRichFrontCategory(frontCategoryId);
    }

    /**
     * 查询后台类目所在类目树
     * @param id 后台类目ID
     * @return 后台类目所在树
     */
    public List<BackCategory> findBackCategoryTreeByBid(Long id){
        return doLoadBackCategoryTree(id);
    }

    /**
     * 查询后台类目所在类目树
     * @param id 后台类目ID
     * @return 后台类目所在树
     */
    public List<FrontCategory> findFrontCategoryTreeByBid(Long id){
        return doLoadFrontCategoryTree(id);
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
    public void clearFrontend() {
        log.info("begin to invalidate frontend category caches");
        log.info("dev environment ^.^");
        log.info("end to invalidate frontend category caches");
    }

    /**
     * 清理后台类目缓存
     */
    public void clearBackend() {
        log.info("begin to invalidate backend category caches");
        log.info("dev environment ^.^");
        log.info("end to invalidate backend category caches");
    }

    /**
     * 清理所有缓存
     */
    public void clear() {
        log.info("begin to invalidate all category caches");
        log.info("dev environment ^.^");
        log.info("end to invalidate all category caches");
    }
}
