/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package io.terminus.parana.category.service;

import com.google.common.base.*;
import com.google.common.base.Objects;
import com.google.common.collect.*;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.Joiners;
import io.terminus.pampas.common.Response;
import io.terminus.parana.category.cache.BaseCacher;
import io.terminus.parana.category.cache.CacheMessage;
import io.terminus.parana.category.dao.mysql.*;
import io.terminus.parana.category.dao.redis.AttributeIndexDao;
import io.terminus.parana.category.dao.redis.AttributeKeyDao;
import io.terminus.parana.category.dao.redis.AttributeValueDao;
import io.terminus.parana.category.dao.redis.SpuAttributeDao;
import io.terminus.parana.category.dto.AttributeDto;
import io.terminus.parana.category.dto.CatImportData;
import io.terminus.parana.category.dto.RichSpu;
import io.terminus.parana.category.dto.SpuImportData;
import io.terminus.parana.category.manager.CategoryManager;
import io.terminus.parana.category.model.*;
import io.terminus.parana.common.event.CoreEventDispatcher;
import io.terminus.parana.common.util.Iters;
import io.terminus.parana.common.util.Params;
import io.terminus.parana.common.util.RespHelper;
import io.terminus.parana.event.category.*;
import io.terminus.parana.event.misc.CategoryPermEvent;
import io.terminus.parana.event.spu.SpuAltEvent;
import io.terminus.parana.event.spu.SpuDisableEvent;
import io.terminus.parana.event.spu.SpuEnableEvent;
import io.terminus.parana.event.spu.SpuInsEvent;
import io.terminus.parana.item.dao.mysql.BrandDao;
import io.terminus.parana.item.dto.ImportResult;
import io.terminus.parana.item.model.Brand;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import io.terminus.zookeeper.pubsub.Publisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static io.terminus.parana.common.util.ServiceUtils.checkResult;

/**
 * 类目写服务实现
 * Author: haolin
 * On: 8/29/14
 */
@Service @Slf4j
public class CategoryWriteServiceImpl implements CategoryWriteService {

    @Autowired
    private BackCategoryDao backCategoryDao;

    @Autowired
    private FrontCategoryDao frontCategoryDao;

    @Autowired
    private CategoryBindingDao categoryBindingDao;

    @Autowired
    private CategoryManager categoryManager;

    @Autowired
    private SpuDao spuDao;

    @Autowired
    private AttributeKeyDao attributeKeyDao;

    @Autowired
    private AttributeValueDao attributeValueDao;

    @Autowired
    private AttributeIndexDao attributeIndexDao;

    @Autowired
    private CategoryAttributeKeyDao categoryAttributeKeyDao;

    @Autowired
    private CategoryAttributeValueDao categoryAttributeValueDao;

    @Autowired
    private SpuAttributeDao spuAttributeDao;

    @Autowired(required = false)
    private Publisher cachePublisher;

    @Autowired
    private BaseCacher categoryCacher;

    @Autowired
    private UserReadService userReadService;

    @Autowired
    private BrandDao brandDao;

    @Autowired
    private BackCategoryPermDao backCategoryPermDao;

    @Autowired
    private CoreEventDispatcher coreEventDispatcher;

    @Autowired
    private CategoryReadService categoryReadService;

    @Autowired
    private BrandReadService brandReadService;

    /**
     * SKU
     */
    private static final Predicate<AttributeDto> SKU_PREDICATE = new Predicate<AttributeDto>() {
        @Override
        public boolean apply(AttributeDto attributeDto) {
            if (attributeDto == null){
                return Boolean.FALSE;
            }
            return attributeDto.getIsSku();
        }
    };

    /**
     * 非枚举属性
     */
    public static final Predicate<AttributeDto> NON_ENUMERALE_PREDICATE = new Predicate<AttributeDto>() {
        @Override
        public boolean apply(AttributeDto attributeDto) {
            if (attributeDto == null){
                return Boolean.FALSE;
            }
            return Objects.equal(attributeDto.getType(), AttributeKey.ValueType.NOT_ENUM.value());
        }
    };

    @Override
    public Response<BackCategory> createBackCategory(BackCategory category) {
        Response<BackCategory> resp = new Response<BackCategory>();
        try {
            BackCategory existed = backCategoryDao.findByNameAndPid(category.getPid(), category.getName());
            if (existed == null){
                // 不存在, 可创建
                doCreateBackCategory(category);
                coreEventDispatcher.publish(new BackCategoryInsEvent(category.getId()));
                resp.setResult(category);
            } else {
                // 若未禁用状态, 则直接启用
                if (!BackCategory.isEnable(existed)){
                    existed.setStatus(BackCategory.Status.ENABLED.value());
                    categoryManager.enable(existed);
                    coreEventDispatcher.publish(new BackCategoryEnableEvent(existed.getId()));
                } else {
                    // 已存在
                    log.warn("back category(name={}) is existed.", existed.getName());
                    resp.setError("category.name.existed");
                    return resp;
                }
                resp.setResult(existed);
            }
        } catch (Exception e){
            log.error("failed to create back category({}), cause: {}", category, Throwables.getStackTraceAsString(e));
            resp.setError("category.create.fail");
        }
        return resp;
    }

    /**
     * 创建后台类目
     * @param category 后台类目
     */
    private void doCreateBackCategory(BackCategory category) {
        category.setStatus(BackCategory.Status.ENABLED.value());
        category.setHasChildren(Boolean.FALSE);
        category.setHasSpu(Boolean.FALSE);
        if (category.getPid() == null || category.getPid() == 0){
            // 无父级类目, 直接创建
            category.setPid(0L);
            category.setLevel(1);
            checkState(backCategoryDao.create(category), "category.create.fail");
        } else {
            // 有父级类目, 需要更新父级类目hasChildren
            categoryManager.create(category);
        }
    }

