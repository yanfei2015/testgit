/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package io.terminus.parana.item.dao;

import io.terminus.parana.item.dao.mysql.ItemSnapshotDao;
import io.terminus.parana.item.model.ItemSnapshot;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Mail: xiao@terminus.io <br>
 * Date: 2014-11-19 9:42 AM  <br>
 * Author: xiao
 */
public class ItemSnapshotDaoTest extends BaseDaoTest {

    @Autowired
    private ItemSnapshotDao itemSnapshotDao;

    private ItemSnapshot itemSnapshot;

    private ItemSnapshot mock() {
        ItemSnapshot t = new ItemSnapshot();
        t.setItemId(1L);
        t.setSellerId(1L);
        t.setItemName("测试商品名称");
        t.setMainImage("http://img.parana.com/items/1");
        t.setDetail("saving img");
        itemSnapshotDao.create(t);
        ItemSnapshot actual = itemSnapshotDao.load(t.getId());
        t.setCreatedAt(actual.getCreatedAt());
        t.setUpdatedAt(actual.getUpdatedAt());
        assertThat(actual, is(t));
        return t;
    }

    @Before
    public void setUp() {
        itemSnapshot = mock();
    }

    @Test
    public void testLoadByOrderItemId() {
        ItemSnapshot actual = itemSnapshotDao.loadByDate(1L, DateTime.now().minusMinutes(1).toDate());
        assertThat(actual, is(itemSnapshot));
    }

}
