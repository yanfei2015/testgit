/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package io.terminus.parana.category.dao.redis;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import io.terminus.common.redis.dao.RedisBaseDao;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.parana.category.model.AttributeKey;
import io.terminus.parana.category.model.BackCategory;
import io.terminus.parana.category.model.Spu;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;

import static io.terminus.common.redis.utils.KeyUtils.entityId;

/**
 * 属性键Dao实现
 * Author: haolin
 * On: 8/31/14
 */
@Repository @Slf4j
public class AttributeKeyDao extends RedisBaseDao<AttributeKey> {

    @Autowired
    public AttributeKeyDao(JedisTemplate template) {
        super(template);
    }

    /**
     * 通过id查询属性键
     * @param id 属性键id
     * @return 属性键
     */
    public AttributeKey findById(Long id) {
        AttributeKey attributeKey = findByKey(id);
        return attributeKey.getId() != null ? attributeKey : null;
    }

    /**
     * 通过名称查询属性键
     * @param name 属性键名称
     * @return 属性键
     */
    public AttributeKey findByName(final String name) {
        String id = template.execute(new JedisTemplate.JedisAction<String>() {
            @Override
            public String action(Jedis jedis) {
                return jedis.get(AttributeKey.keyOfAttributeKey(name));
            }
        });
        if (!Strings.isNullOrEmpty(id)) {
            return findById(Long.parseLong(id));
        }
        return null;
    }

    /**
     * 查询后台类目的所有属性键
     * @param categoryId 类目id
     * @return 对应的属性键列表
     */
    public List<AttributeKey> findByCategoryId(final Long categoryId) {
        final List<String> ids = template.execute(new JedisTemplate.JedisAction<List<String>>() {
            @Override
            public List<String> action(Jedis jedis) {
                return jedis.lrange(BackCategory.keyOfAttributeKeys(categoryId), 0, -1);
            }
        });
        return super.findByIds(ids);
    }

    /**
     * 查询SPU的所有SKU属性键
     * @param spuId SPU编号
     * @return SKU属性键列表
     */
    public List<AttributeKey> findSkuKeysBySpuId(final Long spuId) {
        final List<String> ids = template.execute(new JedisTemplate.JedisAction<List<String>>() {
            @Override
            public List<String> action(Jedis jedis) {
                return jedis.lrange(Spu.keyOfSkuKeys(spuId), 0, -1);
            }
        });
        return super.findByIds(ids);
    }

    /**
     * 创建属性键
     * @param attributeKey 属性键
     * @return 属性键id
     */
    public Long create(final AttributeKey attributeKey) {

        final String name = attributeKey.getName();
        //test name exists
        String existedId = template.execute(new JedisTemplate.JedisAction<String>() {
            @Override
            public String action(Jedis jedis) {
                return jedis.get((AttributeKey.keyOfAttributeKey(name)));
            }
        });
        if (!Strings.isNullOrEmpty(existedId)) {
            log.warn("attribute key {} has been existed,use existed key(id={}) ", name, existedId);
            attributeKey.setId(Long.parseLong(existedId));
            return Long.parseLong(existedId);
        }
        final Long id = newId();
        attributeKey.setId(id);
        template.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                Transaction t = jedis.multi();
                t.hmset(entityId(AttributeKey.class, id), stringHashMapper.toHash(attributeKey));
                //set name index
                t.set(AttributeKey.keyOfAttributeKey(name), id.toString());
                t.exec();
            }
        });
        return id;
    }

    /**
     * 删除属性键
     * @param id 属性键id
     */
    public void delete(final Long id) {
        final AttributeKey attributeKey = findById(id);
        if (attributeKey == null) {
            return;
        }
        template.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                Transaction t = jedis.multi();
                t.del(entityId(AttributeKey.class, id));
                //remove name index
                t.del(AttributeKey.keyOfAttributeKey(attributeKey.getName()));
                t.exec();
            }
        });
    }

    /**
     * 更新属性键
     * @param attributeKey 属性键
     */
    public void update(final AttributeKey attributeKey) {
        final AttributeKey old = findById(attributeKey.getId());
        if (old == null) {
            throw new IllegalStateException("attribute key not exist");
        }
        if (!Strings.isNullOrEmpty(attributeKey.getName())) {
            String oldId = template.execute(new JedisTemplate.JedisAction<String>() {
                @Override
                public String action(Jedis jedis) {
                    return jedis.get(entityId(AttributeKey.class, attributeKey.getId()));
                }
            });
            if (!Strings.isNullOrEmpty(oldId) && !Objects.equal(oldId, attributeKey.getId().toString())) { //not the same attribute key
                log.error("duplicated attribute key name:{}", attributeKey.getName());
                throw new IllegalArgumentException("duplicated attribute key name");
            }
        }
        template.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                Transaction t = jedis.multi();
                t.hmset(entityId(AttributeKey.class, attributeKey.getId()), stringHashMapper.toHash(attributeKey));
                //update name index if necessary
                if (!Strings.isNullOrEmpty(attributeKey.getName())) {
                    t.del(AttributeKey.keyOfAttributeKey(old.getName()));
                    t.set(AttributeKey.keyOfAttributeKey(attributeKey.getName()), attributeKey.getId().toString());
                }
                t.exec();
            }
        });
    }
}
