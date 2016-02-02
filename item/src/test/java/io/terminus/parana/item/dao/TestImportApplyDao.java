package io.terminus.parana.item.dao;

import io.terminus.parana.item.dao.mysql.ImportApplyDao;
import io.terminus.parana.item.model.ImportApply;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by zhanghecheng on 15/10/15.
 */
public class TestImportApplyDao extends BaseDaoTest {

    @Autowired
    ImportApplyDao dao;

    @Test
    public void test(){
        ImportApply t=new ImportApply();
        t.setSellerId(1l);
        t.setStatus(0);
        t.setShopId(2l);
        t.setFileUrl("fileurl");
        dao.create(t);

        ImportApply apply=dao.load(1l);
    }
}
