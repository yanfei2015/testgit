/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package io.terminus.parana.item.dao.redis;

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import io.terminus.common.model.Paging;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.common.redis.utils.RedisClient;
import io.terminus.parana.item.model.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 店铺内商品标签Dao
 * Author: haolin
 * On: 8/28/14
 */
@Repository
public class ItemTagDao {

    @Autowired
    private JedisTemplate jedisTemplate;

    /**
     * 分页查询某标签下的商品id列表
     * @param userId 卖家id
     * @param tag 标签名称
     * @param offset 起始偏移
     * @param limit 分页大小
     * @return 某标签下的分页商品id列表
     */
    public Paging<Long> findItemsByTag2paging(final Long userId, final String tag,
                                   final int offset, final int limit) {
        String tagItemsKey = Item.keyOfTagItems(userId, tag);
        return RedisClient.listPaging(jedisTemplate, tagItemsKey, offset, limit);
    }

    /**
     * 查询多个商品的标签列表
     * @param itemIds 商品id列表
     * @return 商品及其标签列表
     */
    public ListMultimap<Long, String> findTagsByItemIds(final List<Long> itemIds) {
        return jedisTemplate.execute(new JedisTemplate.JedisAction<ListMultimap<Long, String>>() {
            @Override
            public ListMultimap<Long, String> action(Jedis jedis) {
                Pipeline p = jedis.pipelined();
                List<Response<Set<String>>> all = Lists.newArrayListWithCapacity(itemIds.size());
                for (Long itemId : itemIds) {
                    all.add(p.smembers(Item.keyOfItemTags(itemId)));
                }
                p.sync();
                ListMultimap<Long, String> result = ArrayListMultimap.create(itemIds.size(), 3);
                for (int i = 0; i < itemIds.size(); i++) {
                    result.putAll(itemIds.get(i), all.get(i).get());
                }
                return result;
            }
        });
    }

    /**
     * 获取单个商品的标签集合
     * @param itemId 商品id
     * @return 商品的标签集合
     */
    public Set<String> findTagsByItemId(final Long itemId) {
        return jedisTemplate.execute(new JedisTemplate.JedisAction<Set<String>>() {
            @Override
            public Set<String> action(Jedis jedis) {
                return jedis.smembers(Item.keyOfItemTags(itemId));
            }
        });
    }

    /**
     * 获取未打标签的商品分页id
     * @param userId 卖家id
     * @param offset 起始偏移
     * @param limit 分页大小
     * @return 未打标签的商品分页id
     */
    public Paging<Long> findUnKnownItems(final Long userId, final Integer offset, final Integer limit) {
        String unKnownItemsKey = Item.keyOfNoTagItems(userId);
        return RedisClient.listPaging(jedisTemplate, unKnownItemsKey, offset, limit);
    }

    /**
     * 根据标签查询商品
     * @param userId 用户id
     * @param tag 标签名称
     * @param offset 起始偏移
     * @param limit 分页大小
     * @return 商品id集合
     */
    public Set<String> findItemsByTag2set(final Long userId, final String tag, final Integer offset, final Integer limit) {
        return jedisTemplate.execute(new JedisTemplate.JedisAction<Set<String>>() {
            @Override
            public Set<String> action(Jedis jedis) {
                return jedis.zrange(Item.keyOfTagItems(userId, tag), offset, offset + limit - 1);
            }
        });
    }

