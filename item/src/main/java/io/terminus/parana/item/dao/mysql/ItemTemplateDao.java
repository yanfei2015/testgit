/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package io.terminus.parana.item.dao.mysql;

import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.item.model.ItemTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 商品模板Dao实现
 * Author: haolin
 * On: 9/7/14
 */
@Repository
public class ItemTemplateDao extends MyBatisDao<ItemTemplate> {

    /**
     * 通过SPU查询商品模板
     * @param spuId SPU.id
     * @return 商品模板
     */
    public ItemTemplate findBySpuId(Long spuId){
        return getSqlSession().selectOne(sqlId("findBySpuId"), spuId);
    }

    /**
     * 通过多个idSPU查询商品模板
     * @param spuIds SPU.id列表
     * @return 商品模板列表
     */
    public List<ItemTemplate> findBySpuIds(List<Long> spuIds) {
        return getSqlSession().selectOne(sqlId("findBySpuIds"), spuIds);
    }
}
