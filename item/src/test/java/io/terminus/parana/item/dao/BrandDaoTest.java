/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package io.terminus.parana.item.dao;

import com.google.common.collect.ImmutableSet;
import io.terminus.parana.item.dao.mysql.BrandDao;
import io.terminus.parana.item.model.Brand;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Random;
import java.util.Set;

import static io.terminus.parana.test.TestHelper.ck;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Author: haolin
 * On: 9/4/14
 */
public class BrandDaoTest extends BaseDaoTest {

    @Autowired
    private BrandDao brandDao;

    private Set<String> allTestKeys = ImmutableSet.of("name", "enName", "enCap", "logo", "description", "type");

    @Test
    public void testCreate(){
        assertNotNull(brandDao.create(mock()));

        for (int tm=10; tm>0; --tm) {
            Brand b = rmock();
            brandDao.create(b);
            ck(Brand.class, b, brandDao.load(b.getId()), allTestKeys);
        }
    }

    @Test
    public void testUpdate() throws Exception {
        for (int tm=10; tm>0; --tm) {
            Brand b = rmock(), toUpdate = rmock();
            brandDao.create(b);
            ck(Brand.class, b, brandDao.load(b.getId()), allTestKeys);
            toUpdate.setId(b.getId());
            brandDao.update(toUpdate);

            ck(Brand.class, toUpdate, brandDao.load(b.getId()), allTestKeys);
        }
    }

    @Test
    public void testLoad(){
        Brand brand = mock();
        brandDao.create(brand);
        assertNotNull(brandDao.load(brand.getId()));
        assertNull(brandDao.load(1000000L));
    }

    @Test
    public void testList(){
        Brand nike = mock("nike");
        brandDao.create(nike);

        Brand adidas = mock("adidas");
        brandDao.create(adidas);

        Brand adwon = mock("adwon");
        brandDao.create(adwon);

        Brand lining = mock("lining");
        brandDao.create(lining);

        Brand metesbonwe = mock("metesbonwe");
        brandDao.create(metesbonwe);

        Brand criteria = new Brand();
        assertEquals(5, brandDao.list(criteria).size());

        // just do it
        criteria.setName("nike");
        assertEquals(1, brandDao.list(criteria).size());

        criteria.setName("ad");
        assertEquals(2, brandDao.list(criteria).size());

        criteria.setName("i");
        assertEquals(3, brandDao.list(criteria).size());
    }

    @Test
    public void testPaging(){
        Brand nike = mock("nike");
        brandDao.create(nike);

        Brand adidas = mock("adidas");
        brandDao.create(adidas);

        Brand adwon = mock("adwon");
        brandDao.create(adwon);

        Brand lining = mock("lining");
        brandDao.create(lining);

        Brand metesbonwe = mock("metesbonwe");
        brandDao.create(metesbonwe);

        Brand criteria = new Brand();
        assertEquals(5, brandDao.paging(0, 10, criteria).getTotal().intValue());
        assertEquals(5, brandDao.paging(0, 3, criteria).getTotal().intValue());
        assertEquals(3, brandDao.paging(0, 3, criteria).getData().size());

        criteria.setName("n");
        assertEquals(4, brandDao.paging(0, 10, criteria).getTotal().intValue());
        assertEquals(4, brandDao.paging(0, 3, criteria).getTotal().intValue());
        assertEquals(3, brandDao.paging(0, 3, criteria).getData().size());

        criteria.setName("I'am not here");
        assertEquals(0, brandDao.paging(0, 10, criteria).getTotal().intValue());
        assertEquals(0, brandDao.paging(0, 3, criteria).getTotal().intValue());
        assertEquals(0, brandDao.paging(0, 3, criteria).getData().size());
    }

    @Test
    public void testFindByName(){
        Brand brand = mock("NexT");
        brandDao.create(brand);
        assertNotNull(brandDao.findByName(brand.getName()));
        assertNull(brandDao.findByName("NotExist"));
    }

    @Test
    public void testFindByFuzzyName() throws Exception {
        Brand brand = mock("NexT");
        brandDao.create(brand);

        Brand b2 = mock("Next");
        brandDao.create(b2);

        assertEquals(2, brandDao.findByFuzzyName("N", 10).size());
        assertEquals(2, brandDao.findByFuzzyName("Ne", 10).size());
        assertEquals(2, brandDao.findByFuzzyName("Nex", 10).size());
        assertEquals(1, brandDao.findByFuzzyName("NexT", 10).size());
        assertEquals("NexT", brandDao.findByFuzzyName("NexT", 10).get(0).getName());
        assertEquals(1, brandDao.findByFuzzyName("Next", 10).size());
        assertEquals("Next", brandDao.findByFuzzyName("Next", 10).get(0).getName());
    }

    private Brand mock(){
        Brand brand = new Brand();
        brand.setName("brand");
        brand.setEnName("en brand");
        brand.setEnCap("E");
        brand.setLogo("logo");
        brand.setDescription("brandddd");
        brand.setType(Brand.Type.I18.value());
        return brand;
    }

    private Brand mock(String name){
        Brand brand = new Brand();
        brand.setName(name);
        brand.setEnName("en" + name);
        brand.setEnCap(name.substring(0, 0));
        brand.setDescription("brandddd");
        brand.setType(Brand.Type.I18.value());
        return brand;
    }

    private Brand rmock() {
        Brand b = new Brand();
        Random r = new Random();
        b.setName(RandomStringUtils.random(20));
        b.setEnName(RandomStringUtils.random(30));
        b.setEnCap(RandomStringUtils.random(1));
        b.setLogo(RandomStringUtils.random(50));
        b.setType(r.nextInt());
        b.setDescription(RandomStringUtils.random(100));
        return b;
    }
}
