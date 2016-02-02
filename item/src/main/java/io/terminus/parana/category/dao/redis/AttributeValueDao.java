/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package io.terminus.parana.category.dao.redis;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import io.terminus.common.redis.dao.RedisBaseDao;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.parana.category.model.AttributeValue;
import io.terminus.parana.category.model.BackCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;

import static io.terminus.common.redis.utils.KeyUtils.entityId;

/**
 * 属性值Dao实现
 * Author: haolin
 * On: 8/31/14
 */
@Repository @Slf4j
public class AttributeValueDao extends RedisBaseDao<AttributeValue> {

    @Autowired
    public AttributeValueDao(JedisTemplate template) {
        super(template);
    }

    /**
     * 通过id查询属性值
     * @param id 属性值id
     * @return 属性值
     */
    public AttributeValue findById(Long id) {
        AttributeValue attributeValue = findByKey(id);
        return attributeValue.getId() != null ? attributeValue : null;
    }

    /**
     * 通过属性的值查询属性值对象
     * @param value 属性的值
     * @return 属性值对象
     */
    public AttributeValue findByValue(final String value) {
        String id = template.execute(new JedisTemplate.JedisAction<String>() {
            @Override
            public String action(Jedis jedis) {
                return jedis.get(AttributeValue.keyOfAttributeValue(value));
            }
        });
        if (!Strings.isNullOrEmpty(id) && !Objects.equal(id, "null")) {
            return findById(Long.parseLong(id));
        }
        return null;
    }

    /**
     * 查询后台类目某属性键的属性值列表
     * @param categoryId 后台类目id
     * @param attributeKeyId 属性键id
     * @return 属性值列表
     */
    public List<AttributeValue> findByCategoryIdAndKeyId(final Long categoryId, final Long attributeKeyId) {
        final List<String> ids = template.execute(new JedisTemplate.JedisAction<List<String>>() {
            @Override
            public List<String> action(Jedis jedis) {
                return jedis.lrange(BackCategory.keyOfAttributeValues(categoryId, attributeKeyId), 0, -1);
            }
        });
        return super.findByIds(ids);
    }

    /**
     * 创建属性值
     * @param value 属性值的值
     * @return 属性值
     */
    public AttributeValue create(final String value) {
        final Long id = newId();
        final AttributeValue attributeValue = new AttributeValue();
        attributeValue.setId(id);
        attributeValue.setValue(value);

        template.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                Transaction t = jedis.multi();
                //persist value
                t.hmset(entityId(AttributeValue.class, id), stringHashMapper.toHash(attributeValue));
                //add index
                t.set(AttributeValue.keyOfAttributeValue(attributeValue.getValue()), id.toString());
                t.exec();
            }
        });
        return attributeValue;
    }

    /**
     * 删除属性值
     * @param id 属性值id
     */
    public void delete(final Long id) {
        final AttributeValue existed = findById(id);
        if (existed == null) {
            return;
        }
        template.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                Transaction t = jedis.multi();
                t.del(entityId(AttributeValue.class, id));
                //remove value index
                t.del(AttributeValue.keyOfAttributeValue(existed.getValue()));
                t.exec();
            }
        });
    }

    public void update(final AttributeValue attributeValue) {
        final AttributeValue old = findById(attributeValue.getId());
        if (old == null) {
            throw new IllegalStateException("attribute value not exist");
        }
        //update name index if necessary
        if (!Strings.isNullOrEmpty(attributeValue.getValue())) {
            String oldId = template.execute(new JedisTemplate.JedisAction<String>() {
                @Override
                public String action(Jedis jedis) {
                    return jedis.get(AttributeValue.keyOfAttributeValue(attributeValue.getValue()));
                }
            });
            if (!Strings.isNullOrEmpty(oldId) && !Objects.equal(oldId, attributeValue.getId().toString())) { //not the same attribute value
                log.error("duplicated attribute value data:{}", attributeValue.getValue());
                throw new IllegalArgumentException("duplicated attribute value data");
            }
        }

        template.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                Transaction t = jedis.multi();
                t.hmset(entityId(AttributeValue.class, attributeValue.getId()), stringHashMapper.toHash(attributeValue));
                //update name index if necessary
                if (!Strings.isNullOrEmpty(attributeValue.getValue())) {
                    t.del(AttributeValue.keyOfAttributeValue(old.getValue()));
                    t.set(AttributeValue.keyOfAttributeValue(attributeValue.getValue()), attributeValue.getId().toString());
                }
                t.exec();
            }
        });
    }
}
