/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package io.terminus.parana.item.dao;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import io.terminus.parana.item.dao.mysql.SkuDao;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.Sku;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static io.terminus.parana.test.TestHelper.ck;
import static org.apache.commons.lang3.RandomStringUtils.random;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * Author: haolin
 * On: 9/7/14
 */
public class SkuDaoTest extends BaseDaoTest {
    @Autowired
    private SkuDao skuDao;

    private Set<String> allTestKeys = ImmutableSet.of(
            "skuCode", "model", "itemId", "shopId", "status", "outerItemId", "outerShopId", "image", "name", "desc", "originPrice", "price",
            "attributeKey1", "attributeKeyId1", "attributeName1", "attributeValue1",
            "attributeKey2", "attributeKeyId2", "attributeName2", "attributeValue2",
            "stockQuantity"
    );

    private Sku rmock() {
        Sku sku = new Sku();
        Random r = new Random();
        sku.setSkuCode(randomAlphanumeric(10));
        sku.setModel("ZUC-CW000-RED");
        sku.setItemId(r.nextLong());
        sku.setShopId(r.nextLong());
        sku.setStatus(Item.Status.values()[r.nextInt(Item.Status.values().length)]);
        sku.setOuterItemId(random(30));
        sku.setOuterShopId(random(30));
        sku.setImage(random(40));
        sku.setName(random(50));
        sku.setDesc(random(200));
        sku.setPrice(r.nextInt());
        sku.setAttributeKey1(random(50));
        sku.setAttributeKeyId1(r.nextLong());
        sku.setAttributeName1(random(50));
        sku.setAttributeValue1(random(50));
        sku.setAttributeKey2(random(50));
        sku.setAttributeKeyId2(r.nextLong());
        sku.setAttributeName2(random(50));
        sku.setAttributeValue2(random(50));
        sku.setStockQuantity(r.nextInt());
        return sku;
    }

    @Test
    public void testCreate(){
        assertNotNull(skuDao.create(mock(1L)));

        for (int tm=10; tm>0; --tm) {
            Sku s = rmock();
            assertTrue(skuDao.create(s));
            ck(Sku.class, s, skuDao.load(s.getId()), allTestKeys);
        }
    }

    @Test
    public void testCreates() throws Exception {
        List<Sku> ms = new ArrayList<>();
        for (int tm=10; tm>0; --tm)
            ms.add(rmock());

        assertEquals(10, skuDao.creates(ms).intValue());

        List<Sku> all = skuDao.listAll();
        assertEquals(10, all.size());
        for (int i=0; i<10; ++i) {
            ck(Sku.class, ms.get(i), all.get(i), allTestKeys);
        }
    }

    @Test
    public void testUpdate() throws Exception {
        Sku s = rmock();
        skuDao.create(s);

        Random rnd = new Random();

        for (int tm=10; tm>0; --tm) {
            Sku toUpdate = skuDao.load(s.getId());
            toUpdate.setAttributeKeyId1(rnd.nextLong());
            toUpdate.setAttributeKeyId2(rnd.nextLong());
            toUpdate.setOuterItemId(random(30));

            skuDao.update(toUpdate);

            ck(Sku.class, toUpdate, skuDao.load(s.getId()), allTestKeys);
        }
    }

    @Test
    public void testFindByItemId(){
        List<Sku> skus = Lists.newArrayList();
        skus.add(mock(1000L));
        skus.add(mock(1001L));
        skus.add(mock(1001L));
        skus.add(mock(1001L));
        skuDao.creates(skus);

        Sku criteria = new Sku();
        criteria.setItemId(1000L);
        assertEquals(1, skuDao.list(criteria).size());
        criteria.setItemId(1001L);
        assertEquals(3, skuDao.list(criteria).size());
        criteria.setItemId(1002L);
        assertEquals(0, skuDao.list(criteria).size());
    }

    @Test
    public void testDeleteByItemId(){
        List<Sku> skus = Lists.newArrayList();
        skus.add(mock(10L));
        skus.add(mock(10L));
        skus.add(mock(10L));
        skus.add(mock(10L));
        skuDao.creates(skus);

        assertEquals(skus.size(), skuDao.deleteByItemId(10L).intValue());
    }

    private Sku mock(Long itemId){
        Sku s = new Sku();
        s.setSkuCode(randomAlphanumeric(10));
        s.setItemId(itemId);
        s.setShopId(1L);
        s.setStatus(Item.Status.INIT);
        s.setStockQuantity(100);
        s.setAttributeKey1("attrkey1_" + itemId);
        s.setAttributeName1("attrname1_" + itemId);
        s.setAttributeValue1("attrval1_" + itemId);
        s.setAttributeKey2("attrkey2_" + itemId);
        s.setAttributeName2("attrname2_" + itemId);
        s.setAttributeValue2("attrval2_" + itemId);
        return s;
    }

    @Test
    public void testLoads() {
        List<Sku> skus = Lists.newArrayList();
        skus.add(mock(10L));
        skus.add(mock(10L));
        skus.add(mock(10L));
        skus.add(mock(10L));


        List<Long> ids = Lists.newArrayList();

        for (Sku sku : skus) {
            skuDao.create(sku);
            ids.add(sku.getId());
        }

        List<Sku> actual = skuDao.loads(ids);
        assertThat(actual.size(), is(4));


    }

    @Test
    public void testSetStatus() throws Exception {
        Sku sku = rmock();
        skuDao.create(sku);
        assertEquals(sku.getStatus(), skuDao.load(sku.getId()).getStatus());

        for (Item.Status status : Item.Status.values()) {
            skuDao.setStatus(Lists.newArrayList(sku.getItemId()), status);
            assertEquals(status, skuDao.load(sku.getId()).getStatus());
        }
    }
}
