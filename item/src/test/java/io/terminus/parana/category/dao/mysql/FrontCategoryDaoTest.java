/*
 * Copyright (c) 2015 杭州端点网络科技有限公司
 */

package io.terminus.parana.category.dao.mysql;

import com.google.common.collect.Lists;
import io.terminus.parana.category.model.FrontCategory;
import io.terminus.parana.item.dao.BaseDaoTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Effet
 */
public class FrontCategoryDaoTest extends BaseDaoTest {

    @Autowired
    private FrontCategoryDao frontCategoryDao;

    private FrontCategory mock() {
        FrontCategory fc = new FrontCategory();
        fc.setPid(0l);
        fc.setName("fc");
        fc.setHasChildren(false);
        fc.setLevel(1);
        fc.setLogo("logo");
        fc.setBackground("back");
        return fc;
    }

    @Test
    public void testCreate() throws Exception {
        FrontCategory fc = mock();
        frontCategoryDao.create(fc);

        FrontCategory exist = frontCategoryDao.load(fc.getId());

        assertEquals(fc.getLogo(), exist.getLogo());
        assertEquals(fc.getBackground(), exist.getBackground());
    }

    @Test
    public void testUpdate() throws Exception {
        FrontCategory fc = mock();
        frontCategoryDao.create(fc);

        FrontCategory toUpdate = new FrontCategory();
        toUpdate.setId(fc.getId());
        toUpdate.setLogo(fc.getLogo() + "changed");
        toUpdate.setBackground(fc.getBackground() + "changed");

        frontCategoryDao.update(toUpdate);

        assertEquals(fc.getLogo() + "changed", frontCategoryDao.load(fc.getId()).getLogo());
        assertEquals(fc.getBackground() + "changed", frontCategoryDao.load(fc.getId()).getBackground());
    }

    @Test
    public void testFindInPids() throws Exception {
        int maxCnt = 5;
        for (long fid = 0; fid < 5; ++ fid) {
            for (int j=0; j<=fid; ++j) {
                FrontCategory fc = mock();
                fc.setPid(fid);
                frontCategoryDao.create(fc);
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

            assertEquals(cnt, frontCategoryDao.findInPids(fids).size());
        }
    }
}