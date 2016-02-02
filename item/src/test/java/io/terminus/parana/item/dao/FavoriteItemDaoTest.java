package io.terminus.parana.item.dao;

import io.terminus.common.model.Paging;
import io.terminus.parana.item.dao.mysql.FavoriteItemDao;
import io.terminus.parana.item.model.FavoriteItem;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by zhanghecheng on 15/8/11.
 */
public class FavoriteItemDaoTest extends BaseDaoTest {

    @Autowired
    FavoriteItemDao dao;
    FavoriteItem f;

    @Before
    public void init(){
        f=mock();
        dao.create(f);

        f=mock();
        f.setId(null);
        f.setUserId(2l);
        dao.create(f);
    }

    @Test
    public void testLoad(){
        FavoriteItem actual=dao.load(f.getId());
        Assert.assertEquals(f.getName(), actual.getName());
    }

    @Test
    public void testList(){
        List<FavoriteItem> actual=dao.listAll();
        Assert.assertEquals(2l, actual.size());
    }

    @Test
    public void testDeleteByItemId(){
        Integer result=dao.deleteByItemIdAndUserId(1l, 1l);
        Assert.assertEquals(1, result.intValue());
    }

    @Test
    public void testPaging(){
        Map<String, Object> criteria=new HashMap<>();
        criteria.put("userId", 1);
        criteria.put("itemIds", Arrays.asList(1l,2l));
        Paging<FavoriteItem> actual=dao.paging(0, 20, criteria);
        Assert.assertEquals(1l, actual.getTotal().longValue());
    }

    @Test
    public void testCount(){
        Long exists=dao.count(1l,1l);
        Assert.assertEquals(1l, exists.longValue());

        exists=dao.count(2l,1l);
        Assert.assertEquals(0l, exists.longValue());
    }

    public FavoriteItem mock(){
        FavoriteItem item=new FavoriteItem();
        item.setName("item1");
        item.setBrand("addidas");
        item.setItemId(1l);
        item.setUserId(1l);
        item.setImage("urlimage");
        item.setPrice(2l);
        item.setOriginPrice(4l);
        item.setDiscount(50l);
        item.setPaidNumber(4l);
        return item;
    }
}
