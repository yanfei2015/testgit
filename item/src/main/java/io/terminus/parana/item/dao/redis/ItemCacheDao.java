/*
 * Copyright (c) 2015 杭州端点网络科技有限公司
 */

package io.terminus.parana.item.dao.redis;

import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.common.util.Params;
import io.terminus.parana.item.dto.ItemFullDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;

/**
 * @author Effet
 */
@Repository
public class ItemCacheDao {

    @Autowired
    private JedisTemplate jedisTemplate;

    public ItemFullDetail getFullDetail(final long itemId) {
        final String key = keyOfFullDetail(itemId);
        return jedisTemplate.execute(new JedisTemplate.JedisAction<ItemFullDetail>() {
            @Override
            public ItemFullDetail action(Jedis jedis) {
                String json = jedis.get(key);
                ItemFullDetail fullDetail;
                if (json != null) {
                    json = Params.trimToNull(json);
                    fullDetail = json == null ? new ItemFullDetail(): readFullDetailFromJson(json);
                } else {
                    fullDetail = null;
                }
                // fullDetail may null
                return fullDetail;
            }
        });
    }

    protected ItemFullDetail readFullDetailFromJson(String json) {
        return JsonMapper.nonDefaultMapper().fromJson(json, ItemFullDetail.class);
    }

    public void setFullDetail(final long itemId, final ItemFullDetail fullDetail) {
        final String key = keyOfFullDetail(itemId);
        jedisTemplate.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                String json = fullDetail == null ? "" : JsonMapper.nonDefaultMapper().toJson(fullDetail);
                jedis.setex(key, 15 * 60, json);
            }
        });
    }

    public void invalidFullDetail(final long itemId) {
        jedisTemplate.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                jedis.del(keyOfFullDetail(itemId));
            }
        });
    }

    private String keyOfFullDetail(long itemId) {
        return "cache:item:" + itemId + ":full-detail";
    }
}
