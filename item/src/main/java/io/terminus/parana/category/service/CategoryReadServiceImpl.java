/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package io.terminus.parana.category.service;

import com.google.common.base.*;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.primitives.Ints;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.utils.Splitters;
import io.terminus.pampas.common.BaseUser;
import io.terminus.pampas.common.Response;
import io.terminus.parana.category.cache.BaseCacher;
import io.terminus.parana.category.dao.mysql.BackCategoryDao;
import io.terminus.parana.category.dao.mysql.BackCategoryPermDao;
import io.terminus.parana.category.dao.mysql.CategoryAttributeKeyDao;
import io.terminus.parana.category.dao.mysql.CategoryAttributeValueDao;
import io.terminus.parana.category.dao.mysql.CategoryBindingDao;
import io.terminus.parana.category.dao.mysql.FrontCategoryDao;
import io.terminus.parana.category.dao.mysql.SpuDao;
import io.terminus.parana.category.dao.redis.AttributeKeyDao;
import io.terminus.parana.category.dao.redis.AttributeValueDao;
import io.terminus.parana.category.dto.*;
import io.terminus.parana.category.internal.BackendPerms;
import io.terminus.parana.category.internal.CategoryQueries;
import io.terminus.parana.category.model.AttributeKey;
import io.terminus.parana.category.model.AttributeValue;
import io.terminus.parana.category.model.BackCategory;
import io.terminus.parana.category.model.BackCategoryPerm;
import io.terminus.parana.category.model.CategoryAttributeKey;
import io.terminus.parana.category.model.CategoryAttributeValue;
import io.terminus.parana.category.model.CategoryBinding;
import io.terminus.parana.category.model.FrontCategory;
import io.terminus.parana.category.model.Spu;
import io.terminus.parana.common.util.DayRange;
import io.terminus.parana.common.util.Fns;
import io.terminus.parana.common.util.Iters;
import io.terminus.parana.common.util.Params;
import io.terminus.parana.common.util.RespHelper;
import io.terminus.parana.common.util.ServiceUtils;
import io.terminus.parana.item.dao.mysql.BrandDao;
import io.terminus.parana.item.dao.mysql.ItemDao;
import io.terminus.parana.item.model.Brand;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.shop.model.ShopBusiness;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.user.dto.EcpLoginUser;
import io.terminus.parana.user.dto.LoginUser;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.base.Predicates.or;
import static io.terminus.parana.common.util.ServiceUtils.checkResult;
import static io.terminus.parana.common.util.ServiceUtils.unwrap;

/**
 * 类目读服务实现
 * Author: haolin
 * On: 8/29/14
 */
@Service @Slf4j
public class CategoryReadServiceImpl implements CategoryReadService {

    @Autowired
    private BackCategoryDao backCategoryDao;

    @Autowired
    private FrontCategoryDao frontCategoryDao;

    @Autowired
    private CategoryBindingDao categoryBindingDao;

    @Autowired
    private SpuDao spuDao;

    @Autowired
    private AttributeKeyDao attributeKeyDao;

    @Autowired
    private AttributeValueDao attributeValueDao;

    @Autowired
    private CategoryAttributeKeyDao categoryAttributeKeyDao;

    @Autowired
    private CategoryAttributeValueDao categoryAttributeValueDao;

    @Autowired
    private BaseCacher categoryCacher;

    @Autowired
    private CategoryQueries categoryQueries;

    @Autowired
    private BackendPerms backendPerms;

    @Autowired
    private BackCategoryPermDao backCategoryPermDao;

    @Autowired
    private UserReadService userReadService;

    @Autowired
    private ItemDao itemDao;

    @Autowired
    private BrandDao brandDao;

    @Autowired
    private ShopReadService shopReadService;


