/*
 * Copyright (c) 2015 杭州端点网络科技有限公司
 */

package io.terminus.parana.category.service;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.common.utils.JsonMapper;
import io.terminus.pampas.common.Response;
import io.terminus.parana.category.internal.BackendPerms;
import io.terminus.parana.common.util.Params;
import io.terminus.parana.common.util.RespHelper;
import io.terminus.parana.item.dao.mysql.BrandDao;
import io.terminus.parana.item.model.Brand;
import io.terminus.parana.user.dto.LoginUser;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Effet
 */
@Slf4j
@Service
public class BrandReadServiceImpl implements BrandReadService {

    @Autowired
    private BrandDao brandDao;

    @Autowired
    private UserReadService userReadService;

    @Autowired
    private BackendPerms backendPerms;

    @Autowired
    private JedisTemplate jedisTemplate;

    @Override
    public Response<List<Brand>> findAllBrands() {
        try {
            return Response.ok(brandDao.listAll());
        } catch (Exception e) {
            log.error("find all brands failed, cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("brand.find.fail");
        }
    }

    @Override
    public Response<List<Brand>> findAllBrandsWithCache() {
        try {
            Optional<Brand[]> cache = readAllBrand();
            if (cache.isPresent()) {
                return Response.ok(Arrays.asList(cache.get()));
            }
            Response<List<Brand>> resp = findAllBrands();
            if (resp.isSuccess()) {
                putAllBrand(FluentIterable.from(resp.getResult()).toArray(Brand.class));
            }
            return resp;
        } catch (Exception e) {
            log.error("find all brands with cache failed, cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("brand.find.fail");
        }
    }

    @Override
    public Response<List<Brand>> findAllBrandsWithPerm(long userId) {
        try {
            User user = RespHelper.orServEx(userReadService.findById(userId));
            return Response.ok(filterWithPerm(RespHelper.orServEx(findAllBrands()), user.getId(), user.getType()));
        } catch (ServiceException e) {
            log.warn("find all brands with perm failed, userId={}, error:{}", userId, e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("find all brands with perm failed, userId={}, cause:{}", userId, Throwables.getStackTraceAsString(e));
            return Response.fail("brand.find.fail");
        }
    }

    @Override
    public Response<List<Brand>> findAllBrandsWithPerm(Optional<LoginUser> loginUser) {
        try {
            if (!loginUser.isPresent()) {
                return Response.ok(Collections.<Brand>emptyList());
            }
            return Response.ok(filterWithPerm(RespHelper.orServEx(findAllBrands()), loginUser.get().getId(), loginUser.get().getType()));
        } catch (ServiceException e) {
            log.warn("find all brands with perm failed, loginUser={}, error:{}", loginUser, e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("find all brands with perm failed, loginUser={}, cause:{}", loginUser, Throwables.getStackTraceAsString(e));
            return Response.fail("brand.find.fail");
        }
    }

    @Override
    public Response<List<Brand>> findAllBrandsWithCacheAndPerm(long userId) {
        try {
            User user = RespHelper.orServEx(userReadService.findById(userId));
            return Response.ok(filterWithPerm(RespHelper.orServEx(findAllBrandsWithCache()), user.getId(), user.getType()));
        } catch (ServiceException e) {
            log.warn("find all brands with cache and perm failed, userId={}, error:{}", userId, e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("find all brands with cache and perm failed, userId={}, cause:{}",
                    userId, Throwables.getStackTraceAsString(e));
            return Response.fail("brand.find.fail");
        }
    }

    @Override
    public Response<List<Brand>> findAllBrandsWithCacheAndPerm(Optional<LoginUser> loginUser) {
        try {
            if (!loginUser.isPresent()) {
                return Response.ok(Collections.<Brand>emptyList());
            }
            Response<List<Brand>> resp = findAllBrandsWithCache();
            if (!resp.isSuccess()) {
                return resp;
            }
            return Response.ok(filterWithPerm(resp.getResult(), loginUser.get().getId(), loginUser.get().getType()));
        } catch (Exception e) {
            log.error("find all brands with cache and perm failed, loginUser={}, cause:{}",
                    loginUser, Throwables.getStackTraceAsString(e));
            return Response.fail("brand.find.fail");
        }
    }

    private List<Brand> filterWithPerm(List<Brand> brands, Long userId, Integer userType) {
        if (userId == null || userType == null) {
            return Collections.emptyList();
        }
        if (User.TYPE.ADMIN == User.TYPE.fromNumber(userType) || User.TYPE.OPERATOR == User.TYPE.fromNumber(userType)) {
            return brands;
        }
        final BackendPerms.Perm perm = backendPerms.buildPerm(userId, userType);
        return FluentIterable.from(brands).filter(new Predicate<Brand>() {
            @Override
            public boolean apply(@Nullable Brand input) {
                return input != null && perm.checkBrandPerm(input.getId());
            }
        }).toList();
    }

    @Override
    public Response<List<Brand>> suggest(String q, @Nullable Integer limit) {
        if (q == null || q.length() < 1)
            return Response.ok(Collections.<Brand>emptyList());
        try {
            if (limit == null || limit <= 0) {
                limit = 10;
            }
            if (limit > 20) {
                limit = 20;
            }
            // TODO(Effet): 改成缓存方式
            return Response.ok(brandDao.findByFuzzyName(q, limit));
        } catch (ServiceException e) {
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("suggest brands failed, q={}, limit={}, cause:{}",
                    q, limit, Throwables.getStackTraceAsString(e));
            return Response.fail("brand.find.fail");
        }
    }

    @Override
    public Response<Brand> getBrandByName(String brandName) {
        return RespHelper.unwrap(getBrandByNameAllowNotFound(brandName), "brand.not.exist");
    }

    @Override
    public Response<Optional<Brand>> getBrandByNameAllowNotFound(String brandName) {
        try {
            brandName = Params.trimToNull(brandName);
            if (brandName == null) {
                return Response.fail("brand.name.invalid");
            }
            return Response.ok(Optional.fromNullable(brandDao.findByName(brandName)));
        } catch (Exception e) {
            log.error("get brand by name allow not found failed, brandName={}, cause:{}",
                    brandName, Throwables.getStackTraceAsString(e));
            return Response.fail("brand.find.fail");
        }
    }

    @Override
    public Response<Brand> getBrandByNameWithPerm(long userId, String brandName) {
        try {
            Response<Brand> resp = getBrandByName(brandName);
            if (resp.isSuccess()) {
                Response<User> userResp = userReadService.findById(userId);
                if (!userResp.isSuccess()) {
                    return Response.fail(userResp.getError());
                }
                final BackendPerms.Perm perm = backendPerms.buildPerm(userId, userResp.getResult().getType());
                if (!perm.checkBrandPerm(resp.getResult().getId())) {
                    return Response.fail("brand.no.perm");
                }
            }
            return resp;
        } catch (Exception e) {
            log.error("get brand by name with perm failed, userId={}, brandName={}, cause:{}",
                    userId, brandName, Throwables.getStackTraceAsString(e));
            return Response.fail("brand.find.fail");
        }
    }

    @Override
    public Response<Boolean> checkBrandPermByName(long userId, String brandName) {
        try {
            if (Strings.isNullOrEmpty(brandName)) {
                return Response.fail("brand.name.blank");
            }
            Brand brand = brandDao.findByName(brandName);
            if (brand == null) {
                return Response.fail("brand.not.exist");
            }
            User user = RespHelper.orServEx(userReadService.findById(userId));
            BackendPerms.Perm perm = backendPerms.buildPerm(userId, user.getType());
            return Response.ok(perm.checkBrandPerm(brand.getId()));
        } catch (Exception e) {
            log.error("check brand perm by name failed, userId={}, brandName={}, cause:{}",
                    userId, brandName, Throwables.getStackTraceAsString(e));
            return Response.fail("brand.perm.find.fail");
        }
    }

    @Override
    public Response<Brand> findById(long id) {
        try {
            Brand brand = brandDao.load(id);
            if (brand == null) {
                return Response.fail("brand.not.exist");
            }
            return Response.ok(brand);
        } catch (Exception e) {
            log.error("find brand by id failed, id={}, cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("brand.find.fail");
        }
    }

    @Override
    public Response<Brand> findByIdWithCache(long id) {
        try {
            Optional<Brand> cache = readBrand(id);
            if (cache.isPresent()) {
                return Response.ok(cache.get());
            }
            Response<Brand> resp = findById(id);
            if (resp.isSuccess()) {
                putBrand(id, resp.getResult());
            }
            return resp;
        } catch (Exception e) {
            log.error("find brand by id with cache failed, id={}, cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("brand.find.fail");
        }
    }

    private Optional<Brand[]> readAllBrand() {
        String data = jedisTemplate.execute(new JedisTemplate.JedisAction<String>() {
            @Override
            public String action(Jedis jedis) {
                return jedis.get(keyOfAllBrand());
            }
        });
        if (data == null) {
            return Optional.absent();
        }
        return Optional.of(JsonMapper.nonDefaultMapper().fromJson(data, Brand[].class));
    }

    private void putAllBrand(final Brand[] brands) {
        jedisTemplate.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                jedis.setex(keyOfAllBrand(), 600, JsonMapper.nonDefaultMapper().toJson(brands));
            }
        });
    }

    private Optional<Brand> readBrand(final long id) {
        String data = jedisTemplate.execute(new JedisTemplate.JedisAction<String>() {
            @Override
            public String action(Jedis jedis) {
                return jedis.get(keyOfBrand(id));
            }
        });
        if (data == null) {
            return Optional.absent();
        }
        return Optional.of(JsonMapper.nonDefaultMapper().fromJson(data, Brand.class));
    }

    private void putBrand(final long id, final Brand b) {
        jedisTemplate.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                jedis.setex(keyOfBrand(id), 600, JsonMapper.nonDefaultMapper().toJson(b));
            }
        });
    }

    private static String keyOfBrand(long brandId) {
        return "cache:brand:" + brandId;
    }

    private static String keyOfAllBrand() {
        return "cache:brand:all";
    }
}
