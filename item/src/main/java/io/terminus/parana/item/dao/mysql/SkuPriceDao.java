/*
 * Copyright (c) 2015 杭州端点网络科技有限公司
 */

package io.terminus.parana.item.dao.mysql;

import com.google.common.collect.ImmutableMap;
import io.terminus.parana.common.util.Iters;
import io.terminus.parana.item.model.SkuPrice;
import org.mybatis.spring.support.SqlSessionDaoSupport;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Effet
 */
@Repository
public class SkuPriceDao extends SqlSessionDaoSupport {

    public Long create(SkuPrice price) {
        getSqlSession().insert("SkuPrice.create", price);
        return price.getId();
    }

    public boolean updateById(SkuPrice price) {
        return getSqlSession().update("SkuPrice.updateById", price) == 1;
    }

    public boolean updateBySkuId(SkuPrice price) {
        return getSqlSession().update("SkuPrice.updateBySkuId", price) == 1;
    }

    public boolean delete(Long id) {
        return getSqlSession().delete("SkuPrice.delete", id) == 1;
    }

    public SkuPrice findById(Long id) {
        return getSqlSession().selectOne("SkuPrice.findById", id);
    }

    public SkuPrice findBySkuIdWithLevel(Long skuId, Integer level) {
        return getSqlSession().selectOne("SkuPrice.findBySkuIdWithLevel",
                ImmutableMap.of("skuId", skuId, "level", level));
    }

    public List<SkuPrice> findBySkuId(Long skuId) {
        return getSqlSession().selectList("SkuPrice.findBySkuId", skuId);
    }

    public List<SkuPrice> findInSkuIds(List<Long> skuIds) {
        return getSqlSession().selectList("SkuPrice.findInSkuIds", checkNotNull(Iters.emptyToNull(skuIds)));
    }
}
