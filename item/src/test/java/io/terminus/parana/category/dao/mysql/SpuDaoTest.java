/*
 * Copyright (c) 2015 杭州端点网络科技有限公司
 */

package io.terminus.parana.category.dao.mysql;

import io.terminus.parana.category.model.Spu;
import io.terminus.parana.item.dao.BaseDaoTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;

/**
 * @author Effet
 */
public class SpuDaoTest extends BaseDaoTest {

    @Autowired
    private SpuDao spuDao;

    private Spu mock() {
        Spu s = new Spu();
        s.setStatus(1);
        s.setName("name");
        s.setBrandId(1);
        s.setBrandName("gdd");
        s.setCategoryId(123l);
        return s;
    }

    @Test
    public void testCreate() throws Exception {
        Spu s = mock();
        spuDao.create(s);
        Spu t = spuDao.load(s.getId());
        assertEquals(s.getName(), t.getName());
        assertEquals(s.getBrandId(), t.getBrandId());
        assertEquals(s.getBrandName(), t.getBrandName());
        assertEquals(s.getCategoryId(), t.getCategoryId());
    }

    @Test
    public void testUpdate() throws Exception {
        Spu s = mock();
        spuDao.create(s);
        Spu t = spuDao.load(s.getId());
        assertEquals(s.getName(), t.getName());
        t.setName(t.getName() + "blah blah");
        spuDao.update(t);
        Spu u = spuDao.load(s.getId());
        assertEquals(t.getName(), u.getName());
    }
}