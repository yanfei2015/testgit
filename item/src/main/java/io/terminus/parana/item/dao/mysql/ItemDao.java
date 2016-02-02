/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package io.terminus.parana.item.dao.mysql;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import io.terminus.common.model.Paging;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.item.model.Item;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 商品Dao实现
 * Author: haolin
 * On: 8/28/14
 */
@Repository
public class ItemDao extends MyBatisDao<Item> {

    /**
     * 更新商品状态
     * @param id 商品id
     * @param status 状态
     * @return 更新记录数
     */
    @Deprecated
    public Integer setStatus(Long id, Integer status){
        return getSqlSession().update(sqlId("setStatus"), ImmutableMap.of("id", id, "status", status));
    }

    /**
     * 更新商品列表状态
     * @param ids 商品id列表
     * @param status 状态
     * @return 更新记录数
     */
    public Integer setStatuses(List<Long> ids, Integer status){
        return getSqlSession().update(sqlId("setStatuses"), ImmutableMap.of("ids", ids, "status", status));
    }

    /**
     * 卖家店铺内查询商品
     * @param criteria 查询条件
     * @return 商品分页列表
     */
    public Paging<Item> findItemsAsSeller(Map<String, Object> criteria) {
        Long total = getSqlSession().selectOne(sqlId("countItemsAsSeller"), criteria);
        if (total == 0L){
            return new Paging<Item>(0L, Collections.<Item>emptyList());
        }
        List<Item> data = getSqlSession().selectList(sqlId("pagingItemsAsSeller"), criteria);
        return new Paging<Item>(total, data);
    }

    /**
     * 统计某SPU下的商品数
     * @param spuId SPU.id
     * @return 某SPU下的商品数
     */
    public Long countBySpuId(Long spuId) {
        return getSqlSession().selectOne(sqlId("countBySpuId"), spuId);
    }

    /**
     * 统计店铺内上架的商品数
     * @param shopId 店铺id
     * @param types 一般商品类型数组
     * @return 店铺内上架的商品数
     */
    public Long countOnShelfByShopId(Long shopId, int[] types) {
        return getSqlSession().selectOne(sqlId("countOnShelfByShopId"),
                ImmutableMap.of("shopId", shopId, "types", types));
    }

    public Long countOnShelfByUserId(Long userId, int[] types) {
        return getSqlSession().selectOne(sqlId("countOnShelfByUserId"),
                ImmutableMap.of("userId", userId, "types", types));
    }

    public Long countOffShelfByShopId(Long shopId, int[] types) {
        return getSqlSession().selectOne(sqlId("countOffShelfByShopId"),
                ImmutableMap.of("shopId", shopId, "types", types));
    }

    public Long countOffShelfByUserId(Long userId, int[] types) {
        return getSqlSession().selectOne(sqlId("countOffShelfByUserId"),
                ImmutableMap.of("userId", userId, "types", types));
    }

    /**
     * 批量统计店铺内上架的商品数
     * @param shopIds 店铺id列表
     * @param types 一般商品类型数组
     * @return 店铺内上架的商品数
     */
    public Map<Long, Long> countOnShelfByShopIds(List<Long> shopIds, int[] types) {
        List<Map<String, Long>> maps = getSqlSession().selectList(sqlId("countOnShelfByShopIds"),
                ImmutableMap.of("shopIds", shopIds, "types", types));
        if (Iterables.isEmpty(maps)){
           return Maps.newHashMap();
        }
        Map<Long, Long> res = Maps.newHashMapWithExpectedSize(maps.size());
        for (Map<String, Long> map : maps){
            res.put(map.get("SHOPID"), map.get("ITEMCOUNT"));
        }
        return res;
    }

    /**
     * 更新销量
     * @param itemId 商品id
     * @param quantity 更新量
     * @return 更新记录数
     */
    public Integer updateSaleQuantity(Long itemId, Integer quantity) {
        return getSqlSession().update(sqlId("updateSaleQuantity"), ImmutableMap.of("itemId", itemId, "quantity", quantity));
    }

    /**
     * 通过店铺id冻结商品(非逻辑删除的)
     * @param shopIds 店铺id列表
     * @return 更新记录数
     */
    public Integer frozenByShopIds(List<Long> shopIds) {
        return getSqlSession().update(sqlId("frozenByShopIds"), shopIds);
    }

    /**
     * 通过店铺id解冻商品(非逻辑删除的)，即变为下架状态
     * @param shopIds 店铺id列表
     * @return 更新记录数
     */
    public Integer unFrozenByShopIds(List<Long> shopIds) {
        return getSqlSession().update(sqlId("unFrozenByShopIds"), shopIds);
    }

    /**
     * 获取最后一个商品的id
     * @return 最后一个商品的id
     */
    public Long lastId() {
        Long lastId = getSqlSession().selectOne(sqlId("lastId"));
        return lastId == null ? 0L : lastId;
    }

    /**
     * 查询id小于lastId内的limit个商品
     * @param lastId 最大的商品id
     * @param limit 商品个数
     * @return id小于lastId内的pageSize个店铺
     */
    public List<Item> listTo(Long lastId, int limit) {
        return getSqlSession().selectList(sqlId("listTo"), ImmutableMap.of("lastId", lastId, "limit", limit));
    }

