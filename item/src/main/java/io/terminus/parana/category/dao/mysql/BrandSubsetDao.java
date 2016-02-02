/*
 * Copyright (c) 2015 杭州端点网络科技有限公司
 */

package io.terminus.parana.category.dao.mysql;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import io.terminus.parana.category.model.BrandSubset;
import io.terminus.parana.common.util.Iters;
import org.mybatis.spring.support.SqlSessionDaoSupport;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Effet
 */
@Deprecated
@Repository
public class BrandSubsetDao extends SqlSessionDaoSupport {

    public Long create(BrandSubset brandSubset) {
        getSqlSession().insert("BrandSubset.create", checkNotNull(brandSubset, "brand subset null"));
        return checkNotNull(brandSubset.getId(), "exactly not create success");
    }

    public boolean delete(long id) {
        return getSqlSession().delete("BrandSubset.delete", id) == 1;
    }

    public Optional<BrandSubset> findById(long id) {
        BrandSubset bs = getSqlSession().selectOne("BrandSubset.findById", id);
        return Optional.fromNullable(bs);
    }

    public List<BrandSubset> findByBcId(long bcId) {
        List<BrandSubset> subset = getSqlSession().selectList("BrandSubset.findByBcId", bcId);
        return checkNotNull(subset, "at least list obj must not be null");
    }

    public List<BrandSubset> findByBcIds(List<Long> bcIds) {
        checkNotNull(Iters.emptyToNull(bcIds), "bcIds null, empty brand realted");
        List<BrandSubset> subset = getSqlSession().selectList("BrandSubset.findByBcIds", bcIds);
        return checkNotNull(subset, "at least list obj must not be null");
    }

    public Optional<BrandSubset> findByBcIdAndBrandId(long bcId, long brandId) {
        BrandSubset bs = getSqlSession().selectOne("BrandSubset.findByBcIdAndBrandId", ImmutableMap.of("bcId", bcId, "brandId", brandId));
        return Optional.fromNullable(bs);
    }
}
