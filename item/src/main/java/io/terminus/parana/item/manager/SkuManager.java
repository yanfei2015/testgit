/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package io.terminus.parana.item.manager;

import io.terminus.parana.item.dao.mysql.ItemDao;
import io.terminus.parana.item.dao.mysql.SkuDao;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.Sku;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Sku管理
 * Author: haolin
 * On: 9/7/14
 */
@Component
public class SkuManager {

    @Autowired
    private SkuDao skuDao;

    @Autowired
    private ItemDao itemDao;

    @Transactional
    public void creates(Item item, List<Sku> skus) {
        itemDao.update(item);
        for (Sku sku : skus) {
            sku.setShopId(item.getShopId());
            skuDao.create(sku);
        }
    }

    /**
     * 批量更新SKU
     * @param skus SKU列表
     */
    @Transactional
    public void updates(Item item, List<Sku> skus) {
        itemDao.update(item);
        for (Sku sku : skus){
            skuDao.update(sku);
        }
    }
}