    /**
     * 查询id小于lastId内且更新时间大于since的limit个商品
     * @param lastId lastId 最大的店铺id
     * @param since 起始更新时间
     * @param limit 商品个数
     * @return id小于lastId内且更新时间大于since的limit个店铺
     */
    public List<Item> listSince(Long lastId, String since, int limit) {
        return getSqlSession().selectList(sqlId("listSince"), ImmutableMap.of("lastId", lastId, "limit", limit, "since", since));
    }

    /**
     * 根据商品id列表查询店铺id列表
     * @param ids 商品id列表
     * @return 店铺id列表
     */
    public List<Long> findShopIdsByItemIds(List<Long> ids) {
        return getSqlSession().selectList(sqlId("findShopIdsByItemIds"), ids);
    }

    /**
     * 根据店铺id列表查询商品id列表
     * @param shopIds 店铺id列表
     * @param types 一般商品类型数组
     * @return 商品id列表
     */
    public List<Long> findItemIdsByShopIds(List<Long> shopIds, int[] types){
        return getSqlSession().selectList(sqlId("findItemIdsByShopIds"),
                ImmutableMap.of("shopIds", shopIds, "types", types));
    }

    /**
     * 更新商品的营销活动标志
     * @param itemId 商品ID
     * @param marketTag 商品的营销活动标志, 二进制字符串, 0表示无该活动，1表示有该活动
     * @return 更新成功返回true, 反之false
     */
    public Boolean updateMarketTag(Long itemId, String marketTag){
        return getSqlSession().update(sqlId("updateMarketTag"),
                ImmutableMap.of("id", itemId, "marketTag", marketTag)) > 0;
    }

    /**
     * 查询店铺内最近的{limit}个商品
     * @param shopId 店铺id
     * @param limit 商品个数
     * @param types 一般商品类型数组
     * @return 商品列表
     */
    public List<Item> findShopNewestItems(Long shopId, Integer limit, int[] types) {
        return getSqlSession().selectList(sqlId("findShopNewestItems"), ImmutableMap.of(
                "shopId", shopId, "limit", limit, "types", types
        ));
    }

    /**
     * 根据条件查询商品列表
     * @param criteria 查询条件
     * @return 商品列表
     */
    public List<Item> loadsBy(Map<String, Object> criteria) {
        return getSqlSession().selectList(sqlId("loadsBy"), criteria);
    }

    /** 查询店铺内按销量从高到低排序的商品
     * 查询店铺内最近的{limit}个商品，并且这些商品不在itemIds中
     * @param shopId 店铺id
     * @param limit 商品个数
     * @param itemIds 商品id列表
     * @param types 一般商品类型数组
     * @return 商品列表
     */
    public List<Item> findShopNewestItemsWithoutItemIds(Long shopId, Integer limit, List<Long> itemIds, int[] types) {
        return getSqlSession().selectList(sqlId("findShopNewestItemsWithoutItemIds"), ImmutableMap.of(
                "shopId", shopId, "limit", limit, "itemIds", itemIds, "types", types
        ));
    }

    /**
     * 查询店铺内按销量从高到低排序的商品
     * @param shopId 店铺id
     * @param limit 商品个数
     * @param types 一般商品类型数组
     * @return 商品列表
     */
    public List<Item> findShopSaleItem(Long shopId, Integer limit, int[] types) {
        return getSqlSession().selectList(sqlId("findShopSaleItem"), ImmutableMap.of(
                "shopId", shopId, "limit", limit, "types", types
        ));
    }

    /**
     * 查询店铺内按销量从高到低排序的商品, 并且商品不在itemIds中
     * @param shopId 店铺id
     * @param limit 商品个数
     * @param itemIds 商品id列表
     * @param types 一般商品类型数组
     * @return 商品列表
     */
    public List<Item> findShopSaleItemWithoutItemIds(Long shopId, Integer limit, List<Long> itemIds, int[] types) {
        return getSqlSession().selectList(sqlId("findShopSaleItemWithoutItemIds"), ImmutableMap.of(
                "shopId", shopId, "limit", limit, "itemIds", itemIds, "types", types
        ));
    }

    /**
     * 商品每日增长数量
     * @param startAt 开始时间
     * @param endAt 结束时间
     * @param types 一般商品类型数组
     * @return 每日新增商品数
     */
    public Integer dailyItemIncrement(String startAt, String endAt, int[] types) {
        return getSqlSession().selectOne(sqlId("dailyItemIncrement"), ImmutableMap.of(
                "startAt", startAt, "endAt", endAt, "types", types
        ));
    }

    /**
     * 每日上架数量
     * @param startAt 开始时间
     * @param endAt 结束时间
     * @param types 一般商品类型数组
     * @return 每日上架商品数
     */
    public Integer dailyOnShelfItem(String startAt, String endAt, int[] types) {
        return getSqlSession().selectOne(sqlId("dailyOnShelfItem"), ImmutableMap.of(
                "startAt", startAt, "endAt", endAt, "types", types
        ));
    }

    public Optional<Item> findByOuterItemId(String outerItemId) {
        if (Strings.isNullOrEmpty(outerItemId))
            return Optional.absent();
        Item item = getSqlSession().selectOne(sqlId("findByOuterItemId"), outerItemId);
        return Optional.fromNullable(item);
    }

    public Integer checkItemDup(long userId, String name) {
        return getSqlSession().selectOne(sqlId("checkItemDup"), ImmutableMap.of("userId", userId, "name", name));
    }

    public Long getDupItemId(long userId, String name) {
        return getSqlSession().selectOne(sqlId("getDupItemId"), ImmutableMap.of("userId", userId, "name", name));
    }
}
