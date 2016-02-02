/*
 * Copyright (c) 2015 杭州端点网络科技有限公司
 */

package io.terminus.parana.category.internal;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.category.dao.mysql.FrontCategoryDao;
import io.terminus.parana.category.model.FrontCategory;
import io.terminus.parana.item.dto.ItemsAndSpusImportResult;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Effet
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:spring/mysql-dao-context-test.xml"
})
@TransactionConfiguration(transactionManager = "transactionManager", defaultRollback = true)
@Transactional
public class CategoryQueriesTest {

    @Autowired
    private FrontCategoryDao frontCategoryDao;

    void buildTree(int pid, int level, int deep, int wide) {
        if (deep == 0) {
            return;
        }

        for (int i=1; i<=wide; ++i) {
            FrontCategory fc = new FrontCategory();
            fc.setPid((long) pid);
            fc.setName(Joiner.on('_').join(Lists.newArrayList(pid, deep, wide)));
            fc.setHasChildren(deep != 1);
            fc.setLevel(level);
            assertTrue(frontCategoryDao.create(fc));
            buildTree(fc.getId().intValue(), level + 1, deep - 1, wide);
        }
    }

    void checkTree(int pid, int level, int deep, int wide) {
        if (deep == 0) {
            return;
        }

        List<FrontCategory> fcs = frontCategoryDao.findByPid((long)pid);

        assertEquals(wide, fcs.size());

        for (int i=1; i<=wide; ++i) {

            FrontCategory exist = fcs.get(i - 1);

            assertEquals(pid, exist.getPid().intValue());
            assertEquals(Joiner.on('_').join(Lists.newArrayList(pid, deep, wide)), exist.getName());
            assertEquals(deep != 1, exist.getHasChildren());
            assertEquals(level, exist.getLevel().intValue());
        }
    }

    @Test
    public void testFrontTree() throws Exception {
        buildTree(0, 1, 3, 5);
        checkTree(0, 1, 3, 5);
    }

    @Ignore
    public void testFile() throws Exception{
        InputStream inputStream = new URL("http://file.reddeals.com/importResults/import.products.shop35.20151026.225a3104-1227-49c8-aca1-1a0d6d977bd9.xlsx.handle_result.20151102.json").openStream();

        BufferedReader rd;
        StringBuilder sb;

        rd  = new BufferedReader(new InputStreamReader(inputStream , "UNICODE"));
        sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null)
        {
            sb.append(line);
        }

        System.out.println(sb.toString());

        ItemsAndSpusImportResult result = JsonMapper.nonDefaultMapper().fromJson(sb.toString() , ItemsAndSpusImportResult.class);

        System.out.print(result.getProductsImportResult().size());
    }
}