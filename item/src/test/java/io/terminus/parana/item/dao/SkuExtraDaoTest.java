package io.terminus.parana.item.dao;

import com.google.common.collect.Lists;
import io.terminus.parana.item.dao.mysql.SkuExtraDao;
import io.terminus.parana.item.model.SkuExtra;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Author: haolin
 * On: 12/23/14
 */
public class SkuExtraDaoTest extends BaseDaoTest {

    @Autowired
    private SkuExtraDao skuExtraDao;

    @Test
    public void testCreate(){
        SkuExtra extra = mock(1L);
        assertNotNull(skuExtraDao.create(extra));
    }

    @Test
    public void testCreates(){
        List<SkuExtra> extras = Lists.newArrayList();
        extras.add(mock(99L));
        extras.add(mock(100L));
        Assert.assertEquals(2, skuExtraDao.creates(extras).intValue());
    }

    @Test
    public void testUpdate(){
        SkuExtra extra = mock(8L);
        assertNotNull(skuExtraDao.create(extra));
        extra.setSpecialPrice(8888);
        skuExtraDao.update(extra);
        Assert.assertEquals(8888, skuExtraDao.load(extra.getId()).getSpecialPrice().intValue());
    }

    @Test
    public void testFindBySkuId(){
        SkuExtra extra = mock(2L);
        assertNotNull(skuExtraDao.create(extra));
        assertNotNull(skuExtraDao.findBySkuId(extra.getSkuId()));
        assertNull(skuExtraDao.findBySkuId(10001L));
    }

    @Test
    public void testFindByItemId(){
        SkuExtra extra = mock(3L);
        extra.setItemId(101L);
        skuExtraDao.create(extra);
        extra = mock(4L);
        extra.setItemId(101L);
        skuExtraDao.create(extra);
        Assert.assertEquals(2, skuExtraDao.findByItemId(101L).size());
        Assert.assertEquals(0, skuExtraDao.findByItemId(10001L).size());
    }

    private SkuExtra mock(Long skuId) {
        SkuExtra extra = new SkuExtra();
        extra.setItemId(skuId);
        extra.setSkuId(skuId);
        extra.setSpecialPrice(1000);
        return extra;
    }
}
