/*
 *
 *  * Copyright (c) 2014 杭州端点网络科技有限公司
 *
 */

package io.terminus.parana.item.event;

import io.terminus.parana.common.event.EventDispatcher;
import org.springframework.stereotype.Component;

/**
 * 用户事件分发器
 * Author: haolin
 * On: 9/16/14
 */
@Component
public class ItemEventDispatcher extends EventDispatcher<ItemEventListener> {

    public ItemEventDispatcher() {
        super();
    }

    public ItemEventDispatcher(Integer threadCount) {
        super(threadCount);
    }
}
