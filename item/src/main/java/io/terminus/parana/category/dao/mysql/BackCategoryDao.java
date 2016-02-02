/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package io.terminus.parana.category.dao.mysql;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.terminus.common.model.Paging;
import io.terminus.parana.category.model.BackCategory;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 后台类目Dao实现
 * Author: haolin
 * On: 8/29/14
 */
@Repository
public class BackCategoryDao extends BaseCategoryDao<BackCategory> {

    /**
     * 批量设置行业
     *
     * @param businessId  行业 ID
     * @param categoryIds 类目列表
     * @return 更新数量
     */
    public int setBusiness(long businessId, List<Long> categoryIds) {
        return getSqlSession().update(sqlId("setBusiness"), ImmutableMap.of("businessId", businessId, "categoryIds", categoryIds));
    }

    /**
     * 设置费率
     * @param rate 费率
     * @param id 主键
     * @return 是否更新
     */
    public Boolean setRate(Integer rate, Long id) {
        return getSqlSession().update(sqlId("setRate"), ImmutableMap.of("rate", rate, "id", id))>0;
    }





    public List<BackCategory> findByBusiness(long businessId) {
        return getSqlSession().selectList(sqlId("findByBusiness"), ImmutableMap.of("businessId", businessId));
    }

    /**
     * by jack
     * @param offset
     * @param limit
     * @param criteria
     * @return
     */
    public Paging<BackCategory> backCategoryPaging(Integer offset,Integer limit,Map<String,Object> criteria){
        if (criteria == null){
            criteria = Maps.newHashMap();
        }
        Long total = (Long)this.getSqlSession().selectOne(this.sqlId("count"),criteria);
        if (total.longValue() <= 0L){
            return new Paging(Long.valueOf(0L), Collections.emptyList());
        }  else {
            ((Map)criteria).put("offset",offset);
            ((Map)criteria).put("limit",limit);
            List datas = this.getSqlSession().selectList(this.sqlId("backCategoryPaging"),criteria);
            return new Paging(total,datas);
        }
    }
}
