/*
 * Copyright (c) 2015 杭州端点网络科技有限公司
 */

package io.terminus.parana.category.service;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import io.terminus.pampas.common.Response;
import io.terminus.parana.category.dao.mysql.BackCategoryDao;
import io.terminus.parana.category.dao.mysql.BackCategoryPermDao;
import io.terminus.parana.category.dao.mysql.BrandSubsetDao;
import io.terminus.parana.category.manager.BrandManager;
import io.terminus.parana.category.model.BackCategory;
import io.terminus.parana.category.model.BackCategoryPerm;
import io.terminus.parana.category.model.BrandSubset;
import io.terminus.parana.common.event.CoreEventDispatcher;
import io.terminus.parana.common.util.Params;
import io.terminus.parana.event.brand.BrandAltEvent;
import io.terminus.parana.event.brand.BrandInsEvent;
import io.terminus.parana.event.misc.CategoryPermEvent;
import io.terminus.parana.item.dao.mysql.BrandDao;
import io.terminus.parana.item.model.Brand;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;

/**
 * @author Effet
 */
@Slf4j
@Service
public class BrandWriteServiceImpl implements BrandWriteService {

    @Autowired
    private BrandDao brandDao;

    @Autowired
    private BrandSubsetDao brandSubsetDao;

    @Autowired
    private BrandManager brandManager;

    @Autowired
    private BackCategoryDao backCategoryDao;

    @Autowired
    private BackCategoryPermDao backCategoryPermDao;

    @Autowired
    private UserReadService userReadService;

    @Autowired
    private CoreEventDispatcher coreEventDispatcher;

    @Override
    public Response<Long> createBrand(@Valid Brand brand) {
        try {
            Brand existed = brandDao.findByName(brand.getName());
            if (existed != null){
                log.warn("brand name({}) is existed.", brand.getName());
                return Response.fail("brand.name.existed");
            }
            brandDao.create(brand);
            coreEventDispatcher.publish(new BrandInsEvent(brand.getId()));
            return Response.ok(brand.getId());
        } catch (Exception e) {
            log.error("failed to create brand({}), cause: {}", brand, Throwables.getStackTraceAsString(e));
            return Response.fail("brand.create.fail");
        }
    }

    @Override
    public Response<Boolean> updateBrand(long id, Brand brand) {
        if (brand == null) {
            log.warn("brand null to update, id={}, no need update", id);
            return Response.ok(Boolean.FALSE);
        }
        try {
            Brand old = brandDao.load(id);
            if (old == null){
                log.warn("brand(id={}) isn't existed.", id);
                return Response.fail("brand.not.exist");
            }
            old.setName(Params.trimToNull(brand.getName()));
            old.setEnName(Params.trimToNull(brand.getEnName()));
            old.setEnCap(Params.trimToNull(brand.getEnCap()));
            old.setLogo(Params.trimToNull(brand.getLogo()));
            old.setDescription(Params.trimToNull(brand.getDescription()));
            brandDao.update(old);
            coreEventDispatcher.publish(new BrandAltEvent(id));
            return Response.ok(true);
        } catch (Exception e) {
            log.error("failed to update brand(id={}, data={}), cause: {}",
                    id, brand, Throwables.getStackTraceAsString(e));
            return Response.fail("brand.update.fail");
        }
    }

    @Override
    public Response<Boolean> updateBrandsBinding(final long bcId, @Nullable List<Long> brandIds) {
        // ensure brandIds != null
        brandIds = MoreObjects.firstNonNull(brandIds, ImmutableList.<Long>of());
        try {
            BackCategory bc = backCategoryDao.load(bcId);
            if (bc == null) {
                log.warn("back category(id={}) not found");
                return Response.fail("category.not.exist");
            }
            List<BrandSubset> subsets = brandSubsetDao.findByBcId(bcId);
            // {brandId => id}
            Map<Long, Long> cache = new HashMap<>();
            for (BrandSubset subset : subsets) {
                long brandId = subset.getBrandId();
                checkState(cache.get(brandId) == null, "brandId must single");
                cache.put(brandId, checkNotNull(subset.getId(), "subset id null"));
            }

            Set<Long> existBrandIds = cache.keySet();

            final Set<Long> toCreateBrandIds = FluentIterable.from(brandIds).filter(not(in(existBrandIds))).toSet();
            final Collection<Long> toDeleteIds = Maps.filterKeys(cache, not(in(brandIds))).values();

            brandManager.brandSubsetIncrDecr(bcId, toCreateBrandIds, ImmutableSet.copyOf(toDeleteIds));

            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("update brands binding failed, bcId={}, brandIds={}, cause:{}",
                    bcId, brandIds, Throwables.getStackTraceAsString(e));
            return Response.fail("brand.binding.fail");
        }
    }

    @Override
    public Response<Boolean> updateOrCreateBrandPerm(long userId, @Nullable String perm) {
        try {
            checkArgument(userId > 0, "no user selected for update brand perm");

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

            Optional<BackCategoryPerm> permOpt = backCategoryPermDao.findOne(userId, user.getType());

            if (!permOpt.isPresent()) {
                // 查询所有此类用户默认权限
                permOpt = backCategoryPermDao.findOne(-1l, user.getType());
                checkState(permOpt.isPresent(), "user id=%s, type=%s haven't default perm data",
                        userId, user.getType());

                BackCategoryPerm toCreate = new BackCategoryPerm();
                toCreate.setUserId(userId);
                toCreate.setUserType(user.getType());
                toCreate.setAllow(permOpt.get().getAllow());
                toCreate.setDeny(permOpt.get().getDeny());
                toCreate.setBrandAllow(MoreObjects.firstNonNull(perm, permOpt.get().getBrandAllow()));

                backCategoryPermDao.create(toCreate);
            } else {
                BackCategoryPerm toUpdate = new BackCategoryPerm();
                toUpdate.setId(permOpt.get().getId());
                toUpdate.setBrandAllow(Strings.nullToEmpty(perm));

                backCategoryPermDao.update(toUpdate);
            }

            coreEventDispatcher.publish(new CategoryPermEvent(Arrays.asList(userId)));
            return Response.ok(Boolean.TRUE);

        } catch (Exception e) {
            log.error("update or create brand perm failed, userId={}, perm={}, cause:{}",
                    userId, perm, Throwables.getStackTraceAsString(e));
            return Response.fail("brand.update.fail");
        }
    }
}
