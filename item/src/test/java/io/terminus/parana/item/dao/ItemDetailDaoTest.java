/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package io.terminus.parana.item.dao;

import com.google.common.collect.ImmutableSet;
import io.terminus.parana.item.dao.mysql.ItemDetailDao;
import io.terminus.parana.item.model.ItemDetail;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Random;
import java.util.Set;

import static io.terminus.parana.test.TestHelper.ck;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Author: haolin
 * On: 9/7/14
 */
public class ItemDetailDaoTest extends BaseDaoTest {

    @Autowired
    private ItemDetailDao itemDetailDao;

    private Set<String> allTestKeys = ImmutableSet.of("itemId", "image1", "image2", "image3", "image4", "packing", "service", "attrImages");

    @Test
    public void testCreate(){
        assertNotNull(itemDetailDao.create(mock()));

        for (int tm=10; tm>0; --tm) {
            ItemDetail d = rmock();
            itemDetailDao.create(d);
            ck(ItemDetail.class, d, itemDetailDao.load(d.getId()), allTestKeys);
        }
    }

    @Test
    public void testUpdate() throws Exception {

        for (int tm=10; tm>0; --tm) {
            ItemDetail d = rmock(), toUpdate = rmock();
            itemDetailDao.create(d);
            toUpdate.setId(d.getId());
            itemDetailDao.update(toUpdate);

            ck(ItemDetail.class, toUpdate, itemDetailDao.load(d.getId()), allTestKeys);
        }
    }

    @Test
    public void testFindByItemId(){
        ItemDetail detail = mock(200L);
        itemDetailDao.create(detail);

        assertNotNull(itemDetailDao.findByItemId(200L));
        assertNull(itemDetailDao.findByItemId(1024L));
    }

    private ItemDetail mock(){
        ItemDetail detail = new ItemDetail();
        detail.setItemId(1L);
        detail.setImage1("image2");
        detail.setPacking("xxoo");
        detail.setService("ooxx");
        return detail;
    }

    private ItemDetail mock(Long itemId){
        ItemDetail detail = new ItemDetail();
        detail.setItemId(itemId);
        detail.setImage1("image2");
        detail.setPacking("xxoo");
        detail.setService("ooxx");
        return detail;
    }

    private ItemDetail rmock() {
        Random r = new Random();
        ItemDetail detail = new ItemDetail();
        detail.setId(r.nextLong());
        detail.setImage1(RandomStringUtils.random(100));
        detail.setImage2(RandomStringUtils.random(100));
        detail.setImage3(RandomStringUtils.random(100));
        detail.setImage4(RandomStringUtils.random(100));
        detail.setPacking(RandomStringUtils.random(200));
        detail.setService(RandomStringUtils.random(200));
        return detail;
    }
}