    /**
     * 为商品添加标签
     * @param userId 用户id
     * @param tags 标签列表
     * @param itemIds 商品id列表
     */
    public void addTags2Items(final Long userId, final List<String> tags, final List<Long> itemIds) {
        jedisTemplate.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                Transaction t = jedis.multi();
                String[] itemIdStrings = new String[itemIds.size()];
                for (int i = 0; i < itemIds.size(); i++) {
                    Long itemId = itemIds.get(i);
                    itemIdStrings[i] = String.valueOf(itemId);
                    for (String tag : tags) {
                        // 将商品id挂到tag下面, 并按照itemId从小到大排序
                        t.zadd(Item.keyOfTagItems(userId, tag), itemId, String.valueOf(itemId));
                        // 将tag加入item的tag集合
                        t.sadd(Item.keyOfItemTags(itemId), tag);
                    }
                }
                //将商品从未分类中移除
                if (!itemIds.isEmpty()) {
                    t.zrem(Item.keyOfNoTagItems(userId), itemIdStrings);
                }
                t.exec();
            }
        });
    }

    /**
     * 移除单个商品单个标签
     * @param userId 用户id
     * @param itemId 商品id
     * @param tag 标签名称
     */
    public void removeItemTag(final Long userId, final Long itemId, final String tag) {
        jedisTemplate.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                Transaction t = jedis.multi();
                //将商品id从tag下面解除关联
                t.zrem(Item.keyOfTagItems(userId, tag), itemId.toString());
                //将tag从item的tag集合中删除
                String itemTagsKey = Item.keyOfItemTags(itemId);
                t.srem(itemTagsKey, tag);
                t.exec();

                //若商品没有标签了, 则放入未分类标签
                if (jedis.scard(itemTagsKey) <= 0L){
                    addItem2unKnownTag(userId, itemId);
                }
            }
        });
    }

    /**
     * 移除多个商品多个标签
     * @param userId 用户id
     * @param tags 标签列表
     * @param itemIds 商品id列表
     */
    public void removeTagsOfItems(final Long userId, final List<String> tags, final List<Long> itemIds) {
        jedisTemplate.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                Map<Long, String> idKeys = FluentIterable.from(itemIds).toMap(new Function<Long, String>() {
                    @Nullable
                    @Override
                    public String apply(@Nullable Long input) {
                        return Item.keyOfItemTags(input);
                    }
                });
                jedis.watch(idKeys.values().toArray(new String[idKeys.values().size()]));

                Set<Long> idUnknown = new HashSet<>();
                for (Map.Entry<Long, String> entry : idKeys.entrySet()) {
                    Set<String> existTags = jedis.smembers(entry.getValue());
                    if (existTags.isEmpty() || tags.containsAll(existTags)) {
                        idUnknown.add(entry.getKey());
                    }
                }

                Transaction t = jedis.multi();
                String unKnownTagKey = Item.keyOfNoTagItems(userId);
                for (Long itemId : itemIds) {
                    for (String tag : tags) {
                        //将商品id从tag下面解除关联
                        t.zrem(Item.keyOfTagItems(userId, tag), String.valueOf(itemId));
                        //将tag从item的tag集合中删除
                        t.srem(idKeys.get(itemId), tag);
                        //若商品没有标签了, 则放入未分类标签
                        if (idUnknown.contains(itemId)) {
                            t.zadd(unKnownTagKey, itemId, String.valueOf(itemId));
                        }
                    }
                }
                t.exec();
            }
        });
    }

    /**
     * 将商品放入未分类标签
     * @param userId 用户id
     * @param itemId 商品id
     * @return 放入成功返回true, 反之false
     */
    public Boolean addItem2unKnownTag(final Long userId, final Long itemId) {
        return jedisTemplate.execute(new JedisTemplate.JedisAction<Boolean>() {
            @Override
            public Boolean action(Jedis jedis) {
                return jedis.zadd(Item.keyOfNoTagItems(userId), itemId, String.valueOf(itemId)) > 0;
            }
        });
    }

    /**
     * 将商品从未分类标签中删除
     * @param userId 用户id
     * @param itemId 商品id
     * @return 删除成功返回true, 反之false
     */
    public Boolean removeFromUnKnownTag(final Long userId, final Long itemId) {
        return jedisTemplate.execute(new JedisTemplate.JedisAction<Boolean>() {
            @Override
            public Boolean action(Jedis jedis) {
                return jedis.zrem(Item.keyOfNoTagItems(userId), String.valueOf(itemId)) > 0L;
            }
        });
    }

    /**
     * 判断某商品是否有tag
     * @param itemId 商品id
     * @return 有返回true, 无返回false
     */
    public Boolean hasTags(final Long itemId) {
        return jedisTemplate.execute(new JedisTemplate.JedisAction<Boolean>() {
            @Override
            public Boolean action(Jedis jedis) {
                return jedis.scard(Item.keyOfItemTags(itemId)) > 0;
            }
        });
    }

    /**
     * 移除某个商品的tags集合
     * @param userId 用户id
     * @param itemId 商品id
     */
    public void removeTagsOfItem(final Long userId, final Long itemId) {
        final Set<String> tags = jedisTemplate.execute(new JedisTemplate.JedisAction<Set<String>>() {
            @Override
            public Set<String> action(Jedis jedis) {
                return jedis.smembers(Item.keyOfItemTags(itemId));
            }
        });
        jedisTemplate.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                Transaction t = jedis.multi();
                for (String tag : tags) {
                    t.zrem(Item.keyOfTagItems(userId, tag), String.valueOf(itemId));
                }
                // 删除商品tags集合key
                t.del(Item.keyOfItemTags(itemId));
                t.exec();
            }
        });
    }

    /**
     * 删除店铺内商品标签
     * @param userId 用户id
     * @param tag 标签名称
     */
    public Set<Long> removeTag(final Long userId, final String tag) {
        //找出tag下所有的商品id
        final Set<String> itemIdsOfTag = jedisTemplate.execute(new JedisTemplate.JedisAction<Set<String>>() {
            @Override
            public Set<String> action(Jedis jedis) {
                return jedis.zrange(Item.keyOfTagItems(userId, tag), 0, -1);
            }
        });

        final Set<Long> toUnknown = new HashSet<>();
        jedisTemplate.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                for (String itemIdStr : itemIdsOfTag) {
                    Long itemId = Long.parseLong(itemIdStr);
                    Set<String> mm = jedis.smembers(Item.keyOfItemTags(itemId));
                    if (mm != null && mm.size() == 1 && mm.contains(tag)) {
                        toUnknown.add(itemId);
                    }
                }
            }
        });

        jedisTemplate.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                Transaction t = jedis.multi();
                //删除tag到item的关联
                t.del(Item.keyOfTagItems(userId, tag));
                //删除item到tag的关联
                for (String itemIdStr : itemIdsOfTag) {
                    Long itemId = Long.parseLong(itemIdStr);
                    t.srem(Item.keyOfItemTags(itemId), tag);
                    if (toUnknown.contains(itemId)) {
                        t.zadd(Item.keyOfNoTagItems(userId), itemId, String.valueOf(itemId));
                    }
                }
                t.exec();
            }
        });

        return FluentIterable.from(itemIdsOfTag).transform(Longs.stringConverter()).toSet();
    }
}
