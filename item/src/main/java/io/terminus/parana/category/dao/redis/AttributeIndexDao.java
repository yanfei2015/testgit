/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package io.terminus.parana.category.dao.redis;

import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.parana.category.dto.AttributeDto;
import io.terminus.parana.category.model.BackCategory;
import io.terminus.parana.category.model.Spu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

/**
 * 属性索引Dao实现
 * Author: haolin
 * On: 8/31/14
 */
@Repository
public class AttributeIndexDao {

    @Autowired
    private JedisTemplate template;

    /**
     * 为后台类目添加属性键id
     * @param categoryId 后台类目id
     * @param attributeKeyId 属性键id
     */
    public void addCategoryAttributeKey(final Long categoryId, final Long attributeKeyId) {
        template.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                jedis.rpush(BackCategory.keyOfAttributeKeys(categoryId), attributeKeyId.toString());
            }
        });
    }

    /**
     * 移除后台类目某个属性键, 及其该属性键的所有属性值
     * @param categoryId 后台类目id
     * @param attributeKeyId 属性键id
     */
    public void removeCategoryAttributeKey(final Long categoryId, final Long attributeKeyId) {
        template.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                Transaction t = jedis.multi();
                t.lrem(BackCategory.keyOfAttributeKeys(categoryId), 1, attributeKeyId.toString());
                //remove associated attribute values of this categoryAttributeKey
                t.del(BackCategory.keyOfAttributeValues(categoryId, attributeKeyId));
                t.exec();
            }
        });
    }

    /**
     * 为后台类目某个属性键添加属性值
     * @param categoryId 后台类目id
     * @param attributeKeyId 属性键id
     * @param attributeValueId 属性值id
     */
    public void addCategoryAttributeValue(final Long categoryId, final Long attributeKeyId, final Long attributeValueId) {
        template.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                Transaction t = jedis.multi();
                //remove old if exist
                t.lrem(BackCategory.keyOfAttributeValues(categoryId, attributeKeyId), 1, attributeValueId.toString());
                t.rpush(BackCategory.keyOfAttributeValues(categoryId, attributeKeyId), attributeValueId.toString());
                t.exec();
            }
        });
    }

    /**
     * 移除后台类目某个属性键的某个属性值
     * @param categoryId 后台类目id
     * @param attributeKeyId 属性键id
     * @param attributeValueId 属性值id
     */
    public void removeCategoryAttributeValue(final Long categoryId, final Long attributeKeyId, final Long attributeValueId) {
        template.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                jedis.lrem(BackCategory.keyOfAttributeValues(categoryId, attributeKeyId), 1, attributeValueId.toString());
            }
        });
    }

    /**
     * 为SPU添加SKU属性
     * @param spuId SPU编号
     * @param skuKeys SKU键
     */
    public void addSkuKeys(final Long spuId, final Iterable<AttributeDto> skuKeys) {
        template.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                Transaction t = jedis.multi();
                //remove old values first
                String skuKeysKey = Spu.keyOfSkuKeys(spuId);
                t.del(skuKeysKey);
                for (AttributeDto skuKey : skuKeys) {
                    t.rpush(skuKeysKey, skuKey.getAttributeKeyId().toString());
                }
                t.exec();
            }
        });
    }
}
