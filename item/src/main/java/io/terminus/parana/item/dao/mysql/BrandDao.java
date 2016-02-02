/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package io.terminus.parana.item.dao.mysql;

import com.google.common.collect.ImmutableMap;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.item.model.Brand;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 品牌Dao实现
 * Author: haolin
 * On: 9/3/14
 */
@Repository
public class BrandDao extends MyBatisDao<Brand> {

    /**
     * 通过名称唯一查询品牌
     * @param name 品牌名称
     * @return 品牌
     */
    public Brand findByName(String name) {
        return getSqlSession().selectOne(sqlId("findByName"), name);
    }

    /**
     * 模糊查询
     *
     * @param name  模糊名, null时不模糊查询
     * @param limit 条数限制
     * @return 品牌列表
     */
    public List<Brand> findByFuzzyName(@Nullable String name, int limit) {
        ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
        if (name != null) {
            builder.put("name", name);
        }
        builder.put("limit", limit);
        return getSqlSession().selectList(sqlId("findByFuzzyName"), builder.build());
    }

    /**
     * 每日新增品牌
     * @param startAt 开始时间
     * @param endAt 结束时间
     * @return 每日新增品牌数
     */
    public Integer dailyBrandIncrement(String startAt, String endAt) {
        return getSqlSession().selectOne(sqlId("dailyBrandIncrement"), ImmutableMap.of(
                "startAt", startAt, "endAt", endAt
        ));
    }
}
