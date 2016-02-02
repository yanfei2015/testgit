/*
 *
 *  * Copyright (c) 2014 杭州端点网络科技有限公司
 *
 */

package io.terminus.parana.item.event;

import com.google.common.eventbus.Subscribe;
import io.terminus.pampas.common.Response;
import io.terminus.parana.common.event.EventListener;
import io.terminus.parana.common.util.Iters;
import io.terminus.parana.event.item.ItemEvent;
import io.terminus.parana.item.dao.redis.ItemCacheDao;
import io.terminus.parana.item.service.ItemWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Author: haolin
 * On: 11/4/14
 */
@Component @Slf4j
public class ItemEventListener implements EventListener {

    @Autowired
    private ItemWriteService itemWriteService;

    @Autowired
    private ItemCacheDao itemCacheDao;

    /**
     * 商品计数事件
     * @param e 商品计数事件
     */
    @Subscribe
    public void onItemCountEvent(ItemCountEvent e){
        log.info("recount shop's on shelf item's count");
        Response<Boolean> resp;
        if (e.getData().size() <= 1){
            resp = itemWriteService.updateShopOnShelfItemCount(e.getData().get(0));
        } else {
            resp = itemWriteService.updateShopsOnShelfItemCount(e.getData());
        }
        if (!resp.isSuccess()){
            log.warn("failed to recount shop's on shelf item's count, ItemCountEvent[{}], cause: {}", e, resp.getError());
        }
    }

    @Subscribe
    public void onItemChanges(ItemEvent itemEvent) {
        for (Long itemId : Iters.nullToEmpty(itemEvent.ids())) {
            itemCacheDao.invalidFullDetail(itemId);
        }
    }
}
