/*
 * Copyright (c) 2015 杭州端点网络科技有限公司
 */

package io.terminus.parana.item.ext;

import io.terminus.parana.item.model.ItemType;
import org.springframework.stereotype.Component;

/**
 * 系统默认商品类型及分类
 *
 * <p>重载此 bean 可自定义商品类型和分类
 *
 * @author Effet
 */
@Component
public class EcpItemTypeSorter implements ItemTypeSorter {

    /**
     * 一般商品类型列表 (一般商品可能会有多种类型)
     */
    @Override
    public int[] ordinaryItemTypes() {
        return new int[] { ItemType.NORMAL.value() };
    }

    /**
     * 默认一般商品
     */
    @Override
    public int defaultOrdinaryItemType() {
        return ItemType.NORMAL.value();
    }
}
