/*
 * <!--
 *   ~ Copyright (c) 2014 杭州端点网络科技有限公司
 *   -->
 */

package io.terminus.parana.item.dao;

import io.terminus.parana.item.dao.mysql.ShipFeeDao;
import io.terminus.parana.item.model.ShipFee;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Created by yangzefeng on 15/4/30
 */
public class ShipFeeDaoTest extends BaseDaoTest {

    @Autowired
    private ShipFeeDao shipFeeDao;

    private ShipFee shipFee;

    @Before
    public void init() {
        shipFee = make();
        shipFeeDao.create(shipFee);
    }

    @Test
    public void testUpdate() {
        shipFee.setDeliverFeeTemplateId(2L);
        shipFeeDao.update(shipFee);

        assertThat(shipFeeDao.load(shipFee.getId()).getDeliverFeeTemplateId(), is(2L));
    }

    @Test
    public void testFindByItemIds() {
        assertNotNull(shipFeeDao.findByItemIds(1L, 2L));
    }

    private ShipFee make() {
        ShipFee mock = new ShipFee();

        mock.setDeliverFeeTemplateId(1L);
        mock.setItemId(1L);
        return mock;
    }
}