    @Override
    public Response<Boolean> updateBackCategory(long id, @Nullable String name, @Nullable Long shopBusinessId) {
        try {
            if (Strings.isNullOrEmpty(name) && shopBusinessId == null) {
                return Response.ok(Boolean.FALSE);
            }
            BackCategory category = checkBackCategoryExists(id);
            category.setName(Strings.emptyToNull(name));
            category.setShopBusinessId(shopBusinessId);
            backCategoryDao.update(category);
            coreEventDispatcher.publish(new BackCategoryAltEvent(id));
            return Response.ok(Boolean.TRUE);
        } catch (ServiceException e){
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("failed to update back category(id={}, name={}), cause: {}", id, name, Throwables.getStackTraceAsString(e));
            return Response.fail("category.update.fail");
        }
    }

    @Override
    public Response<Boolean> disableBackCategory(Long id) {
        Response<Boolean> resp = new Response<Boolean>();
        try {
            BackCategory category = checkBackCategoryExists(id);
            checkBeforeDisable(category);
            if (BackCategory.isEnable(category)){
                doDisable(category);
                coreEventDispatcher.publish(new BackCategoryDisableEvent(id));
            }
            resp.setResult(Boolean.TRUE);
        } catch (ServiceException e){
            resp.setError(e.getMessage());
        } catch (Exception e){
            log.error("failed to delete back category(id={}), cause: {}",
                    id, Throwables.getStackTraceAsString(e));
            resp.setError("category.disable.fail");
        }
        return resp;
    }

    /**
     * 禁用后台类目
     * @param category 后台类目
     */
    private void doDisable(BackCategory category) {
        if (category.getPid() == 0){
            // 无父级, 直接disable
            backCategoryDao.setStatus(category.getId(), BackCategory.Status.DISABLED.value());
        } else {
            categoryManager.disable(category);
        }
    }

    @Override
    public Response<Boolean> enableBackCategory(Long id) {
        Response<Boolean> resp = new Response<Boolean>();
        try {
            BackCategory category = checkBackCategoryExists(id);
            if (!BackCategory.isEnable(category)){
                doEnable(category);
                coreEventDispatcher.publish(new BackCategoryEnableEvent(id));
            }
            resp.setResult(Boolean.TRUE);
        } catch (ServiceException e){
            resp.setError(e.getMessage());
        } catch (Exception e){
            log.error("failed to enable back category(id={}), cause: {}", id, Throwables.getStackTraceAsString(e));
            resp.setError("category.enable.fail");
        }
        return resp;
    }

    @Override
    public Response<Boolean> updateOrCreateBackCategoryAllows(Long userId, Map<Long, Integer> allows) {
        try {
            checkArgument(userId != null && userId > 0, "no user selected for update brand perm");

            Response<User> userResp = userReadService.findById(userId);
            if (!userResp.isSuccess()) {
                log.warn("find user failed for brand perm, userId={}, {}", userId, userResp.getError());
                return Response.fail(userResp.getError());
            }

            User user = userResp.getResult();
            if (user == null || user.getType() == null) {
                log.warn("userReadService.findById may error, no user returned or user Type is null");
                return Response.fail("user.find.fail");
            }

            String allow = Joiners.COMMA.join(
                    Maps.transformEntries(
                            Maps.filterValues(allows, Predicates.not(Predicates.equalTo(0))),
                            new Maps.EntryTransformer<Long, Integer, String>() {
                                @Override
                                public String transformEntry(Long key, Integer value) {
                                    if (value == 1) return "h" + key;
                                    else return String.valueOf(key);
                                }
                            }
                    ).values()
            );

            Optional<BackCategoryPerm> permOpt = backCategoryPermDao.findOne(userId, user.getType());

            if (!permOpt.isPresent()) {
                // 查询所有此类用户默认权限
                permOpt = backCategoryPermDao.findOne(-1l, user.getType());
                checkState(permOpt.isPresent(), "user id=%s, type=%s haven't default perm data",
                        userId, user.getType());

                BackCategoryPerm toCreate = new BackCategoryPerm();
                toCreate.setUserId(userId);
                toCreate.setUserType(user.getType());
                toCreate.setAllow(MoreObjects.firstNonNull(allow, permOpt.get().getAllow()));
                toCreate.setDeny(permOpt.get().getDeny());
                toCreate.setBrandAllow(permOpt.get().getBrandAllow());

                backCategoryPermDao.create(toCreate);
            } else {
                BackCategoryPerm toUpdate = new BackCategoryPerm();
                toUpdate.setId(permOpt.get().getId());
                toUpdate.setAllow(Strings.nullToEmpty(allow));

                backCategoryPermDao.update(toUpdate);
            }

            coreEventDispatcher.publish(new CategoryPermEvent(Arrays.asList(userId)));
            return Response.ok(Boolean.TRUE);

        } catch (Exception e) {
            log.error("update or create back category allows failed, userId={}, allows={}, cause:{}",
                    userId, allows, Throwables.getStackTraceAsString(e));
            return Response.fail("category.update.fail");
        }
    }

    /**
     * 启用后台类目
     * @param category 后台类目
     */
    private void doEnable(BackCategory category) {
        if (category.getPid() == 0L){
            // 无父级, 直接启用
            backCategoryDao.setStatus(category.getId(), BackCategory.Status.ENABLED.value());
        } else {
            // 有父级, 更新父级hasChildren => true
            categoryManager.enable(category);
        }
    }

    @Override
    public Response<FrontCategory> createFrontCategory(FrontCategory category) {
        Response<FrontCategory> resp = new Response<FrontCategory>();
        try {
            if (category.getPid() == null) {
                category.setPid(0l);
            }
            FrontCategory existed = frontCategoryDao.findByNameAndPid(category.getPid(), category.getName());
            if (existed == null){
                // 不存在, 可直接创建
                doCreateFrontCategory(category);
                resp.setResult(category);
                coreEventDispatcher.publish(new FrontCategoryInsEvent(category.getId()));
            } else {
                // 已存在
                log.warn("front category(name={}) is existed.", existed.getName());
                resp.setError("category.name.existed");
                return resp;
            }
        } catch (Exception e){
            log.error("failed to create front category({}), cause: {}", category, Throwables.getStackTraceAsString(e));
            resp.setError("category.create.fail");
        }
        return resp;
    }

