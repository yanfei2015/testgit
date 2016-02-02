/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package io.terminus.parana.category.dao.redis;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import io.terminus.common.redis.dao.RedisBaseDao;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.parana.category.model.Spu;
import io.terminus.parana.category.model.SpuAttribute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;

import static io.terminus.common.redis.utils.KeyUtils.entityId;

/**
 * SPU属性Dao
 * Author: haolin
 * On: 8/31/14
 */
@Repository
public class SpuAttributeDao extends RedisBaseDao<SpuAttribute> {

    @Autowired
    public SpuAttributeDao(JedisTemplate template) {
        super(template);
    }

    /**
     * 通过id查询SPU属性
     * @param id SPU属性id
     * @return SPU属性
     */
    public SpuAttribute findById(Long id) {
        SpuAttribute spuAttribute = findByKey(id);
        return spuAttribute.getId() != null ? spuAttribute : null;
    }

    /**
     * 查询SPU的属性列表
     * @param spuId SPU.id
     * @return SPU属性列表
     */
    public List<SpuAttribute> findBySpuId(final Long spuId) {
        final List<String> ids = template.execute(new JedisTemplate.JedisAction<List<String>>() {
            @Override
            public List<String> action(Jedis jedis) {
                return jedis.lrange(Spu.keyOfSpuAttributes(spuId), 0, -1);
            }
        });
        return super.findByIds(ids);
    }

    /**
     * 计算SPU的属性个数
     * @param spuId SPU.id
     * @return SPU属性个数
     */
    public Integer countOf(final Long spuId) {
        return template.execute(new JedisTemplate.JedisAction<Integer>() {
            @Override
            public Integer action(Jedis jedis) {
                return jedis.llen(Spu.keyOfSpuAttributes(spuId)).intValue();
            }
        });
    }

    /**
     * 创建SPU属性
     * @param spuId SPU.id
     * @param spuAttributes SPU属性集合
     */
    public void create(final long spuId, final List<SpuAttribute> spuAttributes) {
        final List<SpuAttribute> origin = findBySpuId(spuId);
        for (SpuAttribute spuAttribute : spuAttributes) {
            spuAttribute.setId(newId());
        }
        template.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                Transaction t = jedis.multi();
                //remove old values first
                for (SpuAttribute spuAttribute : origin) {
                    t.del(entityId(SpuAttribute.class, spuAttribute.getId()));
                }
                String spuAttributesKey = Spu.keyOfSpuAttributes(spuId);
                t.del(spuAttributesKey);
                for (SpuAttribute spuAttribute : spuAttributes) {
                    Long id = spuAttribute.getId();
                    t.hmset(entityId(SpuAttribute.class, id), stringHashMapper.toHash(spuAttribute));
                    //add to spu index
                    t.rpush(spuAttributesKey, id.toString());
                }
                t.exec();
            }
        });
    }

    /**
     * 删除SPU属性
     * @param id SPU属性id
     */
    public void delete(final Long id) {
        final SpuAttribute spuAttribute = findById(id);
        if (spuAttribute == null) {
            return;
        }
        template.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                Transaction t = jedis.multi();
                t.del(entityId(SpuAttribute.class, id));
                //remove from spu index
                t.lrem(Spu.keyOfSpuAttributes(spuAttribute.getSpuId()), 1, id.toString());
                t.exec();
            }
        });
    }

    /**
     * 更新SPU属性
     * @param spuAttribute SPU属性
     */
    public void update(final SpuAttribute spuAttribute) {
        final Long id = spuAttribute.getId();
        final SpuAttribute original = findById(id);
        if (original == null) {
            return;
        }
        template.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                Transaction t = jedis.multi();
                t.hmset(entityId(SpuAttribute.class, id), stringHashMapper.toHash(spuAttribute));
                //update spu index if necessary
                if (spuAttribute.getSpuId() != null && !Objects.equal(spuAttribute.getSpuId(), original.getSpuId())) {
                    t.lrem(Spu.keyOfSpuAttributes(original.getSpuId()), 1, id.toString());
                    t.lpush(Spu.keyOfSpuAttributes(spuAttribute.getSpuId()), id.toString());
                }
                t.exec();
            }
        });
    }

    /**
     * 删除SPU的所有属性
     * @param spuId SPU.id
     */
    public void deleteBySpuId(final Long spuId) {
        final List<String> spuAttributes = template.execute(new JedisTemplate.JedisAction<List<String>>() {
            @Override
            public List<String> action(Jedis jedis) {
                return jedis.lrange(Spu.keyOfSpuAttributes(spuId), 0, -1);
            }
        });
        if (spuAttributes.isEmpty()) {
            return;
        }
        template.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                Transaction t = jedis.multi();
                t.del(Iterables.toArray(Iterables.transform(spuAttributes, new Function<String, String>() {
                    @Override
                    public String apply(String attributeId) {
                        return entityId(SpuAttribute.class, attributeId);
                    }
                }), String.class));
                t.del(Spu.keyOfSpuAttributes(spuId));
                t.exec();
            }
        });
    }
}
