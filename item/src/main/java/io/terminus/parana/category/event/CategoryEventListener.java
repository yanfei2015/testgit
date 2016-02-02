/*
 * Copyright (c) 2015 杭州端点网络科技有限公司
 */

package io.terminus.parana.category.event;

import com.google.common.eventbus.Subscribe;
import io.terminus.parana.category.service.CategoryWriteService;
import io.terminus.parana.common.event.EventListener;
import io.terminus.parana.event.category.BackCategoryEvent;
import io.terminus.parana.event.category.FrontCategoryEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Effet
 */
@Slf4j
@Component
public class CategoryEventListener implements EventListener {

    @Autowired
    private CategoryWriteService categoryWriteService;

    @Subscribe
    public void onBackendChanges(BackCategoryEvent event) {
        categoryWriteService.clearBackend();
    }

    @Subscribe
    public void onFrontendChanges(FrontCategoryEvent event) {
        categoryWriteService.clearFrontend();
    }
}