    /**
     * 创建前台类目
     * @param category 前台类目
     */
    private void doCreateFrontCategory(FrontCategory category) {
        category.setHasChildren(Boolean.FALSE);
        if (category.getPid() == null || category.getPid() == 0){
            category.setPid(0L);
            category.setLevel(1);
            // 无父级类目, 直接创建
            checkState(frontCategoryDao.create(category), "front.category.persist.fail");
        } else {
            // 有父级类目, 需要更新父级类目hasChildren
            categoryManager.create(category);
        }
    }

    @Override
    public Response<Boolean> updateFrontCategory(long id, @Nullable String name, @Nullable String logo, @Nullable String background) {
        try {
            if (Strings.isNullOrEmpty(name) && Strings.isNullOrEmpty(logo) && Strings.isNullOrEmpty(background)) {
                return Response.ok(Boolean.FALSE);
            }
            FrontCategory exist = checkFrontCategoryExists(id);
            exist.setName(Strings.emptyToNull(name));
            exist.setLogo(Strings.emptyToNull(logo));
            exist.setBackground(Strings.emptyToNull(background));
            frontCategoryDao.update(exist);
            coreEventDispatcher.publish(new FrontCategoryAltEvent(id));
            return Response.ok(Boolean.TRUE);
        } catch (ServiceException e){
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("update front category failed, id={}, name={}, logo={}, cause:{}",
                    id, name, logo, Throwables.getStackTraceAsString(e));
            return Response.fail("category.update.fail");
        }
    }

    @Override
    public Response<Boolean> deleteFrontCategory(Long id) {
        Response<Boolean> resp = new Response<Boolean>();
        try {
            FrontCategory category = checkFrontCategoryExists(id);
            checkBeforeDelete(category);
            doDelete(category);
            coreEventDispatcher.publish(new FrontCategoryDelEvent(id));
            resp.setResult(Boolean.TRUE);
        } catch (ServiceException e){
            resp.setError(e.getMessage());
        } catch (Exception e){
            log.error("failed to delete front category(id={}), cause: {}",
                    id, Throwables.getStackTraceAsString(e));
            resp.setError("category.delete.fail");
        }
        return resp;
    }

    /**
     * 删除前台类目
     * @param category 前台类目
     */
    private void doDelete(FrontCategory category) {
        if (category.getPid() == 0){
            // 无父级, 直接disable
            frontCategoryDao.delete(category.getId());
        } else {
            categoryManager.delete(category);
        }
    }

    @Override
    public Response<Long> createCategoryBinding(CategoryBinding binding) {
        Response<Long> resp = new Response<Long>();
        try {
            BackCategory backCategory = checkBackCategoryExists(binding.getBackCategoryId());
            if (!BackCategory.isEnable(backCategory)){
                log.warn("back category(id={}) is disabled.");
                resp.setError("category.back.disabled");
                return resp;
            }
            checkFrontCategoryExists(binding.getFrontCategoryId());
            categoryBindingDao.create(binding);
            resp.setResult(binding.getId());
        } catch (Exception e){
            log.error("failed create category binding({}), cause: {}", binding, Throwables.getStackTraceAsString(e));
            resp.setError("category.binding.fail");
        }
        return resp;
    }

    @Override
    public Response<Boolean> deleteCategoryBinding(Long id) {
        Response<Boolean> resp = new Response<Boolean>();
        try {
            categoryBindingDao.delete(id);
            resp.setResult(Boolean.TRUE);
        } catch (Exception e){
            log.error("failed to delete category binding(id={}), cause: {}", id, Throwables.getStackTraceAsString(e));
            resp.setError("category.binding.delete.fail");
        }
        return resp;
    }

    @Override
    public Response<Long> createSpu(Spu spu) {
        Response<Long> resp = new Response<Long>();
        try {
            Spu criteria = new Spu();
            criteria.setCategoryId(spu.getCategoryId());
            criteria.setName(spu.getName());
            // 该类目下是否存在name的SPU
            Spu existed = spuDao.findBy(criteria);
            if (existed == null){
                // 创建SPU并设置后台叶子类目hasSpu=true
                spu.setStatus(Spu.Status.ENABLED.value());
                categoryManager.create(spu);
                coreEventDispatcher.publish(new SpuInsEvent(spu.getCategoryId(), spu.getId()));
            } else {
                if (Spu.isEnable(existed)){
                    log.warn("spu({}) is existed.", existed);
                    resp.setError("spu.is.existed");
                } else {
                    // 直接启用SPU
                    categoryManager.enable(existed);
                    resp.setResult(existed.getId());
                    coreEventDispatcher.publish(new SpuEnableEvent(existed.getCategoryId(), spu.getId()));
                }
                return resp;
            }
            resp.setResult(spu.getId());
        } catch (Exception e){
            log.error("failed to create spu({}), cause: {}", spu, Throwables.getStackTraceAsString(e));
            resp.setError("spu.create.fail");
        }
        return resp;
    }

    private void checkAttributesExist(long categoryId, List<AttributeDto> attrs) throws ServiceException {
        final List<CategoryAttributeKey> keys = categoryAttributeKeyDao.findBy(categoryId);
        for (AttributeDto attr : attrs) {
            long keyId = attr.getAttributeKeyId();
            boolean isExist = false;
            for (CategoryAttributeKey key : keys) {
                if (keyId == key.getKeyId()) {
                    attr.setAttributeKeyName(key.getKeyName());
                    isExist = true;
                    break;
                }
            }
            checkResult(isExist, "category.attribute.not.exist");
            // TODO(Effet): also check value
        }
    }

