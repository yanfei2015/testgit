/*
 * Copyright (c) 2015 杭州端点网络科技有限公司
 */

package io.terminus.parana.item.internal;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.collect.Ordering;
import io.terminus.parana.common.util.Iters;
import io.terminus.parana.item.model.SkuPrice;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Effet
 */
@Slf4j
public final class SkuPriceSelector {

    /**
     * 选择阶梯价
     *
     * @param prices   阶梯价列表
     * @param mode     查询模式
     * @param expectLv 期望的阶梯, NULL 等同于 0
     * @return 阶梯价
     */
    public static Optional<Integer> select(
            List<SkuPrice> prices, SkuPrice.MODE mode, @Nullable Integer expectLv) {
        // params check
        if (Iters.emptyToNull(prices) == null || mode == null) {
            return Optional.absent();
        }

        // {lv => price}
        Map<Integer, Integer> lvMap = new HashMap<>();
        for (SkuPrice price : prices) {
            if (price.getLv() != null && price.getPrice() != null) {
                lvMap.put(price.getLv(), price.getPrice());
            }
        }

        if (lvMap.isEmpty()) {
            return Optional.absent();
        }

        return innerSelect(lvMap, mode, MoreObjects.firstNonNull(expectLv, 0));
    }

    private static Optional<Integer> innerSelect(Map<Integer, Integer> lvMap, SkuPrice.MODE mode, int lv) {
        switch (mode) {
            case EXACT:
                return Optional.fromNullable(lvMap.get(lv));
            case LOWER:
                for ( int i=lv; i>=0; --i) {
                    Integer p = lvMap.get(i);
                    if (p != null) {
                        return Optional.of(p);
                    }
                }
                return Optional.absent();
            case HIGHEST:
                Integer price = lvMap.get(lv);
                if (price == null) {
                    price = lvMap.get(0);
                }
                return Optional.fromNullable(price);
            default:
                log.warn("no support mode:{}", mode);
                return Optional.absent();
        }
    }
}
