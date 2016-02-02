/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package io.terminus.parana.item.dao.mysql;

import com.google.common.collect.ImmutableMap;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.Sku;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * SKU Dao实现
 * Author: haolin
 * On: 9/7/14
 */
@Repository
public class SkuDao extends MyBatisDao<Sku> {

    /**
     * 根据商品id列表查询Sku
     * @param itemIds 商品id列表
     * @return Sku列表
     */
    public List<Sku> findByItemIds(List<Long> itemIds){
        return getSqlSession().selectList(sqlId("findByItemIds"), itemIds);
    }

    /**
     * 根据商品id删除SKU
     * @param itemId 商品id
     * @return 删除记录数
     */
    public Integer deleteByItemId(Long itemId){
        return getSqlSession().delete(sqlId("deleteByItemId"), itemId);
    }

    /**
     * 根据itemId, attributeValue1, attributeValue2来唯一查询SKU
     * @param criteria 查询条件
     * @return SKU
     */
    public Sku findSkuByAttributeValuesAndItemId(Sku criteria){
        return getSqlSession().selectOne(sqlId("findSkuByAttributeValuesAndItemId"), criteria);
    }

    /**
     * 根据商品id查询SKU列表
     * @param itemId 商品id
     * @return SKU列表
     */
    public List<Sku> findByItemId(Long itemId) {
        return getSqlSession().selectList(sqlId("findByItemId"), itemId);
    }

    /**
     * 更新库存
     * @param skuId SKU.id
     * @param quantity 更新量, 可正可负
     * @return 更新记录数
     */
    public Integer updateStockQuantity(Long skuId, Integer quantity) {
        return getSqlSession().update("updateStockQuantity", ImmutableMap.of("skuId", skuId, "quantity", quantity));
    }

    /**
     * 商品状态变更
     *
     * @param itemIds 商品 ID 列表
     * @param status  变更后状态
     * @return 更新数量
     */
    public int setStatus(List<Long> itemIds, Item.Status status) {
        return getSqlSession().update(sqlId("setStatus"), ImmutableMap.of("itemIds", itemIds, "status", status));
    }
}
