/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package io.terminus.parana.item.dao.mysql;

import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.item.model.ShipFee;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 商品运费Dao
 * Author: haolin
 * On: 9/3/14
 */
@Repository
public class ShipFeeDao extends MyBatisDao<ShipFee> {

    /**
     * 根据商品id查询其运费信息
     * @param itemId 商品id
     * @return 运费信息
     */
    public ShipFee findByItemId(Long itemId) {
        return getSqlSession().selectOne(sqlId("findByItemId"), itemId);
    }

    public List<ShipFee> findByItemIds(Long... itemIds) {
        return getSqlSession().selectList(sqlId("findByItemIds"), itemIds);
    }

    /**
     * 删除商品运费信息
     * @param itemId 商品id
     * @return 删除记录数
     */
    public Integer deleteByItemId(Long itemId) {
        return getSqlSession().delete(sqlId("deleteByItemId"), itemId);
    }

    public Integer deleteByTemplateId(Long templateId) {
        return getSqlSession().delete(sqlId("deleteByTemplateId"), templateId);
    }
}