    @Override
    public Response<Long> createSpu(Spu spu, List<AttributeDto> attrs) {
        try {
            checkResult(spu != null, "spu.null");
            // TODO: regexp check name
            checkResult(spu.getName() != null, "spu.name.blank");
            checkResult(spu.getCategoryId() != null, "category.id.invalid");
            checkResult(spu.getBrandId() != null, "brand.id.invalid");
            Brand brand = brandDao.load(spu.getBrandId());
            checkResult(brand != null, "brand.not.exist");
            spu.setBrandName(brand.getName());

            checkResult(Iters.emptyToNull(attrs) != null, "spu.attributes.is.empty");

            // 检查类目属性是否存在
            checkAttributesExist(spu.getCategoryId(), attrs);

            // save sku key first
            List<AttributeDto> skuAttrs = ImmutableList.copyOf(Iterables.filter(attrs, SKU_PREDICATE));
            if (skuAttrs.size() > 2){
                log.warn("spu's sku({})'s size is > 2", skuAttrs);
                return Response.fail("spu.sku.size.gt2");
            }
            if (skuAttrs.size() == 0) {
                log.warn("spu's skus size is==0");
                return Response.fail("spu.sku.size.0");
            }

            // remove sku keys from attrs
            Iterables.removeIf(attrs, SKU_PREDICATE);

            List<SpuAttribute> spuAttrs = Lists.newArrayListWithCapacity(attrs.size());
            // save not-enumerate-attributes

            Iterable<AttributeDto> nonEnumerable = Iterables.filter(attrs, NON_ENUMERALE_PREDICATE);
            for (AttributeDto attributeDto : nonEnumerable) {
                SpuAttribute spuAttribute = toSpuNonEnumAttr(null, attributeDto);
                spuAttrs.add(spuAttribute);
            }

            Iterables.removeIf(attrs, NON_ENUMERALE_PREDICATE);
            // for enumerable attributes
            for (AttributeDto attribute : attrs) {
                SpuAttribute spuAttribute = toSpuEnumAttr(null, attribute);
                spuAttrs.add(spuAttribute);
            }

            long spuId = categoryManager.create(spu, skuAttrs, spuAttrs);
            coreEventDispatcher.publish(new SpuInsEvent(spu.getCategoryId(), spuId));
            return Response.ok(spuId);
        } catch (ServiceException e) {
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("create spu failed, spu={}, attrs={}, cause:{}",
                    spu, attrs, Throwables.getStackTraceAsString(e));
            return Response.fail("spu.create.fail");
        }
    }

    @Override
    public Response<Long> createRichSpu(RichSpu richSpu) {
        if (richSpu == null) {
            return Response.fail("spu.null");
        }
        return createSpu(richSpu.getSpu(), richSpu.getAttributes());
    }

    @Override
    public Response<Boolean> updateSpu(Spu spu) {
        Response<Boolean> resp = new Response<Boolean>();
        try {
            Spu exist = spuDao.load(spu.getId());
            if (exist == null) {
                log.warn("spu(id={}) not exsit", spu.getId());
                return Response.fail("spu.not.exist");
            }
            spuDao.update(spu);
            coreEventDispatcher.publish(new SpuAltEvent(exist.getCategoryId(), spu.getId()));
            resp.setResult(Boolean.TRUE);
        } catch (Exception e){
            log.error("failed to update spu({}), cuase: {}", spu, Throwables.getStackTraceAsString(e));
            resp.setError("spu.update.fail");
        }
        return resp;
    }

    @Override
    public Response<Boolean> updateRichSpu(RichSpu richSpu) {
        Response<Boolean> resp = new Response<Boolean>();
        try {
            Spu spu = richSpu.getSpu();
            // TODO(Effet): updateSpu 里已经发送更新事件
            Response<Boolean> updateSpuResp = updateSpu(spu);
            if (!updateSpuResp.isSuccess()){
                resp.setError(updateSpuResp.getError());
                return resp;
            }
            // 更新SPU属性
            List<AttributeDto> attributes = richSpu.getAttributes();
            if (attributes != null && !Iterables.isEmpty(attributes)) {
                Response<Boolean> createSpuAttrsResp = createSpuAttributes(spu.getId(), attributes);
                if (!createSpuAttrsResp.isSuccess()){
                    resp.setError(createSpuAttrsResp.getError());
                    return resp;
                }
            }
            resp.setResult(Boolean.TRUE);
        } catch (Exception e){
            log.error("failed to create rich spu({}), cause: {}", richSpu, Throwables.getStackTraceAsString(e));
            resp.setError("spu.create.fail");
        }
        return resp;
    }

    @Override
    public Response<Boolean> disableSpu(Long id) {
        Response<Boolean> resp = new Response<Boolean>();
        try {
            Spu spu = spuDao.load(id);
            if (spu == null){
                log.warn("spu(id={}) isn't exist.", id);
                resp.setError("spu.not.exist");
                return resp;
            }
            if (Spu.isEnable(spu)){
                categoryManager.disable(spu);
                coreEventDispatcher.publish(new SpuDisableEvent(spu.getCategoryId(), id));
            }
            resp.setResult(Boolean.TRUE);
        } catch (Exception e){
            log.error("failed to delete spu(id={}), cause: {}", id, Throwables.getStackTraceAsString(e));
            resp.setError("spu.disable.fail");
        }
        return resp;
    }

    @Override
    public Response<Boolean> enableSpu(Long id) {
        Response<Boolean> resp = new Response<Boolean>();
        try {
            Spu spu = spuDao.load(id);
            if (spu == null){
                log.warn("spu(id={}) isn't existed", id);
                resp.setError("spu.not.exist");
                return resp;
            }
            if (!Spu.isEnable(spu)){
                categoryManager.enable(spu);
                coreEventDispatcher.publish(new SpuEnableEvent(spu.getCategoryId(), id));
            }
            resp.setResult(Boolean.TRUE);
        } catch (Exception e){
            log.error("failed to enable spu(id={}), cause: {}", id, Throwables.getStackTraceAsString(e));
            resp.setError("spu.enable.fail");
        }
        return resp;
    }

