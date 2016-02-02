/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package io.terminus.parana.item.dao;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.terminus.parana.item.dao.mysql.ItemDao;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.ItemType;
import io.terminus.parana.test.MySQLDaoTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Author: haolin
 * On: 9/7/14
 */
@RunWith(SpringJUnit4ClassRunner.class)
@MySQLDaoTest
public class ItemDaoTest {

    @Autowired
    private ItemDao itemDao;

    @Test
    public void testCreate(){
        Item item = mock();
        item.setDistributable(true);
        assertNotNull(itemDao.create(item));

        assertTrue(itemDao.load(item.getId()).getDistributable());
        assertEquals(mock().getOuterItemId(), itemDao.load(item.getId()).getOuterItemId());
    }

    @Test
    public void testUpdate() throws Exception {
        Item item = mock();
        item.setType(1);
        item.setBitMark(10);
        item.setDistributable(true);
        item.setOuterItemId("outer origin");

        itemDao.create(item);

        Item exist = itemDao.load(item.getId());
        assertEquals(item.getType(), exist.getType());
        assertEquals(item.getBitMark(), exist.getBitMark());
        assertEquals(item.getDistributable(), exist.getDistributable());
        assertEquals(item.getOuterItemId(), exist.getOuterItemId());

        Item toUpdate = new Item();
        toUpdate.setId(item.getId());
        toUpdate.setType(2);
        toUpdate.setBitMark(20);
        toUpdate.setDistributable(false);
        toUpdate.setOuterItemId("outer changed");

        itemDao.update(toUpdate);

        exist = itemDao.load(item.getId());
        assertEquals(toUpdate.getType(), exist.getType());
        assertEquals(toUpdate.getBitMark(), exist.getBitMark());
        assertEquals(toUpdate.getDistributable(), exist.getDistributable());
        assertEquals(toUpdate.getOuterItemId(), exist.getOuterItemId());
    }

    @Test
    public void testSetStatus(){
        Item mock = mock();
        itemDao.create(mock);
        Item inserted = itemDao.load(mock.getId());
        assertEquals(Item.Status.INIT.value(), inserted.getStatus().intValue());

        // on shelf
        itemDao.setStatus(inserted.getId(), Item.Status.ON_SHELF.value());
        Item updated = itemDao.load(mock.getId());
        assertEquals(Item.Status.ON_SHELF.value(), updated.getStatus().intValue());
    }

    @Test
    public void testSetStatuses(){
        Item mock = mock();
        itemDao.create(mock);
        itemDao.setStatuses(Arrays.asList(mock.getId()), Item.Status.FROZEN.value());
    }

    @Test
    public void testPagingItemsAsSeller(){
        int total = 10;
        for (int i=0; i < total; i++){
            Item item = mock();
            item.setUserId(1L);
            item.setName("item"+i);
            item.setSaleQuantity(i);
            item.setStockQuantity(i * 2);
            item.setStatus(Item.Status.ON_SHELF.value());
            item.setPrice(i * 100);
            itemDao.create(item);
        }

        Map<String, Object> criteria = Maps.newHashMap();
        criteria.put("userId", 1L);
        assertEquals(total, itemDao.findItemsAsSeller(criteria).getTotal().intValue());

        criteria.put("saleQuantityFrom", 4);
        assertEquals(6, itemDao.findItemsAsSeller(criteria).getTotal().intValue());
        criteria.put("saleQuantityTo", 8);
        assertEquals(5, itemDao.findItemsAsSeller(criteria).getTotal().intValue());

        criteria.put("priceFrom", 600);
        assertEquals(3, itemDao.findItemsAsSeller(criteria).getTotal().intValue());

        criteria.put("statuses", Lists.newArrayList(Item.Status.OFF_SHELF.value(), Item.Status.INIT.value()));
        assertEquals(0, itemDao.findItemsAsSeller(criteria).getTotal().intValue());
    }

    @Test
    public void testFindNewestItem() {
        assertThat(itemDao.findShopNewestItems(1L, 3, new int[] {1}), notNullValue());
    }

    @Test
    public void testCountOnShelfByShopIds(){
        Item mock = mock();
        mock.setStatus(1);
        itemDao.create(mock);

        mock = mock();
        mock.setStatus(1);
        itemDao.create(mock);

        mock = mock();
        mock.setStatus(1);
        mock.setShopId(2L);
        itemDao.create(mock);

        mock = mock();
        mock.setStatus(1);
        mock.setShopId(3L);
        itemDao.create(mock);

        List<Long> shopIds = Lists.newArrayList(2L, 1L, 3L);
        Map<Long, Long> shopItemCountMap = itemDao.countOnShelfByShopIds(shopIds, new int[] {1});
        for (Map.Entry<Long, Long> entry : shopItemCountMap.entrySet()){
            System.out.println(entry.getKey() + "--->" + entry.getValue());
        }
    }

    @Test
    public void testSearchType() throws Exception {
        Item im = mock();
        itemDao.create(im);

        assertTrue(itemDao.loadsBy(ImmutableMap.<String, Object>of("type", im.getType())).size() == 1);
        for (ItemType itemType : ItemType.values()) {
            if (itemType.value() != im.getType()) {
                assertTrue(itemDao.loadsBy(ImmutableMap.<String, Object>of("type", itemType.value())).size() == 0);
            }
        }
    }

    @Test
    public void testFindByOuterItemId() throws Exception {
        for (int i=0; i<5; ++i) {
            Item item = mock();
            item.setOuterItemId("outer" + i);
            itemDao.create(item);
        }

        for (int i=0; i<5; ++i) {
            assertEquals("outer" + i, itemDao.findByOuterItemId("outer" + i).get().getOuterItemId());
        }
    }

    private Item mock(){
        Item item = new Item();
        item.setOuterItemId("outer");
        item.setCategoryId(1L);
        item.setName("item_mock");
        item.setUserId(1L);
        item.setShopId(1L);
        item.setShopName("item_shop_name");
        item.setBrandId(1L);
        item.setStatus(Item.Status.INIT.value());
        item.setBrandName("brand_name_test");
        item.setProvinceId(1);
        item.setCityId(2);
        item.setRegionId(3);
        item.setMainImage("http://www");
        item.setSaleQuantity(10);
        item.setOriginPrice(10000);
        item.setPrice(9999);
        item.setSpuId(1L);
        item.setStockQuantity(1990);
        item.setType(ItemType.NORMAL.value());
        item.setBitMark(0);
        item.setDistributable(false);
        return item;
    }
}
