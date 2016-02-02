/*
 *
 *  * Copyright (c) 2014 杭州端点网络科技有限公司
 *
 */

package io.terminus.parana.item.dao.redis;

import com.google.common.base.Optional;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.parana.common.util.Params;
import io.terminus.parana.item.model.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.util.List;
import java.util.Map;

/**
 * 商品RedisDao
 * Author: haolin
 * On: 11/4/14
 */
@Repository
public class ItemRedisDao {

    @Autowired
    private JedisTemplate jedisTemplate;

    /**
     * 设置店铺上架商品数
     * @param shopId 店铺id
     * @param count 上架商品数
     */
    public void setShopItemCount(final Long shopId, final Long count){
        jedisTemplate.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                jedis.set(Item.keyOfShopItemCount(shopId), String.valueOf(count));
            }
        });
    }

    /**
     * 批量设置店铺上架商品数
     * @param shopIds 店铺id列表
     * @param countMap 统计映射<shopId, itemCount>
     */
    public void setShopsItemCount(final List<Long> shopIds, final Map<Long, Long> countMap) {
        jedisTemplate.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                Pipeline p = jedis.pipelined();
                for (Long shopId : shopIds){
                    if (countMap.containsKey(shopId)){
                        p.set(Item.keyOfShopItemCount(shopId), String.valueOf(countMap.get(shopId)));
                    }
                }
                p.sync();
            }
        });
    }

    public void setItemDiscount(final long itemId, final int expireSec, final String data) {
        jedisTemplate.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                jedis.setex(keyOfItemDiscount(itemId), expireSec, data);
            }
        });
    }

    public void delItemDiscount(final long itemId) {
        jedisTemplate.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                jedis.del(keyOfItemDiscount(itemId));
            }
        });
    }

    public Optional<String> getItemDiscount(final long itemId) {
        String data = jedisTemplate.execute(new JedisTemplate.JedisAction<String>() {
            @Override
            public String action(Jedis jedis) {
                return jedis.get(keyOfItemDiscount(itemId));
            }
        });
        return Optional.fromNullable(Params.trimToNull(data));
    }

    private String keyOfItemDiscount(long itemId) {
        return "item:" + itemId + ":discount";
    }
}