    private AttributeKey buildOldTimeKey(CategoryAttributeKey key) {
        AttributeKey attrKey = new AttributeKey();
        attrKey.setId(key.getKeyId());
        attrKey.setName(key.getKeyName());
        attrKey.setValueType(key.getKeyType());
        return attrKey;
    }

    private AttributeValue buildOldTimeValue(CategoryAttributeValue value) {
        AttributeValue attrValue = new AttributeValue();
        attrValue.setId(value.getValueId());
        attrValue.setValue(value.getValue());
        attrValue.setLogo(value.getLogo());
        return attrValue;
    }

    @Override
    public Response<AttributeKey> createCategoryAttributeKey(long categoryId, String keyName, int keyType) {
        try {
            keyName = Params.trimToNull(keyName);
            // TODO(Effet): key name check
            if (keyName == null || !CategoryAttributeKey.KeyType.from(keyType).isPresent()) {
                return Response.fail("category.attribute.key.create.fail");
            }
            // check keyType
            AttributeKey existed = attributeKeyDao.findByName(keyName);
            if (existed == null) {
                //if attributeKey not exists, create it first
                AttributeKey toCreate = new AttributeKey();
                toCreate.setName(keyName);
                toCreate.setValueType(0); // fallback
                Long attributeKeyId = attributeKeyDao.create(toCreate);
                existed = attributeKeyDao.findById(attributeKeyId);
            }
            long keyId = existed.getId();
            CategoryAttributeKey key = new CategoryAttributeKey();
            key.setCategoryId(categoryId);
            key.setKeyId(keyId);
            key.setKeyName(keyName);
            key.setKeyType(keyType);
            categoryAttributeKeyDao.create(key);
            return Response.ok(buildOldTimeKey(key));
        } catch (Exception e) {
            log.error("create category attribute key failed, categoryId={}, keyName={}, keyType={}, cause:{}",
                    categoryId, keyName, keyType, Throwables.getStackTraceAsString(e));
            return Response.fail("category.attribute.key.create.fail");
        }
    }

    @Override
    public Response<AttributeValue> createCategoryAttributeValue(long categoryId, long keyId, String value, @Nullable String logo) {
        try {
            value = Params.trimToNull(value);
            if (value == null) {
                return Response.fail("category.attribute.value.create.fail");
            }
            AttributeValue existed = attributeValueDao.findByValue(value.trim());
            if (existed == null) {
                //if attribute value not exists, create it first
                existed = attributeValueDao.create(value.trim());
            }
            long valueId = existed.getId();
            logo = Params.trimToNull(logo);
            CategoryAttributeValue attrValue = new CategoryAttributeValue();
            attrValue.setCategoryId(categoryId);
            attrValue.setKeyId(keyId);
            attrValue.setValueId(valueId);
            attrValue.setValue(value);
            attrValue.setLogo(logo);
            categoryAttributeValueDao.create(attrValue);
            return Response.ok(buildOldTimeValue(attrValue));
        } catch (Exception e) {
            log.error("create category attribute value failed, categoryId={}, keyId={}, value={}, logo={}, cause:{}",
                    categoryId, keyId, value, logo, Throwables.getStackTraceAsString(e));
            return Response.fail("category.attribute.value.create.fail");
        }
    }

    @Override
    public Response<Boolean> deleteCategoryAttributeKey(long categoryId, long keyId) {
        try {
            Optional<CategoryAttributeKey> keyOpt = categoryAttributeKeyDao.findBy(categoryId, keyId);
            if (!keyOpt.isPresent()) {
                return Response.ok(Boolean.FALSE);
            }
            List<CategoryAttributeValue> values = categoryAttributeValueDao.findBy(categoryId, keyId);
            if (values.isEmpty()) {
                return Response.ok(categoryAttributeKeyDao.deleteById(keyOpt.get().getId()));
            } else {
                return Response.fail("category.attribute.key.delete.fail.has.values");
            }
        } catch (Exception e) {
            log.error("delete category attribute key failed, categoryId={}, keyId={}, cause:{}",
                    categoryId, keyId, Throwables.getStackTraceAsString(e));
            return Response.fail("category.attribute.key.delete.fail");
        }
    }

    @Override
    public Response<Boolean> deleteCategoryAttributeValue(long categoryId, long keyId, long valueId) {
        try {
            Optional<CategoryAttributeValue> valueOpt = categoryAttributeValueDao.findBy(categoryId, keyId, valueId);
            if (valueOpt.isPresent()) {
                return Response.ok(categoryAttributeValueDao.deleteById(valueOpt.get().getId()));
            } else {
                return Response.ok(Boolean.FALSE);
            }
        } catch (Exception e) {
            log.error("delete category attribute value failed, categoryId={}, keyId={}, valueId={}, cause:{}",
                    categoryId, keyId, valueId, Throwables.getStackTraceAsString(e));
            return Response.fail("category.attribute.value.delete.fail");
        }
    }

