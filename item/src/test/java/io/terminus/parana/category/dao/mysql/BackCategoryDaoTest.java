/*
 * Copyright (c) 2015 杭州端点网络科技有限公司
 */

package io.terminus.parana.category.dao.mysql;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.terminus.parana.category.model.BackCategory;
import io.terminus.parana.item.dao.BaseDaoTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Effet
 */
public class BackCategoryDaoTest extends BaseDaoTest {

    @Autowired
    private BackCategoryDao backCategoryDao;

    private BackCategory mock() {
        BackCategory bc = new BackCategory();
        bc.setPid(1L);
        bc.setLevel(1);
        bc.setHasChildren(false);
        bc.setName("name");
        bc.setHasSpu(false);
        bc.setStatus(1);
        bc.setOuterId("outer id");
        bc.setShopBusinessId(1l);
        return bc;
    }

    @Test
    public void testCreate() throws Exception {
        BackCategory b = mock();
        assertTrue(backCategoryDao.create(b));
        BackCategory c = backCategoryDao.load(b.getId());
        assertEquals(b.getOuterId(), c.getOuterId());
        assertEquals(b.getShopBusinessId(), c.getShopBusinessId());
    }

    @Test
    public void testUpdate() throws Exception {
        BackCategory b = mock();
        assertTrue(backCategoryDao.create(b));
        BackCategory c = backCategoryDao.load(b.getId());
        assertEquals(b.getOuterId(), c.getOuterId());

        c.setOuterId(c.getOuterId() + "out out");
        c.setShopBusinessId(c.getShopBusinessId() + 1);
        backCategoryDao.update(c);

        BackCategory d = backCategoryDao.load(b.getId());
        assertEquals(c.getOuterId(), d.getOuterId());
    }

    @Test
    public void testFindInPids() throws Exception {
        int maxCnt = 5;
        for (long fid = 0; fid < 5; ++ fid) {
            for (int j=0; j<=fid; ++j) {
                BackCategory bc = mock();
                bc.setPid(fid);
                backCategoryDao.create(bc);
            }
        }

        for (int st = 1; st < (1<<maxCnt); ++st) {
            List<Long> fids = Lists.newArrayList();
            int cnt = 0;
            for (long fid = 0; fid < 5; ++ fid) {
                if ((st & (1<<fid)) != 0) {
                    fids.add(fid);
                    cnt += fid + 1;
                }
            }

            assertEquals(cnt, backCategoryDao.findInPids(fids).size());
        }
    }

    @Test
    public void testBackCategoryPaging() throws Exception{
        BackCategory bca = mock();
        bca.setName("test1");
        backCategoryDao.create(bca);
        BackCategory bcb = mock();
        bcb.setName("test2");
        backCategoryDao.create(bcb);
        BackCategory bcc = mock();
        bcc.setName("backtest");
        bcc.setUpdatedAt(new Date());
        backCategoryDao.create(bcc);

        Map<String,Object> criteria = Maps.newHashMap();
        criteria.put("updatedFrom","2015-10-9");
        criteria.put("updatedTo",new Date());
        assertEquals(3, backCategoryDao.backCategoryPaging(0, 10, criteria).getTotal().intValue());
    }
}