/*
 * <!--
 *   ~ Copyright (c) 2014 杭州端点网络科技有限公司
 *   -->
 */

package io.terminus.parana.item.dao;

import com.google.common.collect.Lists;
import io.terminus.parana.item.dao.mysql.DeliverFeeTemplateDao;
import io.terminus.parana.item.model.DeliverFeeTemplate;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * Created by yangzefeng on 15/4/28
 */
public class DeliverFeeTemplateDaoTest extends BaseDaoTest {

    @Autowired
    private DeliverFeeTemplateDao deliverFeeTemplateDao;

    private DeliverFeeTemplate deliverFeeTemplate;

    @Before
    public void init() {
        deliverFeeTemplate = make();
        deliverFeeTemplateDao.create(deliverFeeTemplate);
    }

    @Test
    public void testUpdate() {
        deliverFeeTemplate.setLowFee(100);
        deliverFeeTemplateDao.update(deliverFeeTemplate);
        assertThat(deliverFeeTemplateDao.load(deliverFeeTemplate.getId()).getLowFee(), is(100));
    }

    @Test
    public void testDelete() {
        deliverFeeTemplateDao.delete(deliverFeeTemplate.getId());
        assertNull(deliverFeeTemplateDao.load(deliverFeeTemplate.getId()));
    }

    @Test
    public void testFindDefaultByUserId() {
        assertNotNull(deliverFeeTemplateDao.findDefaultByUserId(deliverFeeTemplate.getUserId()));
    }

    @Test
    public void unDefault() {
        deliverFeeTemplateDao.unDefault(deliverFeeTemplate.getUserId());
        assertThat(deliverFeeTemplateDao.load(deliverFeeTemplate.getId()).getIsDefault(), is(false));
    }

    @Test
    public void testLoads() {
        assertThat(deliverFeeTemplateDao.loads(Lists.newArrayList(deliverFeeTemplate.getId())).size(), is(1));
    }

    private DeliverFeeTemplate make() {
        DeliverFeeTemplate deliverFeeTemplate = new DeliverFeeTemplate();
        deliverFeeTemplate.setName("test");
        deliverFeeTemplate.setUserId(1L);
        deliverFeeTemplate.setFee(10);
        deliverFeeTemplate.setLowFee(20);
        deliverFeeTemplate.setLowPrice(50);
        deliverFeeTemplate.setMethod(1);
        deliverFeeTemplate.setValuation(1);
        deliverFeeTemplate.setIsDefault(true);
        return deliverFeeTemplate;
    }
}
