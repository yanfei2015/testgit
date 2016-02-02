/*
 * Copyright (c) 2015 杭州端点网络科技有限公司
 */

package io.terminus.parana.item.dao.redis;

import io.terminus.common.redis.utils.JedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;

/**
 * @author Effet
 */
@Repository
public class ItemDescriptionRedisDao {

    @Autowired
    private JedisTemplate jedisTemplate;

    public void set(final long itemId, final String json) {
        jedisTemplate.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                jedis.set(keyByItemId(itemId), json);
            }
        });
    }

    public String get(final long itemId) {
        return jedisTemplate.execute(new JedisTemplate.JedisAction<String>() {
            @Override
            public String action(Jedis jedis) {
                return jedis.get(keyByItemId(itemId));
            }
        });
    }

    public void del(final long itemId) {
        jedisTemplate.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                jedis.del(keyByItemId(itemId));
            }
        });
    }

    public static String keyByItemId(final long itemId) {
        return "item:" + itemId + ":description";
    }
}
