/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package io.terminus.parana.item.dao.redis;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import io.terminus.common.redis.dao.RedisBaseDao;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.item.model.SkuAttribute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;

import static io.terminus.common.redis.utils.KeyUtils.entityId;
/**
 * Author: haolin
 * On: 9/7/14
 */
@Repository
public class SkuAttributeDao extends RedisBaseDao<SkuAttribute> {

    @Autowired
    public SkuAttributeDao(JedisTemplate jedisTemplate) {
        super(jedisTemplate);
    }

    /**
     * 创建SKU属性
     * @param skuAttribute
     */
    public void create(final SkuAttribute skuAttribute) {
        final Long id = newId();
        skuAttribute.setId(id);
        template.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                Transaction t = jedis.multi();
                t.hmset(entityId(SkuAttribute.class, id), stringHashMapper.toHash(skuAttribute));
                //add to sku index
                t.rpush(Sku.keyOfAttributes(skuAttribute.getSkuId()), id.toString());
                t.exec();
            }
        });
    }

    /**
     * 通过id查询SKU属性对象
     * @param id SKU.id
     * @return SKU属性对象
     */
    public SkuAttribute findById(final long id) {
        SkuAttribute skuAttribute = findByKey(id);
        return skuAttribute.getId() != null ? skuAttribute : null;
    }

    /**
     * 删除SKU属性
     * @param id SKU属性id
     */
    public void delete(final Long id) {
        final SkuAttribute skuAttribute = findById(id);
        if (skuAttribute == null) {
            return;
        }
        template.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                Transaction t = jedis.multi();
                t.del(entityId(SkuAttribute.class, id));
                //remove from spu index
                t.lrem(Sku.keyOfAttributes(skuAttribute.getSkuId()), 1, id.toString());
                t.exec();
            }
        });
    }

    /**
     * 根据Sku.id查询其属性列表
     * @param skuId SKU.id
     * @return SKU属性列表
     */
    public List<SkuAttribute> findBySkuId(final Long skuId) {
        final List<String> ids = template.execute(new JedisTemplate.JedisAction<List<String>>() {
            @Override
            public List<String> action(Jedis jedis) {
                return jedis.lrange(Sku.keyOfAttributes(skuId), 0, -1);
            }
        });
        return super.findByIds(ids);
    }

    /**
     * 根据SKU.id删除其SKU属性
     * @param skuId SKU.id
     */
    public void deleteBySkuId(final Long skuId) {
        final List<String> spuAttributes = template.execute(new JedisTemplate.JedisAction<List<String>>() {
            @Override
            public List<String> action(Jedis jedis) {
                return jedis.lrange(Sku.keyOfAttributes(skuId), 0, -1);
            }
        });
        template.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                Transaction t = jedis.multi();
                t.del(Iterables.toArray(Iterables.transform(spuAttributes, new Function<String, String>() {
                    @Override
                    public String apply(String attributeId) {
                        return entityId(SkuAttribute.class, attributeId);
                    }
                }), String.class));
                t.del(Sku.keyOfAttributes(skuId));
                t.exec();
            }
        });
    }
}
