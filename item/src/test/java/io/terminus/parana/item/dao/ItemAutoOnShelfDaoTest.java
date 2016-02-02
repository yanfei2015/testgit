package io.terminus.parana.item.dao;

import io.terminus.parana.item.dao.mysql.ItemAutoOnShelfDao;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * @author Effet
 */
public class ItemAutoOnShelfDaoTest extends BaseDaoTest {

    @Autowired
    private ItemAutoOnShelfDao itemAutoOnShelfDao;

    @Test
    public void testCloseBy() throws Exception {
        itemAutoOnShelfDao.closeBy(new Date(), new Date());
    }

    @Test
    public void testCloseBy1() throws Exception {

    }

    @Test
    public void testOpenBy() throws Exception {

    }

    @Test
    public void testCheckDup() throws Exception {

    }

    @Test
    public void testSetting() throws Exception {

    }

    @Test
    public void testGetIfPresent() throws Exception {

    }
}