    @Override
    public Response<BackCategory> findBackCategoryById(long id) {
        try {
            BackCategory b = backCategoryDao.load(id);
            if (b == null) {
                log.warn("back category(id={}) isn't existed.");
                return Response.fail("category.not.exist");
            }
            return Response.ok(b);
        } catch (Exception e) {
            log.error("find back category by id={} failed, cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("category.find.fail");
        }
    }

    @Override
    public Response<BackCategory> findBackCategoryByIdWithCache(long id) {
        try {
            BackCategory b = categoryCacher.findBackCategoryById(id);
            if (b == null) {
                log.warn("back category(id={}) isn't existed.");
                return Response.fail("category.not.exist");
            }
            return Response.ok(b);
        } catch (Exception e) {
            log.error("find back category by id={} failed, cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("category.find.fail");
        }
    }

    @Override
    public Response<FrontCategory> findFrontCategoryById(long id) {
        try {
            FrontCategory f = frontCategoryDao.load(id);
            if (f == null) {
                log.warn("front category(id={}) isn't existed.");
                return Response.fail("category.not.exist");
            }
            return Response.ok(f);
        } catch (Exception e) {
            log.error("find front category by id={} failed, cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("category.find.fail");
        }
    }

    @Override
    public Response<FrontCategory> findFrontCategoryByIdWithCache(long id) {
        try {
            FrontCategory f = categoryCacher.findFrontCategoryById(id);
            if (f == null) {
                log.warn("front category(id={}) isn't existed.");
                return Response.fail("category.not.exist");
            }
            return Response.ok(f);
        } catch (Exception e) {
            log.error("find front category by id={} failed, cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("category.find.fail");
        }
    }

    @Override
    public Response<List<FrontCategory>> findFrontCategoryByIds(Collection<Long> ids) {
        if (Iters.emptyToNull(ids) == null) {
            return Response.ok(Collections.<FrontCategory>emptyList());
        }
        try {
            return Response.ok(frontCategoryDao.loads(ImmutableSet.copyOf(ids).asList()));
        } catch (Exception e) {
            log.error("find front category by ids failed, ids={}, cause:{}",
                    ids, Throwables.getStackTraceAsString(e));
            return Response.fail("category.find.fail");
        }
    }

    @Override
    public Response<List<FrontCategory>> findFrontCategoryByIdsWithCache(Collection<Long> ids) {
        // TODO: to be completed
        return findFrontCategoryByIds(ids);
    }

    @Override
    public Response<List<BackCategory>> findBackCategoriesByFrontCategoryId(Long frontCategoryId) {
        Response<List<BackCategory>> resp = new Response<List<BackCategory>>();
        try {
            List<BackCategory> backCategories = categoryCacher.findBackCategoriesByFrontCategoryId(frontCategoryId);
            resp.setResult(backCategories);
        } catch (Exception e){
            log.error("failed to find back categories by frontCategoryId(id={}), cause: {}",
                    frontCategoryId, Throwables.getStackTraceAsString(e));
            resp.setError("category.find.fail");
        }
        return resp;
    }

    @Override
    public Response<List<BackCategory>> findBackCategoriesByFrontCategoryIdNoCache(Long id) {
        Response<List<BackCategory>> resp = new Response<List<BackCategory>>();
        try {
            resp.setResult(categoryCacher.doLoadBackCategoriesByFid(id));
        } catch (Exception e){
            log.error("failed to find front category(id={})'s back categories, cause: {}", id, Throwables.getStackTraceAsString(e));
            resp.setError("category.find.fail");
        }
        return resp;
    }

    @Override
    public Response<List<CategoryBinding>> findBindingsByFrontCategoryId(Long id) {
        Response<List<CategoryBinding>> resp = new Response<List<CategoryBinding>>();
        try {
            resp.setResult(appendBackCategoryPath(categoryBindingDao.findByFrontCategoryId(id)));
        } catch (Exception e){
            log.error("failed to find front category(id={})'s category bindings, cause: {}",
                    id, Throwables.getStackTraceAsString(e));
            resp.setError("category.binding.find.fail");
        }
        return resp;
    }

    @Override
    public Response<List<CategoryBinding>> findBindingsByBackCategoryId(long id) {
        try {
            return Response.ok(appendBackCategoryPath(categoryBindingDao.findByBackCategoryId(id)));
        } catch (Exception e) {
            log.error("failed to find back category(id={})'s category bindings, cause:{}",
                    id, Throwables.getStackTraceAsString(e));
            return Response.fail("category.binding.find.fail");
        }
    }

    /**
     * TODO: dirty code, to be refactored
     */
    private List<CategoryBinding> appendBackCategoryPath(List<CategoryBinding> bindings) {
        if (bindings == null) {
            return null;
        }
        for (CategoryBinding binding : bindings) {
            if (binding == null || binding.getBackCategoryId() == null) {
                continue;
            }
            Response<List<BackCategory>> resp = findAncestorsByBackCategoryId(binding.getBackCategoryId());
            if (!resp.isSuccess()) {
                log.error("find back category path failed, id={}, error={}",
                        binding.getBackCategoryId(), resp.getError());
            } else {
                List<String> path = new ArrayList<>();
                for (BackCategory bc : Iters.nullToEmpty(resp.getResult())) {
                    if (bc != null && !Strings.isNullOrEmpty(bc.getName())) {
                        path.add(bc.getName());
                    }
                }
                binding.setPath(Joiner.on(" > ").skipNulls().join(path));
            }
        }
        return bindings;
    }

    @Override
    public Response<RichCategory<BackCategory>> findChildrenByBackCategoryId(@Nullable Long id) {
        try {
            return Response.ok(categoryCacher.doLoadRichBackCategory(MoreObjects.firstNonNull(id, 0l)));
        } catch (Exception e) {
            log.error("find children of back category(id={}) failed, cause:{}",
                    id, Throwables.getStackTraceAsString(e));
            return Response.fail("category.find.children.fail");
        }
    }

    @Override
    public Response<RichCategory<BackCategory>> findChildrenByBackCategoryIdWithCache(@Nullable Long id) {
        try {
            return Response.ok(categoryCacher.findChildrenByBackCategoryId(MoreObjects.firstNonNull(id, 0l)));
        } catch (Exception e) {
            log.error("find children of back category(id={}) with cache failed, cause:{}",
                    id, Throwables.getStackTraceAsString(e));
            return Response.fail("category.find.children.fail");
        }
    }

    @Override
    public Response<RichCategory<BackCategory>> findChildrenByBackCategoryIdWithCacheAndPerm(
            LoginUser loginUser, @Nullable Long id) {
        if (loginUser == null) {
            log.warn("can't find children of back category(id={}), cause the perm-check ver. user not login", id);
            return Response.fail("authorize.fail");
        }
        try {
            id = MoreObjects.firstNonNull(id, 0l);

            List<Long> path = categoryQueries.ancestorPath(id, backCategoryDao);
            BackendPerms.Perm perm = backendPerms.buildPerm(loginUser.getId(), loginUser.getType());
            if (!perm.checkPathPerm(path)) {
                log.warn("user has no permission to open back category, loginUser={}, bid={}",
                        loginUser, id);
                return Response.fail("category.find.no.perm");
            }

            RichCategory<BackCategory> richCategory = new RichCategory<>();
            BeanUtils.copyProperties(RespHelper.orServEx(findChildrenByBackCategoryIdWithCache(id)), richCategory);

            List<BackCategory> bcs = Lists.newArrayList();
            for (BackCategory bc : richCategory.getChildren()) {
                Long bid = bc.getId();
                if (perm.checkPathPerm(FluentIterable.from(path).append(bid).toList())) {
                    bcs.add(bc);
                }
            }
            richCategory.setChildren(bcs);
            richCategory.getParent().setHasChildren(!bcs.isEmpty());
            return Response.ok(richCategory);
        } catch (ServiceException e) {
            log.warn("find children of back category(id={}) with cache and perm(loginUser={}) failed, error:{}",
                    id, loginUser, e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("find children of back category(id={}) with cache and perm(loginUser={}) failed, cause:{}",
                    id, loginUser, Throwables.getStackTraceAsString(e));
            return Response.fail("category.find.children.fail");
        }
    }

    @Override
    public Response<RichCategory<BackCategory>> findChildrenNoBusinessByBackCategoryId(long id) {
        Response<RichCategory<BackCategory>> resp = findChildrenByBackCategoryId(id);
        if (resp.isSuccess()) {
            List<BackCategory> children = new ArrayList<>();
            for (BackCategory bc : Iters.nullToEmpty(resp.getResult().getChildren())) {
                if (bc != null && bc.getShopBusinessId() == null) {
                    children.add(bc);
                }
            }
            resp.getResult().setChildren(children);
        }
        return resp;
    }

    private boolean isPathSafe(List<String> path) {
        if (path == null || path.isEmpty()) return false;
        for (String s : path) {
            if (Strings.isNullOrEmpty(s)) return false;
        }
        return true;
    }

    private List<String> checkPathSafe(List<String> path) throws ServiceException {
        checkResult(isPathSafe(path), "category.path.invalid");
        return path;
    }

    private List<BackCategory> buildRoadByPath(List<String> path) throws ServiceException {
        List<BackCategory> road = new ArrayList<>();
        long pid = 0;
        for (String s : path) {
            BackCategory bc = backCategoryDao.findByNameAndPid(pid, s);
            ServiceUtils.checkResult(bc != null, "category.not.exist");
            road.add(bc);
            pid = bc.getId();
        }
        BackCategory leaf = Iterables.getLast(road);
        checkResult(!leaf.getHasChildren(), "category.path.invalid.not.pass.to.leaf");
        return road;
    }

    private List<Long> transformFromRoad(List<BackCategory> road) throws ServiceException {
        List<Long> ids = Lists.newArrayList(0l);
        for (BackCategory bc : road) {
            checkResult(bc.getId() != null, "category.id.invalid");
            ids.add(bc.getId());
        }
        return ids;
    }

    @Override
    public Response<BackCategory> getLeafByBackCategoryPath(List<String> path) {
        try {
            return Response.ok(Iterables.getLast(buildRoadByPath(checkPathSafe(path))));
        } catch (ServiceException e) {
            log.warn("find leaf by back category path={} failed, error:{}",
                    path, e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("find leaf by back category path={} failed, cause:{}",
                    path, Throwables.getStackTraceAsString(e));
            return Response.fail("category.find.fail");
        }
    }

    @Override
    public Response<BackCategory> getLeafByBackCategoryPathWithPerm(long userId, List<String> path) {
        try {
            List<BackCategory> road = buildRoadByPath(checkPathSafe(path));
            List<Long> idPath = transformFromRoad(road);

            User user = RespHelper.orServEx(userReadService.findById(userId));
            final BackendPerms.Perm perm = backendPerms.buildPerm(userId, user.getType());
            checkResult(perm.checkPathPerm(idPath), "category.find.no.perm");
            return Response.ok(Iterables.getLast(road));
        } catch (ServiceException e) {
            log.warn("find leaf by back category path={} with perm failed, userId={}, error:{}",
                    path, userId, e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("find leaf by back category path={} with perm failed, userId={}, cause:{}",
                    path, userId, Throwables.getStackTraceAsString(e));
            return Response.fail("category.find.fail");
        }
    }

    @Override
    public Response<Map<Long, Integer>> getBackCategoryAllows(Long userId) {
        try {
            checkArgument(userId != null && userId > 0, "no user selected for search brand perm");

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
                Map<Long, Integer> result = new HashMap<>();
                return Response.ok(result);
            }

            BackendPerms.Perm perm = backendPerms.buildPerm(user.getId(), user.getType());

            return Response.ok(perm.getTreeAllow());
        } catch (Exception e) {
            log.error("get back category allows failed, cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("category.find.fail");
        }
    }

    @Override
    public Response<Boolean> checkCategoryPermByPath(long userId, List<String> path) {
        if (path == null) {
            return Response.fail("category.not.exist");
        }
        for (String s : path) {
            if (Strings.isNullOrEmpty(s)) {
                return Response.fail("category.not.exist");
            }
        }
        try {
            User user = RespHelper.orServEx(userReadService.findById(userId));
            List<Long> lp = Lists.newArrayList(0l);
            long pid = 0;
            for (String s : path) {
                BackCategory bc = backCategoryDao.findByNameAndPid(pid, s);
                if (bc == null) {
                    return Response.fail("category.not.exist");
                }
                pid = bc.getId();
                lp.add(bc.getId());
            }
            BackendPerms.Perm perm = backendPerms.buildPerm(userId, user.getType());
            return Response.ok(perm.checkPathPerm(lp));
        } catch (ServiceException e) {
            log.warn("check category perm by path failed, userId={}, path={}, error={}",
                    userId, path, e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("check category perm by path failed, userId={}, path={}, cause:{}",
                    userId, path, Throwables.getStackTraceAsString(e));
            return Response.fail("category.find.fail");
        }
    }

    @Override
    public Response<Boolean> checkCategoryPermByPath(long userId, String... path) {
        if (Iters.emptyToNull(path) == null) {
            return Response.fail("category.not.exist");
        }
        return checkCategoryPermByPath(userId, Arrays.asList(path));
    }

    @Override
    public Response<RichCategory<FrontCategory>> findChildrenByFrontCategoryId(Long id) {
        try {
            return Response.ok(categoryCacher.doLoadRichFrontCategory(MoreObjects.firstNonNull(id, 0L)));
        } catch (Exception e){
            log.error("failed to find front category(id={})'s children, cause: {}",
                    id, Throwables.getStackTraceAsString(e));
            return Response.fail("category.find.children.fail");
        }
    }

    /**
     * 查询某前台类目的孩子类目列表
     *
     * @param id 前台类目id, NULL查询顶级类目
     * @return 前台类目的孩子类目列表
     */
    @Override
    public Response<RichCategory<FrontCategory>> findChildrenByFrontCategoryIdWithCache(Long id) {
        try {
            return Response.ok(categoryCacher.findChildrenByFrontCategoryId(MoreObjects.firstNonNull(id, 0L)));
        } catch (Exception e){
            log.error("failed to find front category(id={})'s children, cause: {}",
                    id, Throwables.getStackTraceAsString(e));
            return Response.fail("category.find.children.fail");
        }
    }

    @Override
    public Response<List<BackCategory>> findAncestorsByBackCategoryId(long id) {
        try {
            return Response.ok(categoryCacher.doLoadBackCategoryTree(id));
        } catch (Exception e) {
            log.error("find ancestors by back category id={} failed, cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("category.find.ancestor.fail");
        }
    }

    @Override
    public Response<List<BackCategory>> findAncestorsByBackCategoryIdWithCache(long id) {
        try {
            return Response.ok(categoryCacher.findBackCategoryTreeByBid(id));
        } catch (Exception e) {
            log.error("find ancestors by back category id={} with cache failed, cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("category.find.ancestor.fail");
        }
    }

    @Override
    public Response<List<FrontCategory>> findAncestorsByFrontCategoryId(long id) {
        try {
            return Response.ok(categoryCacher.doLoadFrontCategoryTree(id));
        } catch (Exception e) {
            log.error("find ancestors by front category id={} failed, cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("category.find.ancestor.fail");
        }
    }

    @Override
    public Response<List<FrontCategory>> findAncestorsByFrontCategoryIdWithCache(long id) {
        try {
            return Response.ok(categoryCacher.findFrontCategoryTreeByBid(id));
        } catch (Exception e) {
            log.error("find ancestors by front category id={} with cache failed, cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("category.find.ancestor.fail");
        }
    }

    @Override
    public Response<List<FrontCategory>> findAncestorsByFrontCategoryIdWithCacheNullSafe(@Nullable Long id) {
        if (id == null) return Response.ok(Collections.<FrontCategory>emptyList());
        return findAncestorsByFrontCategoryIdWithCache(id);
    }

    @Override
    public Response<List<BackCategory>> findBcPathBySpuId(long spuId) {
        try {
            Spu spu = spuDao.load(spuId);
            if (spu == null) {
                log.warn("spu not exist, id={}", spuId);
                return Response.fail("spu.not.exist");
            }
            if (spu.getCategoryId() == null) {
                log.warn("spu(id={})'s categoryId null", spuId);
                return Response.fail("spu.illegal");
            }
            return findAncestorsByBackCategoryId(spu.getCategoryId());
        } catch (Exception e) {
            log.error("find bcPath by spuId={} failed, cause:{}",
                    spuId, Throwables.getStackTraceAsString(e));
            return Response.fail("category.find.fail");
        }
    }

    @Override
    public Response<List<BackCategory>> findBcPathByItemId(long itemId) {
        try {
            Item item = itemDao.load(itemId);
            if (item == null) {
                log.warn("item not exist, id={}", itemId);
                return Response.fail("item.not.exist");
            }
            if (item.getSpuId() == null) {
                log.warn("item(id={})'s spuId null", itemId);
                return Response.fail("item.spu.invalid");
            }
            return findBcPathBySpuId(item.getSpuId());
        } catch (Exception e) {
            log.error("find bcPath by itemId={} failed, cause:{}",
                    itemId, Throwables.getStackTraceAsString(e));
            return Response.fail("category.find.fail");
        }
    }

    @Override
    public Response<ShopBusiness> findBusinessByCategoryId(long categoryId) {
        return RespHelper.Opt.unwrap(findBusinessByCategoryIdAllowNotFound(categoryId), "shop.business.find.fail.not.found");
    }

    @Override
    public Response<Optional<ShopBusiness>> findBusinessByCategoryIdAllowNotFound(long categoryId) {
        try {
            BackCategory bc = backCategoryDao.load(categoryId);
            if (bc == null) {
                return RespHelper.Opt.absent();
            }
            while (true) {
                if (bc.getShopBusinessId() != null) {
                    // found it
                    break;
                }
                if (bc.getLevel() == null || bc.getPid() == null) {
                    // illegal data
                    break;
                }
                if (bc.getLevel() <= 1) {
                    // last item
                    break;
                }
                BackCategory tmp = backCategoryDao.load(bc.getPid());
                if (tmp == null) {
                    // illegal data
                    break;
                }
                bc = tmp;
            }
            Long businessId = bc.getShopBusinessId();
            if (businessId == null) {
                return RespHelper.Opt.absent();
            }
            List<ShopBusiness> businesses = RespHelper.orServEx(shopReadService.listShopBusinesses());
            for (ShopBusiness shopBusiness : Iters.nullToEmpty(businesses)) {
                if (Objects.equals(businessId, shopBusiness.getId())) {
                    return RespHelper.Opt.of(shopBusiness);
                }
            }
            return RespHelper.Opt.absent();
        } catch (ServiceException e) {
            log.warn("find business by category id={}, failed, error={}",
                    categoryId, e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("find business by category id={} failed, cause:{}",
                    categoryId, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.business.find.fail");
        }
    }

    @Override
    public Response<Map<Long, ShopBusiness>> bulkFindBusinessByCategoryIds(Collection<Long> categoryIds) {
        try {
            if (categoryIds == null || categoryIds.isEmpty()) {
                return RespHelper.Map.empty();
            }
            List<Long> uniqueCategoryIds = FluentIterable.from(categoryIds).filter(Predicates.notNull()).toSet().asList();
            Map<Long, BackCategory> trans = new HashMap<>();
            List<BackCategory> bcs = backCategoryDao.loads(uniqueCategoryIds);
            for (BackCategory bc : Iters.nullToEmpty(bcs)) {
                if (bc == null || bc.getId() == null) {
                    continue;
                }
                trans.put(bc.getId(), bc);
            }
            while (true) {
                Map<Long, Long> action = new HashMap<>();
                for (Map.Entry<Long, BackCategory> entry : trans.entrySet()) {
                    BackCategory bc = entry.getValue();
                    if (bc.getShopBusinessId() != null) {
                        // already found
                        continue;
                    }
                    if (bc.getLevel() == null || bc.getPid() == null) {
                        // illegal data
                        continue;
                    }
                    if (bc.getLevel() <= 1) {
                        // last item
                        continue;
                    }
                    action.put(entry.getKey(), bc.getPid());
                }
                if (action.isEmpty()) {
                    break;
                }
                List<Long> uniqueActionIds = FluentIterable.from(action.values()).filter(Predicates.notNull()).toSet().asList();
                if (uniqueActionIds.isEmpty()) {
                    break;
                }
                Map<Long, BackCategory> bcMap = Fns.safeToMap(backCategoryDao.loads(uniqueActionIds));
                for (Map.Entry<Long, Long> entry : action.entrySet()) {
                    Long toReplaceId = entry.getKey();
                    Long distId = entry.getValue();
                    BackCategory dist = bcMap.get(distId);
                    if (dist == null) {
                        continue;
                    }
                    if (trans.get(toReplaceId) == null) {
                        continue;
                    }
                    trans.put(toReplaceId, dist);
                }
            }
            Map<Long, ShopBusiness> businesses = Fns.safeToMap(RespHelper.orServEx(shopReadService.listShopBusinesses()));
            Map<Long, ShopBusiness> result = new HashMap<>();
            for (Map.Entry<Long, BackCategory> entry : trans.entrySet()) {
                Long categoryId = entry.getKey();
                BackCategory bc = entry.getValue();
                if (bc == null || bc.getShopBusinessId() == null) {
                    continue;
                }
                ShopBusiness shopBusiness = businesses.get(bc.getShopBusinessId());
                if (shopBusiness == null) {
                    continue;
                }
                result.put(categoryId, shopBusiness);
            }
            return Response.ok(result);
        } catch (ServiceException e) {
            log.warn("bulk find business by category ids={} failed, error={}",
                    categoryIds, e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("bulk find business by category ids={} failed, cause:{}",
                    categoryIds, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.business.find.fail");
        }
    }

    @Override
    public Response<List<List<BackCategory>>> findCategoryPathsByBusiness(long businessId) {
        try {
            List<BackCategory> backCategories = backCategoryDao.findByBusiness(businessId);
            List<List<BackCategory>> paths = new ArrayList<>();
            for (BackCategory backCategory : Iters.nullToEmpty(backCategories)) {
                List<BackCategory> path = RespHelper.orServEx(findAncestorsByBackCategoryId(backCategory.getId()));
                paths.add(path);
            }
            return Response.ok(paths);
        } catch (ServiceException e) {
            log.warn("find category paths by business(id={}) failed, error={}",
                    businessId, e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("find category paths by business(id={}) failed, cause:{}",
                    businessId, Throwables.getStackTraceAsString(e));
            return Response.fail("category.find.fail");
        }
    }

    /**
     * 查询指定id列表的前台类目
     *
     * @param ids 一级类目的id串, 以","分割
     * @return 前台类目列表
     */
    @Override
    public Response<List<FrontCategoryTree>> findFrontCategoryByIds(String ids) {

        Response<List<FrontCategoryTree>> result = new Response<List<FrontCategoryTree>>();
        if(Strings.isNullOrEmpty(ids)){
            log.warn("no front category ids provided");
            result.setResult(Collections.<FrontCategoryTree>emptyList());
            return result;
        }
        try {
            List<String> idList = Splitters.COMMA.splitToList(ids);
            List<FrontCategoryTree> fcts = Lists.newArrayListWithCapacity(idList.size());
            for (String id : idList) {
                FrontCategoryTree fct = new FrontCategoryTree();
                //查找一级类目
                final long firstLevelId = Long.parseLong(id);
                FrontCategory firstLevelFC = frontCategoryDao.load(firstLevelId);
                if(firstLevelFC == null){
                    log.warn("failed to find 1st front category(id={}), skip",id );
                    fcts.add(fct); // TODO 既然没找到为什么还要 add 进去
                    continue;
                }
                fct.setFirstId(firstLevelId);
                fct.setFirstFrontCategory(firstLevelFC);

                //查找二级类目
                List<FrontCategory> secondLevels = frontCategoryDao.findByPid(firstLevelId);
                List<FrontCategoryNav> fcns = Lists.newArrayListWithCapacity(secondLevels.size());
                for (FrontCategory secondLevel : secondLevels) {
                    FrontCategoryNav fcn = new FrontCategoryNav();
                    fcn.setSecondLevel(secondLevel);
                    //查找三级类目
                    List<FrontCategory> thirdLevels = frontCategoryDao.findByPid(secondLevel.getId());
                    fcn.setThirdLevel(thirdLevels);
                    fcns.add(fcn);
                }
                fct.setChildFrontCategory(fcns);
                fcts.add(fct);
            }
            result.setResult(fcts);
            return result;
        } catch (Exception e) {
            log.error("failed to find front category hierarchy by ids {} , cause:{}", ids, Throwables.getStackTraceAsString(e));
            result.setError("frontCategory.query.fail");
            return result;
        }
    }

    @Override
    public Response<Tree<BackCategory>> findBackCategoryTreeByRootId(
            BaseUser baseUser, @Nullable Long rootId, @Nullable Integer deep, @Nullable Boolean cache) {
        if (rootId == null) rootId = 0l;
        if (deep == null || deep > 10) deep = 10;
        if (deep <= 0) deep = 1;
        if (cache == null) cache = false;

        try {
            Tree<BackCategory> result = categoryQueries.backTree(rootId, deep, cache);
            if (result == null) {
                log.warn("backend tree find failed, rootId={}, deep={}, cache={}", rootId, deep, cache);
            }
            return Response.ok(result);
        } catch (Exception e) {
            log.error("find back category tree by rootId={} failed, deep={}, cache={}, cause:{}",
                    rootId, deep, cache, Throwables.getStackTraceAsString(e));
            return Response.fail("category.find.fail");
        }
    }

    @Override
    public Response<Tree<BackCategory>> findSubtreeByCategoryId(long id) {
        try {
            // fix id
            if (id < 0) {
                id = 0;
            }
            Tree<BackCategory> result = categoryQueries.backTree(id, 5, false);
            if (result == null) {
                log.error("find subtree by categoryId={} failed, so result", id);
            }
            return Response.ok(result);
        } catch (Exception e) {
            log.error("find subtree by categoryId={} failed, cause:{}",
                    id, Throwables.getStackTraceAsString(e));
            return Response.fail("category.find.fail");
        }
    }

    @Override
    public Response<Tree<FrontCategory>> findFrontCategoryTreeByRootId(
            @Nullable Long rootId, @Nullable Integer deep, @Nullable Boolean cache) {
        if (rootId == null) rootId = 0l;
        if (deep == null || deep > 10) deep = 10;
        if (deep <= 0) deep = 1;
        if (cache == null) cache = false;

        try {
            Tree<FrontCategory> result = categoryQueries.getFrontTree(rootId, deep, cache);
            if (result == null) {
                log.warn("frontend tree find failed, rootId={}, deep={}, {}", rootId, deep);
                return Response.fail("category.find.fail");
            }
            return Response.ok(result);
        } catch (Exception e) {
            log.error("find front category tree by root id={} failed, deep={}, cause:{}",
                    rootId, deep, Throwables.getStackTraceAsString(e));
            return Response.fail("category.find.fail");
        }
    }

    @Override
    public Response<Spu> findSpuById(long id) {
        try {
            Spu spu = spuDao.load(id);
            if (spu == null){
                log.warn("spu(id={}) isn't existed.", id);
                return Response.fail("spu.not.exist");
            }
            return Response.ok(spu);
        } catch (Exception e) {
            log.error("find spu by id={} failed, cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("spu.find.fail");
        }
    }

    @Override
    public Response<Spu> findSpuByIdWithCache(long id) {
        try {
            Spu spu = categoryCacher.findSpuById(id);
            if (spu == null){
                log.warn("spu(id={}) isn't existed.", id);
                return Response.fail("spu.not.exist");
            }
            return Response.ok(spu);
        } catch (Exception e) {
            log.error("find spu by id={} with cache failed, cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("spu.find.fail");
        }
    }

    /**
     * 根据后台叶子类目id查询其下挂载的SPU列表
     *
     * @param id 后台叶子类目id
     * @return SPU列表
     */
    @Override
    public Response<List<Spu>> findSpusByCategoryId(long id) {
        Response<List<Spu>> resp = new Response<List<Spu>>();
        try {
            resp.setResult(categoryCacher.doLoadBackCategorySpusByBid(id));
        } catch (Exception e){
            log.error("failed to find spu by back category(id={}), cause: {}", id, Throwables.getStackTraceAsString(e));
            resp.setError("spu.find.fail");
        }
        return resp;
    }

    @Override
    public Response<List<Spu>> findSpusByCategoryIdWithCacheAndPerm(long userId, long id) {
        try {
            User user = RespHelper.orServEx(userReadService.findById(userId));
            EcpLoginUser loginUser = new EcpLoginUser();
            loginUser.setId(user.getId());
            loginUser.setType(user.getType());
            // TODO: to be refactored
            return findSpusByCategoryIdWithCacheAndPerm(Optional.<LoginUser>of(loginUser), id);
        } catch (ServiceException e) {
            log.warn("find spus by category id={} with cache and perm failed, userId={}, error:{}",
                    id, userId, e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("find spus by category id={} with cache and perm failed, userId={}, cause:{}",
                    id, userId, Throwables.getStackTraceAsString(e));
            return Response.fail("spu.find.fail");
        }
    }

    @Override
    public Response<List<Spu>> findSpusByCategoryIdWithCacheAndPerm(Optional<LoginUser> loginUser, long id) {
        Response<List<Spu>> resp = new Response<List<Spu>>();
        try {
            if (!loginUser.isPresent()) {
                return Response.fail("category.find.no.perm");
            }
            List<Long> path = categoryQueries.ancestorPath(id, backCategoryDao);

            final BackendPerms.Perm perm = backendPerms.buildPerm(loginUser.get().getId(), loginUser.get().getType());
            if (!perm.checkPathPerm(path)) {
                log.warn("user has no permission to open back category, loginUser={}, bid={}",
                        loginUser, id);
                return Response.fail("category.find.no.perm");
            }

            List<Spu> spus = categoryCacher.findSpusByBackCategoryId(id);

            resp.setResult(FluentIterable.from(spus).filter(new Predicate<Spu>() {
                @Override
                public boolean apply(@Nullable Spu input) {
                    return input != null && input.getBrandId() != null && input.getBrandId() > 0
                            && perm.checkBrandPerm(Long.valueOf(input.getBrandId()));
                }
            }).toList());
        } catch (Exception e){
            log.error("failed to find spu by back category(id={}), cause: {}", id, Throwables.getStackTraceAsString(e));
            resp.setError("spu.find.fail");
        }
        return resp;
    }

    @Override
    public Response<Boolean> checkSpusPerm(Set<Long> spuIds, BaseUser baseUser) {
        try {
            if (spuIds.isEmpty()) {
                return Response.ok(Boolean.TRUE);
            }
            List<Spu> spus = spuDao.loads(Lists.newArrayList(spuIds));
            if (spus.isEmpty()) {
                return Response.ok(Boolean.FALSE);
            }

            BackendPerms.Perm perm = backendPerms.buildPerm(baseUser.getId(), baseUser.getType());

            for (Spu spu : spus) {
                Long bid = spu.getCategoryId();
                checkState(bid != null && bid > 0, "spu invalid, bid must not null nor <= 0");

                List<Long> path = categoryQueries.ancestorPath(bid, backCategoryDao);

                if (!perm.checkPathPerm(path)) {
                    log.debug("user has no permission to open back category, baseUser={}, bid={}",
                            baseUser, bid);
                    return Response.ok(Boolean.FALSE);
                }

                Integer brandId = spu.getBrandId();
                checkState(brandId != null && brandId > 0, "spu invalid, brandId must not null nor <= 0");

                if (!perm.checkBrandPerm((long) brandId)) {
                    log.debug("user has no permission to open brand, id={}, baseUser={}", brandId, baseUser);
                    return Response.ok(Boolean.FALSE);
                }
            }

            return Response.ok(Boolean.TRUE);

        } catch (Exception e) {
            log.error("check spus perm failed, spuIds={}, baseUser={}, cause:{}",
                    spuIds, baseUser, Throwables.getStackTraceAsString(e));
            return Response.fail("spu.find.fail");
        }
    }

    @Override
    public Response<Boolean> checkAllBcPerm(Set<Long> bcIds, long userId) {
        try {
            User user = RespHelper.orServEx(userReadService.findById(userId));
            BackendPerms.Perm perm = backendPerms.buildPerm(user.getId(), user.getType());
            for (Long bcId : bcIds) {
                List<Long> path = categoryQueries.ancestorPath(bcId, backCategoryDao);
                if (!perm.checkPathPerm(path)) {
                    return Response.ok(Boolean.FALSE);
                }
            }
            return Response.ok(Boolean.TRUE);
        } catch (ServiceException e) {
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("check perm of bc failed, bcIds={}, userId={}, cause:{}",
                    bcIds, userId, Throwables.getStackTraceAsString(e));
            return Response.fail("category.find.fail");
        }
    }

    @Override
    public Response<List<AttributeKey>> findCategoryAttributeKeys(long categoryId) {
        try {
            List<CategoryAttributeKey> keys = categoryAttributeKeyDao.findBy(categoryId);
            List<AttributeKey> result = FluentIterable.from(keys)
                    .filter(Predicates.notNull())
                    .transform(new Function<CategoryAttributeKey, AttributeKey>() {
                        @Override
                        public AttributeKey apply(CategoryAttributeKey input) {
                            AttributeKey key = new AttributeKey();
                            key.setId(input.getKeyId());
                            key.setName(input.getKeyName());
                            key.setValueType(input.getKeyType());
                            return key;
                        }
                    }).toList();
            return Response.ok(result);
        } catch (Exception e) {
            log.error("find category attribute keys failed, categoryId={}, cause:{}",
                    categoryId, Throwables.getStackTraceAsString(e));
            return Response.fail("category.attribute.key.find.fail");
        }
    }

    @Override
    public Response<List<AttributeValue>> findCategoryAttributeValues(long categoryId, long keyId) {
        try {
            List<CategoryAttributeValue> values = categoryAttributeValueDao.findBy(categoryId, keyId);
            List<AttributeValue> result = FluentIterable.from(values)
                    .filter(Predicates.notNull())
                    .transform(new Function<CategoryAttributeValue, AttributeValue>() {
                        @Override
                        public AttributeValue apply(CategoryAttributeValue input) {
                            AttributeValue value = new AttributeValue();
                            value.setId(input.getValueId());
                            value.setValue(input.getValue());
                            value.setLogo(input.getLogo());
                            return value;
                        }
                    }).toList();
            return Response.ok(result);
        } catch (Exception e) {
            log.error("find category attribute values failed, categoryId={}, keyId={}, cause:{}",
                    categoryId, keyId, Throwables.getStackTraceAsString(e));
            return Response.fail("category.attribute.value.find.fail");
        }
    }

    @Override
    public Response<List<RichAttribute>> findSpuAttributes(Long spuId) {
        Response<List<RichAttribute>> resp = new Response<List<RichAttribute>>();
        try {
            resp.setResult(categoryCacher.findSpuRichAttributes(spuId));
        } catch (Exception e){
            log.warn("failed to find spu(id={})'s attributes", spuId);
            resp.setError("spu.attribute.find.fail");
        }
        return resp;
    }

    @Override
    public Response<List<RichAttribute>> findSpuAttributesNoCache(Long spuId) {
        Response<List<RichAttribute>> resp = new Response<List<RichAttribute>>();
        try {
            resp.setResult(categoryCacher.doLoadSpuAttributesBySpuId(spuId));
        } catch (Exception e){
            log.error("failed to find spu(id={})'s attributes no cache, cause: {}", spuId, Throwables.getStackTraceAsString(e));
            resp.setError("spu.attribute.find.fail");
        }
        return resp;
    }

    private long readCategoryIdFromSpu(long spuId) throws ServiceException {
        Spu spu = spuDao.load(spuId);
        checkResult(spu != null, "spu.not.exist");
        checkResult(spu.getCategoryId() != null, "spu.illegal");
        return spu.getCategoryId();
    }

    @Override
    public Response<List<AttributeKey>> findSkuKeys(long spuId) {
        try {
            long categoryId = readCategoryIdFromSpu(spuId);
            // FIXME(Effet): dirty hack
            List<AttributeKey> skuKeys = attributeKeyDao.findSkuKeysBySpuId(spuId);
            for (AttributeKey skuKey : skuKeys) {
                long skuKeyId = skuKey.getId();
                CategoryAttributeKey realKey = unwrap(categoryAttributeKeyDao.findBy(categoryId, skuKeyId), "spu.attribute.id.invalid"); // TODO(Effet): proper msg
                skuKey.setValueType(realKey.getKeyType());
            }
            return Response.ok(skuKeys);
        } catch (ServiceException e) {
            log.warn("find sku keys failed, spuId={}, error={}", spuId, e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("find sku keys failed, spuId={}, cause:{}",
                    spuId, Throwables.getStackTraceAsString(e));
            return Response.fail("spu.sku.find.fail");
        }
    }

    @Override
    public Response<List<AttributeKeyValues>> findSkuAttributes(long spuId) {
        try {
            long categoryId = readCategoryIdFromSpu(spuId);

            // FIXME(Effet): dirty hack
            List<AttributeKey> skuKeys = attributeKeyDao.findSkuKeysBySpuId(spuId);
            List<AttributeKeyValues> result = Lists.newArrayListWithExpectedSize(skuKeys.size());
            for (AttributeKey skuKey : skuKeys) {
                long skuKeyId = skuKey.getId();
                CategoryAttributeKey realKey = unwrap(categoryAttributeKeyDao.findBy(categoryId, skuKeyId), "spu.attribute.id.invalid"); // TODO(Effet): proper msg
                skuKey.setValueType(realKey.getKeyType());

                List<CategoryAttributeValue> realValues = Iters.emptyToNull(categoryAttributeValueDao.findBy(categoryId, skuKeyId));
                checkResult(realValues != null, "spu.attribute.id.invalid"); // TODO(Effet): proper msg
                List<AttributeValue> values = Lists.newArrayListWithExpectedSize(realValues.size());
                for (CategoryAttributeValue realValue : realValues) {
                    AttributeValue value = new AttributeValue();
                    value.setId(realValue.getValueId());
                    value.setValue(realValue.getValue());
                    value.setLogo(realValue.getLogo());
                    values.add(value);
                }

                AttributeKeyValues kv = new AttributeKeyValues();
                kv.setAttributeKey(skuKey);
                kv.setAttributeValues(values);
                result.add(kv);
            }
            return Response.ok(result);
        } catch (ServiceException e) {
            log.warn("find sku attributes failed, spuId={}, error:{}", spuId, e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("find sku attributes failed, spuId={}, cause:{}",
                    spuId, Throwables.getStackTraceAsString(e));
            return Response.fail("spu.sku.find.fail");
        }
    }

    /**
     * 查找sku属性
     *
     * @param spuId SPU编号
     * @return sku属性及属性值
     */
    @Override
    public Response<RichSpu> findRichSpuById(Long spuId) {
        Response<RichSpu> resp = new Response<RichSpu>();
        try {
            RichSpu richSpu = new RichSpu();
            Spu spu = categoryCacher.findSpuById(spuId);
            if (spu == null){
                log.warn("spu(id={}) isn't exist.", spuId);
                resp.setError("spu.not.exist");
                return resp;
            }
            richSpu.setSpu(spu);
            List<RichAttribute> spuRichAttributes = categoryCacher.doLoadSpuAttributesBySpuId(spuId);
            List<AttributeKey> skuAttributes = categoryCacher.doLoadSpuSkuKeysBySpuId(spuId);
            List<AttributeDto> attributeDtos = buildAttributeDtos(spuRichAttributes, skuAttributes);
            richSpu.setAttributes(attributeDtos);
            resp.setResult(richSpu);
        } catch (Exception e){
            log.error("failed to find rich spu(id={}), cause: {}", spuId, Throwables.getStackTraceAsString(e));
            resp.setError("spu.find.fail");
        }
        return resp;
    }

    /**
     * 查找sku属性
     *
     * @param spuId SPU编号
     * @return sku属性及属性值
     */
    @Override
    public Response<RichSpu> findRichSpuByIdNoCache(Long spuId) {
        Response<RichSpu> resp = new Response<RichSpu>();
        try {
            RichSpu richSpu = new RichSpu();
            Spu spu = spuDao.load(spuId);
            if (spu == null){
                log.warn("spu(id={}) isn't exist.", spuId);
                resp.setError("spu.not.exist");
                return resp;
            }
            richSpu.setSpu(spu);
            List<RichAttribute> spuRichAttributes = categoryCacher.doLoadSpuAttributesBySpuId(spuId);
            List<AttributeKey> skuAttributes = attributeKeyDao.findSkuKeysBySpuId(spuId);
            List<AttributeDto> attributeDtos = buildAttributeDtos(spuRichAttributes, skuAttributes);
            richSpu.setAttributes(attributeDtos);
            resp.setResult(richSpu);
        } catch (Exception e){
            log.error("failed to find rich spu(id={}), cause: {}", spuId, Throwables.getStackTraceAsString(e));
            resp.setError("spu.find.fail");
        }
        return resp;
    }

    private List<AttributeDto> buildAttributeDtos(List<RichAttribute> spuAttributes, List<AttributeKey> skuAttributeKeys) {
        List<AttributeDto> attributeDtos = Lists.newArrayListWithCapacity(spuAttributes.size() + skuAttributeKeys.size());
        for (RichAttribute spuAttribute : spuAttributes) {
            AttributeDto attributeDto = new AttributeDto(spuAttribute.getAttributeKeyId(), null, spuAttribute.getAttributeValue(), false, spuAttribute.getAttributeKey());
            attributeDtos.add(attributeDto);
        }
        for (AttributeKey skuAttributeKey : skuAttributeKeys) {
            AttributeDto attributeDto = new AttributeDto(skuAttributeKey.getId(), null, null, true, skuAttributeKey.getName());
            attributeDtos.add(attributeDto);
        }
        return attributeDtos;
    }

    /**
     * 查询前台类目绑定的所有后台类目id
     *
     * @param frontCategoryId 前台类目id
     * @return 前台类目绑定的所有后台类目id
     */
    @Override
    public Response<Iterable<Long>> findBackCategoryIdsByFrontCategoryId(Long frontCategoryId) {
        Response<Iterable<Long>> res = new Response<>();
        try {
            Iterable<Long> ids = categoryCacher.findBackCategoryIdsByFrontCategoryId(frontCategoryId);
            res.setResult(ids);
        } catch (Exception e) {
            log.error("failed to find backCategoryIds with frontCategoryId:{}, cause: {}",
                    frontCategoryId, Throwables.getStackTraceAsString(e));
            res.setError("front.category.find.fail");
        }
        return res;
    }

    @Override
    public Response<SearchQuery> analyzeSearchQuery(String q) {
        try {
            List<String> queries = analyzeQueryString(q);
            SearchQuery searchQuery = new SearchQuery();
            Long brandId = null;
            Map<Long, FrontCategory> fcMap = new HashMap<>();
            Set<String> fcNames = new HashSet<>();
            Set<String> keywords = new HashSet<>();

            List<AnalyticalKeyword> analyticalKeywords = new ArrayList<>();

            for (String query : queries) {
                query = Params.trimToNull(query);
                if (query == null) continue;
                // 优先判断品牌
                if (brandId == null) {
                    Brand b = brandDao.findByName(query);
                    if (b != null) {
                        brandId = b.getId();
                        AnalyticalKeyword ak = new AnalyticalKeyword();
                        ak.setText(query);
                        ak.setType(AnalyticalKeyword.KeywordType.BRAND);
                        analyticalKeywords.add(ak);
                        continue;
                    }
                }
                List<FrontCategory> fcs = frontCategoryDao.findByName(query);
                if (!fcs.isEmpty()) {
                    for (FrontCategory fc : fcs) {
                        if (fc == null || fc.getName() == null) {
                            continue;
                        }
                        String name = fixFcName(fc.getName());
                        fcMap.put(fc.getId(), fc);
                        if (name.length() > 0) fcNames.add(name);
                    }
                    AnalyticalKeyword ak = new AnalyticalKeyword();
                    ak.setText(query);
                    ak.setType(AnalyticalKeyword.KeywordType.FRONT_CATEGORY);
                    analyticalKeywords.add(ak);
                    continue;
                }
                // plain keyword
                keywords.add(query);
                AnalyticalKeyword ak = new AnalyticalKeyword();
                ak.setText(query);
                ak.setType(AnalyticalKeyword.KeywordType.PLAIN);
                analyticalKeywords.add(ak);
            }
            searchQuery.setBrandId(Optional.fromNullable(brandId));
            searchQuery.setFrontCategories(buildQueryFrontCategory(fcMap, fcNames, keywords, analyticalKeywords));
            searchQuery.setKeywords(keywords);
            searchQuery.setAnalyticalKeywords(analyticalKeywords);
            return Response.ok(searchQuery);
        } catch (Exception e) {
            log.error("analyze search query failed, q={}, cause:{}",
                    q, Throwables.getStackTraceAsString(e));
            return Response.fail("item.build.search.query.fail");
        }
    }

    private static final Pattern KEYWORD_SPLIT_PATTERN = Pattern.compile("(?:[^\\s\"]+|\"[^\"]*\")+");

    private List<String> analyzeQueryString(@Nullable String q) {
        q = Params.trimToNull(q);
        if (q == null) {
            return Collections.emptyList();
        }

        Matcher m = KEYWORD_SPLIT_PATTERN.matcher(q);

        List<String> keywords = new ArrayList<>();
        while (m.find()) {
            String raw = m.group();
            if (raw != null) {
                String keyword = raw.replace('"', ' ').trim();
                if (keyword.length() > 0) {
                    keywords.add(keyword);
                }
            }
        }
        return keywords;
    }

    private Multimap<Long, Long> genAncestors(Set<Long> fcs) {
        Multimap<Long, Long> map = ArrayListMultimap.create();
        for (Long fc : fcs) {
            List<FrontCategory> ancestors = RespHelper.orServEx(findAncestorsByFrontCategoryIdWithCache(fc));
            Set<Long> set = new HashSet<>();
            for (FrontCategory ancestor : ancestors) {
                if (fc.compareTo(ancestor.getId()) != 0) {
                    set.add(ancestor.getId());
                }
            }
            if (!set.isEmpty()) {
                map.putAll(fc, set);
            }
        }
        return map;
    }

    private String fixFcName(String name) {
        return name == null ? null : name.toLowerCase().trim();
    }

    private Optional<SearchQuery.QueryFrontCategory> buildQueryFrontCategory(Map<Long, FrontCategory> fcMap, Set<String> fcNames, Set<String> keywords, List<AnalyticalKeyword> analyticalKeywords) {
        if (fcMap.isEmpty()) {
            return Optional.absent();
        }
        Multimap<Long, Long> ancestorMap = genAncestors(fcMap.keySet());
        Multimap<Long, Long> decedentMap = ArrayListMultimap.create();
        Multimaps.invertFrom(ancestorMap, decedentMap);
        Map<Long, Integer> valueMap = new HashMap<>();
        // init value
        for (Long fcId : fcMap.keySet()) {
            valueMap.put(fcId, 0);
        }
        for (Long fcId : fcMap.keySet()) {
            for (Long maybeAncestor : fcMap.keySet()) {
                Integer value = valueMap.get(fcId);
                if (value == null) {
                    // already no business with fcId
                    break;
                }
                if (Objects.equals(fcId, maybeAncestor)) {
                    continue;
                }
                if (ancestorMap.get(fcId).contains(maybeAncestor)) {
                    // maybeAncestor is fcId's ancestor
                    valueMap.put(fcId, value + 1);
                    // TODO: 放弃这个优化真的好吗？
                    // valueMap.remove(maybeAncestor);
                }
            }
        }
        // 只保留价值最高的前台类目
        Map<Long, FrontCategory> remainFcs = new HashMap<>();
        // 删除价值最高前台类目的前驱 (不退化成普通关键字)
        Map<Long, FrontCategory> doNotDegenerateFcs = new HashMap<>();
        if (!valueMap.isEmpty()) {
            Integer maxValue = Collections.max(valueMap.values());
            for (Map.Entry<Long, Integer> entry : valueMap.entrySet()) {
                if (maxValue.compareTo(entry.getValue()) == 0) {
                    remainFcs.put(entry.getKey(), fcMap.get(entry.getKey()));
                }
            }
            for (Map.Entry<Long, Integer> entry : valueMap.entrySet()) {
                Long fcId = entry.getKey();
                Integer value = entry.getValue();
                if (maxValue.compareTo(value) > 0) {
                    if (!Collections.disjoint(decedentMap.get(fcId), remainFcs.keySet())) {
                        doNotDegenerateFcs.put(fcId, fcMap.get(fcId));
                    }
                }
            }
        }

        Set<String> remainNames = new HashSet<>();
        for (FrontCategory fc : remainFcs.values()) {
            if (fc != null && fc.getName() != null) {
                String name = fixFcName(fc.getName());
                if (name.length() > 0) remainNames.add(name);
            }
        }
        Set<String> doNotDegenerateNames = new HashSet<>();
        for (FrontCategory fc : doNotDegenerateFcs.values()) {
            if (fc != null && fc.getName() != null) {
                String name = fixFcName(fc.getName());
                if (name.length() > 0) doNotDegenerateNames.add(name);
            }
        }
        // 未保留的退化成普通关键字
        keywords.addAll(FluentIterable.from(fcNames).filter(not(or(in(remainNames), in(doNotDegenerateNames)))).toSet());
        for (AnalyticalKeyword analyticalKeyword : analyticalKeywords) {
            if (AnalyticalKeyword.KeywordType.FRONT_CATEGORY.name().equalsIgnoreCase(analyticalKeyword.getType())) {
                String name = fixFcName(analyticalKeyword.getText());
                if (Strings.isNullOrEmpty(name) || (!remainNames.contains(name) && !doNotDegenerateNames.contains(name))) {
                    analyticalKeyword.setType(AnalyticalKeyword.KeywordType.PLAIN);
                } else if (doNotDegenerateNames.contains(name)) {
                    analyticalKeyword.setType(AnalyticalKeyword.KeywordType.FRONT_CATEGORY_REPLACED);
                }
            }
        }

        SearchQuery.QueryFrontCategory queryFrontCategory = new SearchQuery.QueryFrontCategory();
        Set<Long> backIds = new HashSet<>();
        Set<Long> leafFcIds = new HashSet<>();
        int maxLevel = 0;
        for (Map.Entry<Long, FrontCategory> entry : remainFcs.entrySet()) {
            backIds.addAll(findRelatedBcIds(entry.getKey(), leafFcIds));
            Integer level = entry.getValue().getLevel();
            maxLevel = Ints.max(maxLevel, level);
        }
        queryFrontCategory.setFrontIds(remainFcs.keySet());
        queryFrontCategory.setMaxLevel(maxLevel);
        queryFrontCategory.setBackIds(backIds);
        queryFrontCategory.setLeafFcIds(leafFcIds);
        return Optional.of(queryFrontCategory);
    }

    private Set<Long> findRelatedBcIds(long fcId, Set<Long> leafFcIds) {
        Set<Long> result = new HashSet<>();
        FrontCategory fc = frontCategoryDao.load(fcId);
        if (fc == null) {
            return Collections.emptySet();
        }
        if (fc.getHasChildren()) {
            List<FrontCategory> fcs = frontCategoryDao.findByPid(fcId);
            for (FrontCategory frontCategory : fcs) {
                result.addAll(findRelatedBcIds(frontCategory.getId(), leafFcIds));
            }
            return result;
        }

        List<CategoryBinding> bindings = categoryBindingDao.findByFrontCategoryId(fcId);
        for (CategoryBinding binding : bindings) {
            result.add(binding.getBackCategoryId());
            leafFcIds.add(binding.getFrontCategoryId());
        }

        return result;
    }

    @Override
    public Response<List<FrontCategory>> findFrontCategoriesByBackCategoryIds(List<Long> categoryIds, Set<Long> analyticalFcIds, int fcLevel) {
        try {
            List<FrontCategory> fcs = new ArrayList<>();
            for (Long categoryId : Iters.nullToEmpty(categoryIds)) {
                if (categoryId == null) continue;
                // TODO(Effet): to be optimized
                List<CategoryBinding> bindings = categoryBindingDao.findByBackCategoryId(categoryId);
                for (CategoryBinding binding : bindings) {
                    Long fcId = binding.getFrontCategoryId();
                    if (fcId == null || !analyticalFcIds.contains(fcId)) continue;
                    FrontCategory fc = findFcRelated(fcId, fcLevel + 1).orNull();
                    if (fc != null) {
                        fcs.add(fc);
                    }
                }
            }
            return Response.ok(fcs);
        } catch (Exception e) {
            log.error("find front categories by back categoryIds failed, categoryIds={}, fcLevel={}, cause:{}",
                    categoryIds, fcLevel, Throwables.getStackTraceAsString(e));
            return Response.fail("category.find.fail");
        }
    }

    /**
     * by jack
     * @param pageNo 页号
     * @param pageSize  分页大小
     * @param criteria  查询条件
     * @return
     */
    @Override
    public Response<Paging<BackCategoryBusinessDto>> pagingBackCategory(Integer pageNo, Integer pageSize, Map<String, Object> criteria) {
        Response<Paging<BackCategoryBusinessDto>> resp = new Response<Paging<BackCategoryBusinessDto>>();
        try {
            Paging<BackCategoryBusinessDto> pbcbd = new Paging<>();
            List<BackCategoryBusinessDto> bcbd = Lists.newArrayList();
            String start = (String) criteria.get("updatedFrom");
            String end = (String) criteria.get("updatedTo");
            criteria.putAll(DayRange.from(start, end).toMap("startAt", "endAt"));
            PageInfo page = new PageInfo(pageNo, pageSize);
            Map<String, Object> nonNullAndEmpty = Params.filterNullOrEmpty(criteria);
            Paging<BackCategory> backCategory = backCategoryDao.backCategoryPaging(page.getOffset(), page.getLimit(), nonNullAndEmpty);
            BackCategoryBusinessDto backCategoryBusinessDto = null;
            for (BackCategory back: backCategory.getData()){
                backCategoryBusinessDto = new BackCategoryBusinessDto();
                if (back != null){
                    backCategoryBusinessDto.setBackCategory(back);
                    if (back.getShopBusinessId() != null){
                        Response<ShopBusiness> shopBusinessResponse = shopReadService.findShopBusinessById(back.getShopBusinessId());
                        if (!shopBusinessResponse.isSuccess()){
                            log.warn("find shopBusiness is failed id=(), cause:{}", back.getShopBusinessId(), shopBusinessResponse.getError());
                        } else {
                            backCategoryBusinessDto.setShopBusiness(shopBusinessResponse.getResult());
                        }
                    }
                    Response<List<BackCategory>> listResponse = findAncestorsByBackCategoryId(back.getId());
                    if (! listResponse.isSuccess()){
                        log.warn("find back category path is faild id=(),cause:{}",back.getId(),listResponse.getError());
                    } else {
                        backCategoryBusinessDto.setBackCategoryList(listResponse.getResult());
                    }
                }
                bcbd.add(backCategoryBusinessDto);
            }
            pbcbd.setData(bcbd);
            pbcbd.setTotal(backCategory.getTotal());
            resp.setResult(pbcbd);
        } catch (Exception e){
            log.error("failed to paging backCategory (pageNo={}, pageSize={}, criteria={}), cause: {}",
                    pageNo, pageSize, criteria, Throwables.getStackTraceAsString(e));
            resp.setError("backCategory.find.fail");
        }
        return resp;
    }

    private Optional<FrontCategory> findFcRelated(long fcId, int fcLevel) {
        List<FrontCategory> ancestors = RespHelper.orServEx(findAncestorsByFrontCategoryIdWithCache(fcId));
        for (FrontCategory ancestor : ancestors) {
            if (Objects.equals(fcLevel, ancestor.getLevel())) {
                return Optional.of(ancestor);
            }
        }
        return Optional.absent();
    }
}