    @Override
    public Response<Boolean> createSpuAttributes(Long spuId, List<AttributeDto> attrs) {
        Response<Boolean> resp = new Response<Boolean>();
        try {
            Spu spu = spuDao.load(spuId);
            if (spu == null) {
                log.warn("spu(id={}) not exist", spuId);
                return Response.fail("spu.not.exist");
            }
            // save sku key first
            Iterable<AttributeDto> skus = Iterables.filter(attrs, SKU_PREDICATE);
            if (Iterables.size(skus) > 2){
                log.warn("spu(id={})'s sku({})'s size is > 2", spuId, skus);
                return Response.fail("spu.sku.size.gt2");
            }
            if (Iterables.size(skus) == 0) {
                log.warn("spu(id={})'s skus size is==0", spuId);
                return Response.fail("spu.sku.size.0");
            }
            attributeIndexDao.addSkuKeys(spuId, skus);

            // remove sku keys from attrs
            Iterables.removeIf(attrs, SKU_PREDICATE);

            List<SpuAttribute> spuAttributes = Lists.newArrayListWithCapacity(attrs.size());
            // save not-enumerate-attributes

            Iterable<AttributeDto> nonEnumerable = Iterables.filter(attrs, NON_ENUMERALE_PREDICATE);
            for (AttributeDto attributeDto : nonEnumerable) {
                SpuAttribute spuAttribute = toSpuNonEnumAttr(spuId, attributeDto);
                spuAttributes.add(spuAttribute);
            }

            Iterables.removeIf(attrs, NON_ENUMERALE_PREDICATE);
            // for enumerable attributes
            for (AttributeDto attribute : attrs) {
                SpuAttribute spuAttribute = toSpuEnumAttr(spuId, attribute);
                spuAttributes.add(spuAttribute);
            }
            // save attrs
            spuAttributeDao.create(spuId, spuAttributes);
            coreEventDispatcher.publish(new SpuAltEvent(spu.getCategoryId(), spuId));
            resp.setResult(Boolean.TRUE);
        } catch (Exception e){
            log.error("failed to create spu(id={})'s attributes({}), cause: {}",
                    spuId, attrs, Throwables.getStackTraceAsString(e));
            resp.setError("spu.attribute.create.fail");
        }
        return resp;
    }

    /**
     * 将AttributeDto转换为SpuAttribute(枚举属性)
     * @param spuId SPU.id
     * @param attributeDto AttributeDto
     * @return SpuAttribute
     */
    private SpuAttribute toSpuEnumAttr(Long spuId, AttributeDto attributeDto) {
        SpuAttribute spuAttribute = new SpuAttribute();
        spuAttribute.setAttributeKeyId(attributeDto.getAttributeKeyId());
        spuAttribute.setAttributeValueId(Long.parseLong(attributeDto.getValue()));
        spuAttribute.setSpuId(spuId);
        return spuAttribute;
    }

    /**
     * 将AttributeDto转换为SpuAttribute(非枚举属性)
     * @param spuId SPU.id
     * @param attributeDto AttributeDto
     * @return SpuAttribute
     */
    private SpuAttribute toSpuNonEnumAttr(Long spuId, AttributeDto attributeDto) {
        String value = attributeDto.getValue();
        AttributeValue attributeValue = attributeValueDao.findByValue(value);
        if (attributeValue == null) {
            attributeValue = attributeValueDao.create(value);
        }
        SpuAttribute spuAttribute = new SpuAttribute();
        spuAttribute.setAttributeKeyId(attributeDto.getAttributeKeyId());
        spuAttribute.setAttributeValueId(attributeValue.getId());
        spuAttribute.setSpuId(spuId);
        return spuAttribute;
    }

    @Override
    public Response<Boolean> deleteSpuAttribute(Long attributeId) {
        Response<Boolean> resp = new Response<Boolean>();
        try {
            SpuAttribute spuAttribute = spuAttributeDao.findById(attributeId);
            if (spuAttribute == null){
                log.warn("can't find spu attribute(id={})", attributeId);
                resp.setError("spu.attribute.not.exist");
                return resp;
            }
            Spu spu = spuDao.load(spuAttribute.getSpuId());
            if (spu == null) {
                log.warn("spu(id={}) not exist", spuAttribute.getSpuId());
                return Response.fail("spu.not.exist");
            }
            coreEventDispatcher.publish(new SpuAltEvent(spu.getCategoryId(), spu.getId()));
            resp.setResult(Boolean.TRUE);
        } catch (Exception e){
            log.error("failed to delete spu attribute(attributeId={}), cause: {}", attributeId, Throwables.getStackTraceAsString(e));
            resp.setError("spu.attribute.delete.fail");
        }
        return resp;
    }

