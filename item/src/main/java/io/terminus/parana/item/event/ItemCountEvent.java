/*
 *
 *  * Copyright (c) 2014 杭州端点网络科技有限公司
 *
 */

package io.terminus.parana.item.event;

import io.terminus.parana.common.event.Event;

import java.util.List;

/**
 * 商品计数事件<店铺id列表>
 * Author: haolin
 * On: 11/4/14
 */
public class ItemCountEvent extends Event<List<Long>> {

    public ItemCountEvent(List<Long> data) {
        super(data);
    }

    public ItemCountEvent() {
    }
}
