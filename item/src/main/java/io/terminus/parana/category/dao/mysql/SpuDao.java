/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package io.terminus.parana.category.dao.mysql;

import com.google.common.collect.ImmutableMap;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.category.model.Spu;
import org.springframework.stereotype.Repository;

/**
 * SPU Dao实现
 * Author: haolin
 * On: 8/30/14
 */
@Repository
public class SpuDao extends MyBatisDao<Spu> {

    /**
     * 设置SPU状态
     * @param id SPU id
     * @param status 状态, 1启用, -1禁用
     * @return 更新记录数
     */
    public Integer setStatus(Long id, Integer status){
        return getSqlSession().update(sqlId("setStatus"), ImmutableMap.of("id", id, "status", status));
    }

    /**
     * 通过条件唯一查询SPU
     * @param criteria 查询条件
     * @return SPU
     */
    public Spu findBy(Spu criteria) {
        return getSqlSession().selectOne(sqlId("findBy"), criteria);
    }
}
