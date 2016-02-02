package io.terminus.parana.item.dao.mysql;

import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.item.model.SkuExtra;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * SKU额外信息Dao
 * Author: haolin
 * On: 12/23/14
 */
@Repository
public class SkuExtraDao extends MyBatisDao<SkuExtra> {

    /**
     * 查询SKU的额外信息
     * @param skuId SKU ID
     * @return SKU额外信息
     */
    public SkuExtra findBySkuId(Long skuId){
        return getSqlSession().selectOne(sqlId("findBySkuId"), skuId);
    }

    /**
     * 查询SKU的额外信息
     * @param skuIds sku id 列表
     * @return SKU额外信息列表
     */
    public List<SkuExtra> findBySkuIds(List<Long> skuIds) {
        return getSqlSession().selectList(sqlId("findBySkuIds"), skuIds);
    }

    /**
     * 查询商品的SKU额外信息
     * @param itemId 商品ID
     * @return 商品的SKU额外信息
     */
    public List<SkuExtra> findByItemId(Long itemId){
        return getSqlSession().selectList(sqlId("findByItemId"), itemId);
    }

}