    @Override
    public Response<Boolean> bindBusinessToCategories(long businessId, Collection<Long> categoryIds) {
        try {
            List<Long> ids = FluentIterable.from(Iters.nullToEmpty(categoryIds)).filter(Predicates.notNull()).toSet().asList();
            if (ids.isEmpty()) {
                return Response.ok(Boolean.FALSE);
            }
            return Response.ok(backCategoryDao.setBusiness(businessId, ids) > 0);
        } catch (Exception e) {
            log.error("bind business(id={}) to categories(ids={}) failed, cause:{}",
                    businessId, categoryIds, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.business.bind.fail");
        }
    }

    @Override
    public Response<List<ImportResult<CatImportData>>> importCategories(List<CatImportData> data) {
        try {
            List<ImportResult<CatImportData>> results = new ArrayList<>();
            for (CatImportData catImportData : data) {
                results.add(singleImportCat(catImportData));
            }
            return Response.ok(results);
        } catch (Exception e) {
            log.error("import category failed, data(count={}), cause:{}",
                    safeSize(data), Throwables.getStackTraceAsString(e));
            return Response.fail("category.import.fail");
        }
    }

    @Override
    public Response<List<ImportResult<CatImportData>>> importFontCategories(List<CatImportData> catImportDatas) {
        try {
            List<ImportResult<CatImportData>> results = Lists.newArrayList();
            for (CatImportData catImportData : catImportDatas) {
                results.add(singleImportFontCat(catImportData));
            }
            return Response.ok(results);
        } catch (Exception e) {
            log.error("import font category failed,cateImportDatas(count={}),cause:{}",
                    safeSize(catImportDatas), Throwables.getStackTraceAsString(e));
            return Response.fail("category.import.fail");
        }
    }

    private ImportResult<CatImportData> singleImportCat(CatImportData data) {
        try {
            if (data.getCategories() == null) {
                return ImportResult.fail(data, "category.path.invalid");
            }
            for (String s : data.getCategories()) {
                if (Strings.isNullOrEmpty(s)) {
                    return ImportResult.fail(data, "category.path.invalid");
                }
            }
            categoryManager.createByPath(data.getCategories());
            return ImportResult.ok(data);
        } catch (ServiceException e) {
            log.warn("single import cat failed, error:{}", e.getMessage());
            return ImportResult.fail(data, e.getMessage());
        } catch (Exception e) {
            log.warn("single import cat failed, cause:{}", Throwables.getStackTraceAsString(e));
            return ImportResult.fail(data, "category.create.fail");
        }
    }
    private ImportResult<CatImportData> singleImportFontCat(CatImportData catImportData){
        try {
            if (catImportData.getFontCategories() == null) {
                return ImportResult.fail(catImportData,"category.path.invalid");
            }
            for (String str : catImportData.getFontCategories()){
                if (Strings.isNullOrEmpty(str)){
                    return ImportResult.fail(catImportData,"category.path.invalid");
                }
            }
            if (catImportData.getCategories() == null){
                return ImportResult.fail(catImportData,"category.is.null");
            }
            for (String str : catImportData.getCategories()){
                if (Strings.isNullOrEmpty(str)){
                    return ImportResult.fail(catImportData,"category.is.null");
                }
            }
            categoryManager.createFontCatByPath(catImportData.getFontCategories(),catImportData.getCategories()) ;
            return ImportResult.ok(catImportData);
        } catch (ServiceException e){
            log.warn("single import font cat failed,error:{}",e.getMessage());
            return  ImportResult.fail(catImportData,e.getMessage());
        } catch (Exception e){
            log.warn("single import font cat failed,error:{}",Throwables.getStackTraceAsString(e));
            return ImportResult.fail(catImportData,"category.create.fail");
        }
    }

    private int safeSize(@Nullable List<?> list) {
        return list == null ? 0 : list.size();
    }

    @Override
    public Response<List<ImportResult<SpuImportData>>> importSpu(long userId, List<SpuImportData> data) {
        try {
            List<ImportResult<SpuImportData>> results = Lists.newArrayList();
            for (SpuImportData cat : Iters.nullToEmpty(data)) {
                results.add(singleSafeImport(userId, cat));
            }
            return Response.ok(results);
        } catch (Exception e) {
            log.error("import spu failed, data(count={}), cause:{}",
                    safeSize(data), Throwables.getStackTraceAsString(e));
            return Response.fail("spu.import.fail");
        }
    }

    private ImportResult<SpuImportData> singleSafeImport(long userId, SpuImportData row) {
        try {
            long categoryId = RespHelper.orServEx(tryToGetLeafCat(userId, row.getCategories()));
            long brandId = RespHelper.orServEx(tryToGetBrand(userId, row.getBrand()));
            RespHelper.orServEx(tryToCreateSpu(categoryId, brandId, row.getSpu(), row.getAttributes()));
            return ImportResult.ok(row);
        } catch (ServiceException e) {
            return ImportResult.fail(row, e.getMessage());
        } catch (Exception e) {
            return ImportResult.fail(row, "category.create.fail");
        }
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

    private Response<Long> tryToGetBrand(long userId, String brand) {
        Response<Brand> brandResp = brandReadService.getBrandByNameWithPerm(userId, brand);
        if (!brandResp.isSuccess()) {
            return Response.fail(brandResp.getError());
        }
        return Response.ok(brandResp.getResult().getId());
    }

    private Response<Long> tryToCreateSpu(long categoryId, long brandId, String spuName, SpuImportData.Attr[] attributes) {
        try {
            Spu spu = new Spu();
            spu.setCategoryId(categoryId);
            spu.setName(spuName);

            // TODO(Effet): 这个查询只查 status = 1
            Spu existSpu = spuDao.findBy(spu);
            boolean spuExist = false;
            if (existSpu != null) {
                spuExist = true;
                spu = existSpu;
            }

            spu.setBrandId((int) brandId);
            spu.setCategoryId(categoryId);

            List<AttributeDto> attrs = Lists.newArrayList();
            for (SpuImportData.Attr attribute : attributes) {
                String keyName = Params.trimToNull(attribute.getKeyName());
                if (keyName == null) {
                    return Response.fail("attribute.key.name.blank");
                }
                boolean isSku = "S".equalsIgnoreCase(attribute.getKeyType());
                boolean isEnum = isSku || "E".equalsIgnoreCase(attribute.getKeyType());
                String[] values = attribute.getValues();
                if (Iters.emptyToNull(values) == null) {
                    return Response.fail("category.attribute.value.blank");
                }

                AttributeDto attr = new AttributeDto();
                attr.setIsSku(isSku);
                attr.setAttributeKeyName(keyName);
                attr.setType(isEnum ? CategoryAttributeKey.KeyType.ENUM.value() : CategoryAttributeKey.KeyType.NOT_ENUM.value());
                AttributeKey key = attributeKeyDao.findByName(keyName);
                if (key == null) {
                    key = new AttributeKey();
                    key.setName(keyName);
                    Long id = attributeKeyDao.create(key);
                    key.setId(id);
                }
                attr.setAttributeKeyId(key.getId());
                if (!isSku) {
                    String enumValue = Params.trimToNull(values[0]);
                    if (enumValue == null) return Response.fail("category.attribute.value.blank");
                    AttributeValue v = attributeValueDao.findByValue(enumValue);
                    if (v == null) v = attributeValueDao.create(enumValue);

                    if (isEnum) {
                        attr.setValue(v.getId().toString());
                    } else {
                        attr.setValue(v.getValue());
                    }
                }
                attrs.add(attr);
                CategoryAttributeKey catKey = categoryAttributeKeyDao.findBy(categoryId, key.getId()).orNull();
                if (catKey == null) {
                    catKey = new CategoryAttributeKey();
                    catKey.setCategoryId(categoryId);
                    catKey.setKeyId(key.getId());
                    catKey.setKeyType(isEnum ? CategoryAttributeKey.KeyType.ENUM.value() : CategoryAttributeKey.KeyType.NOT_ENUM.value());
                    catKey.setKeyName(key.getName());
                    categoryAttributeKeyDao.create(catKey);
                }
                if (Objects.equal(catKey.getKeyType(), CategoryAttributeKey.KeyType.NOT_ENUM.value())) {
                    if (isEnum) {
                        return Response.fail("category.attribute.key.type.illegal");
                    }
                } else {
                    List<CategoryAttributeValue> catValues = categoryAttributeValueDao.findBy(categoryId, catKey.getKeyId());
                    for (String value : values) {
                        AttributeValue rawValue = attributeValueDao.findByValue(value);
                        if (rawValue == null) {
                            rawValue = attributeValueDao.create(value);
                        }

                        boolean exist = false;
                        for (CategoryAttributeValue catValue : catValues) {
                            if (Objects.equal(value, catValue.getValue())) {
                                exist = true;
                                break;
                            }
                        }
                        if (!exist) {
                            CategoryAttributeValue toCreate = new CategoryAttributeValue();
                            toCreate.setCategoryId(categoryId);
                            toCreate.setKeyId(key.getId());
                            toCreate.setValueId(rawValue.getId());
                            toCreate.setValue(value);
                            categoryAttributeValueDao.create(toCreate);
                            catValues.add(toCreate);
                        }
                    }
                }
            }

            if (spuExist) {
//                List<AttributeKey> attrKeys = RespHelper.orServEx(categoryReadService.findSkuKeys(spu.getId()));
//                Set<Long> exists = new HashSet<>();
//                for (AttributeDto attr : attrs) if (attr.getIsSku()) exists.add(attr.getAttributeKeyId());
//                Set<Long> anoExists = new HashSet<>();
//                for (AttributeKey attrKey : attrKeys) anoExists.add(attrKey.getId());
//                checkResult(Sets.symmetricDifference(exists, anoExists).isEmpty(), "spu.update.fail.sku.invalid");
                for (AttributeDto attr : attrs) {
                    if (attr.getIsSku()) {
                        throw new ServiceException("spu.update.fail.sku.cant.update");
                    }
                }
                List<AttributeKey> attrKeys = RespHelper.orServEx(categoryReadService.findSkuKeys(spu.getId()));
                for (AttributeKey attrKey : attrKeys) {
                    AttributeDto attributeDto = new AttributeDto();
                    attributeDto.setIsSku(true);
                    attributeDto.setType(CategoryAttributeKey.KeyType.ENUM.value());
                    attributeDto.setAttributeKeyId(attrKey.getId());
                    attributeDto.setAttributeKeyName(attrKey.getName());
                    attrs.add(attributeDto);
                }
                RichSpu richSpu = new RichSpu();
                richSpu.setSpu(spu);
                richSpu.setAttributes(attrs);
                RespHelper.orServEx(updateRichSpu(richSpu));
                return Response.ok(spu.getId());
            } else {
                return createSpu(spu, attrs);
            }
        } catch (ServiceException e) {
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            return Response.fail("spu.create.fail");
        }
    }

    /**
     * notify backend category cache clear
     */
    @Override
    public void clearBackend() {
        if (cachePublisher != null){
            try {
                cachePublisher.publish(CacheMessage.toBytes(CacheMessage.SYNC_BACKEND));
            } catch (Exception e) {
                log.error("failed to clear backend categories, cause: {}", Throwables.getStackTraceAsString(e));
            }
        } else {
            categoryCacher.clearBackend();
        }
    }

    /**
     * notify frontend category cache clear
     */
    @Override
    public void clearFrontend() {
        if (cachePublisher != null){
            try {
                cachePublisher.publish(CacheMessage.toBytes(CacheMessage.SYNC_FRONTEND));
            } catch (Exception e) {
                log.error("failed to clear frontend categories, cause: {}", Throwables.getStackTraceAsString(e));
            }
        } else {
            categoryCacher.clearFrontend();
        }
    }

    @Override
    public Response<Boolean> setRate(Long id, Integer rate) {

        Response<Boolean> result = new Response<Boolean>();
        try {
            BackCategory exit = backCategoryDao.load(id);
            checkState(Arguments.notNull(exit),"category.not.exist");
            backCategoryDao.setRate(rate,id);
            result.setResult(Boolean.TRUE);
        }catch (IllegalStateException e){
            log.error("set back category rate fail,error:{}",e.getMessage());
            result.setError(e.getMessage());
        }catch (Exception e){
            log.error("set back category rate fail,cause:{}",Throwables.getStackTraceAsString(e));
            result.setError("category.update.fail");
        }
        return result;
    }


    /**
     * 检查后台类目状态
     * @param id 后台类目id
     */
    private BackCategory checkBackCategoryExists(Long id){
        BackCategory category = backCategoryDao.load(id);
        if (category == null){
            log.warn("back category(id={}) isn't existed.");
            throw new ServiceException("category.not.exist");
        }
        return category;
    }

    /**
     * 检查前台类目是否存在
     * @param id 前台类目id
     */
    private FrontCategory checkFrontCategoryExists(Long id){
        FrontCategory category = frontCategoryDao.load(id);
        if (category == null){
            log.warn("front category(id={}) isn't existed.");
            throw new ServiceException("category.not.exist");
        }
        return category;
    }

    /**
     * 禁用后台类目条件:
     *  1. 类目无子类目
     *  2. 没有SPU挂载
     * @param category 类目
     */
    private void checkBeforeDisable(BackCategory category) {
        if (category.getHasChildren()){
            log.warn("category() has child categories, can't be disabled", category);
            throw new ServiceException("category.disable.has.children");
        }
        if (category.getHasSpu()){
            log.warn("category() has spu, can't be disabled", category);
            throw new ServiceException("category.disable.has.spu");
        }
    }

    /**
     * 删除前台类目条件:
     *  1. 类目无子类目
     * @param category 类目
     */
    private void checkBeforeDelete(FrontCategory category) {
        if (category.getHasChildren()){
            log.warn("category() has child categories, can't be disabled", category);
            throw new ServiceException("category.disable.has.children");
        }
        if (!categoryBindingDao.findByFrontCategoryId(category.getId()).isEmpty()) {
            log.warn("category={} has binding, can't be disabled", category);
            throw new ServiceException("category.disable.has.binding");
        }
    }
}
