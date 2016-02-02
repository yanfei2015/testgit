/*
 * Copyright (c) 2015 杭州端点网络科技有限公司
 */

package io.terminus.parana.item.dao;

import com.google.common.collect.Lists;
import io.terminus.parana.item.dao.mysql.SkuPriceDao;
import io.terminus.parana.item.model.SkuPrice;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SkuPriceDaoTest extends BaseDaoTest {

    @Autowired
    private SkuPriceDao dao;

    private SkuPrice mock() {
        SkuPrice p = new SkuPrice();
        p.setItemId(1L);
        p.setSkuId(1L);
        p.setDesc("desc");
        p.setPrice(1000);
        p.setLv(1);
        return p;
    }

    @Test
    public void testCreate() throws Exception {
        SkuPrice p = mock();
        dao.create(p);
        assertEquals(p, dao.findById(p.getId()));
    }

    @Test
    public void testUpdateById() throws Exception {
        Long id = dao.create(mock());
        SkuPrice toUpdate = new SkuPrice();
        toUpdate.setId(id);
        toUpdate.setDesc(mock().getDesc() + "2");
        toUpdate.setPrice(100);
        toUpdate.setLv(2);
        dao.updateById(toUpdate);

        SkuPrice truly = mock();
        truly.setDesc(truly.getDesc() + "2");
        truly.setPrice(100);
        truly.setLv(2);
        assertEquals(truly, dao.findById(id));
    }

    @Test
    public void testUpdateBySkuId() throws Exception {
        SkuPrice p = mock();
        dao.create(p);
        SkuPrice toUpdate = new SkuPrice();
        toUpdate.setDesc(mock().getDesc() + "2");
        toUpdate.setSkuId(p.getSkuId());
        toUpdate.setPrice(100);
        toUpdate.setLv(2);
        dao.updateBySkuId(toUpdate);

        SkuPrice truly = mock();
        truly.setDesc(truly.getDesc() + "2");
        truly.setPrice(100);
        truly.setLv(2);
        assertEquals(truly, dao.findById(p.getId()));

    }

    @Test
    public void testDelete() throws Exception {
        SkuPrice p = mock();
        dao.create(p);
        assertEquals(mock(), dao.findById(p.getId()));
        dao.delete(p.getId());
        assertNull(dao.findById(p.getId()));
    }

    @Test
    public void testFindById() throws Exception {
        // need not to test
    }

    @Test
    public void testFindBySkuIdWithLevel() throws Exception {
        SkuPrice p = mock();
        dao.create(p);

        SkuPrice p1 = mock();
        p1.setLv(p1.getLv() + 1);
        dao.create(p1);

        assertEquals(p, dao.findBySkuIdWithLevel(p.getSkuId(), p.getLv()));
        assertEquals(p1, dao.findBySkuIdWithLevel(p.getSkuId(), p.getLv() + 1));
    }

    @Test
    public void testFindBySkuId() throws Exception {
        SkuPrice p = mock();
        dao.create(p);

        SkuPrice p1 = mock();
        p1.setLv(p1.getLv() + 1);
        dao.create(p1);

        assertTrue(Lists.newArrayList(p, p1).containsAll(dao.findBySkuId(p.getSkuId())));
    }

    @Test
    public void testFindInSkuIds() throws Exception {
        SkuPrice[] ps = new SkuPrice[5];
        for (int i=0; i<5; ++i) {
            ps[i] = mock();
            ps[i].setSkuId(1L);
            dao.create(ps[i]);
        }

        SkuPrice[] ps2 = new SkuPrice[10];
        for (int i=0; i<10; ++i) {
            ps2[i] = mock();
            ps2[i].setSkuId(2L);
            dao.create(ps2[i]);
        }

        SkuPrice[] ps3 = new SkuPrice[15];
        for (int i=0; i<15; ++i) {
            ps3[i] = mock();
            ps3[i].setSkuId(3L);
            dao.create(ps3[i]);
        }

        assertEquals(5, dao.findInSkuIds(Lists.newArrayList(1L)).size());
        assertEquals(10, dao.findInSkuIds(Lists.newArrayList(2L)).size());
        assertEquals(15, dao.findInSkuIds(Lists.newArrayList(3L)).size());

        assertEquals(25, dao.findInSkuIds(Lists.newArrayList(2L, 3L)).size());
    }
}