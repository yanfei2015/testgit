/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package io.terminus.parana.item.dao.mysql;

import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.item.model.ItemDetail;
import org.springframework.stereotype.Repository;

/**
 * Author: haolin
 * On: 9/7/14
 */
@Repository
public class ItemDetailDao extends MyBatisDao<ItemDetail> {

    /**
     * 根据商品id查询商品详情
     * @param itemId 商品编号
     * @return 商品详情
     */
    public ItemDetail findByItemId(Long itemId){
        return getSqlSession().selectOne(sqlId("findByItemId"), itemId);
    }

    /**
     * 删除商品的详情
     * @param itemId 商品id
     * @return 删除记录数
     */
    public Integer deleteByItemId(Long itemId) {
        return getSqlSession().delete(sqlId("deleteByItemId"), itemId);
    }
}
