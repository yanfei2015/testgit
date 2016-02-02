/*
 * <!--
 *   ~ Copyright (c) 2014 杭州端点网络科技有限公司
 *   -->
 */

package io.terminus.parana.item.dao;

import com.google.common.collect.Lists;
import io.terminus.parana.item.dao.mysql.SpecialDeliverDao;
import io.terminus.parana.item.model.SpecialDeliver;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * Created by yangzefeng on 15/4/28
 */
public class SpecialDeliverDaoTest extends BaseDaoTest {

    @Autowired
    private SpecialDeliverDao specialDeliverDao;

    private SpecialDeliver specialDeliver;

    @Before
    public void init() {
        specialDeliver = make();
        specialDeliverDao.create(specialDeliver);
    }

    @Test
    public void testUpdate() {
        specialDeliver.setFirstFee(50);
        specialDeliver.setValuation(2);
        specialDeliverDao.update(specialDeliver);
        assertThat(specialDeliverDao.load(specialDeliver.getId()).getFirstFee(), is(50));
        assertThat(specialDeliverDao.load(specialDeliver.getId()).getValuation(), is(2));
    }

    @Test
    public void testDelete() {
        specialDeliverDao.delete(specialDeliver.getId());
        assertNull(specialDeliverDao.load(specialDeliver.getId()));
    }

    @Test
    public void testFindByTemplateId() {
        assertThat(specialDeliverDao.findByTemplateIds(specialDeliver.getDeliverFeeTemplateId()).size(), is(1));
    }

    @Test
    public void testDeletes() {
        SpecialDeliver specialDeliver1 = make();
        specialDeliverDao.create(specialDeliver1);
        assertThat(specialDeliverDao.deletes(Lists.newArrayList(specialDeliver1.getId(), specialDeliver.getId())), is(2));
    }

    private SpecialDeliver make() {
        SpecialDeliver specialDeliver = new SpecialDeliver();
        specialDeliver.setUserId(1L);
        specialDeliver.setAddresses("test");
        specialDeliver.setAddressForDisplay("test");
        specialDeliver.setDeliverFeeTemplateId(1L);
        specialDeliver.setFirstFee(10);
        specialDeliver.setValuation(1);
        specialDeliver.setPerFee(5);
        specialDeliver.setMethod(1);
        return specialDeliver;
    }
}
