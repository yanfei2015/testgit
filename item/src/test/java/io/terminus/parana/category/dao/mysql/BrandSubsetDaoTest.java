/*
 * Copyright (c) 2015 杭州端点网络科技有限公司
 */

package io.terminus.parana.category.dao.mysql;

import io.terminus.parana.category.model.BrandSubset;
import io.terminus.parana.item.dao.BaseDaoTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author Effet
 */
public class BrandSubsetDaoTest extends BaseDaoTest {

    @Autowired
    private BrandSubsetDao brandSubsetDao;

    private BrandSubset mock() {
        BrandSubset bs = new BrandSubset();
        bs.setBcId(1L);
        bs.setBrandId(2L);
        return bs;
    }

    private void ck(BrandSubset a, BrandSubset b) {
        assertNotNull("a null", a);
        assertNotNull("b null", b);

        assertEquals("bc id not equal", a.getBcId(), b.getBcId());
        assertEquals("brand id not equal", a.getBrandId(), b.getBrandId());
    }

    @Test
    public void testCreate() throws Exception {
        BrandSubset b = mock();
        brandSubsetDao.create(b);
        BrandSubset c = brandSubsetDao.findById(b.getId()).get();
        ck(b, c);
    }

    @Test
    public void testDelete() throws Exception {
        BrandSubset b = mock();
        brandSubsetDao.create(b);
        BrandSubset c = brandSubsetDao.findById(b.getId()).get();
        ck(b, c);

        brandSubsetDao.delete(b.getId());
        assertFalse(brandSubsetDao.findById(b.getId()).isPresent());
    }

    @Test
    public void testFindById() throws Exception {

    }

    @Test
    public void testFindByBcId() throws Exception {
        for (long bcId=1; bcId<=5; ++bcId) {
            for (long brandId = bcId; brandId <= 10; ++ brandId) {
                BrandSubset b = mock();
                b.setBcId(bcId);
                b.setBrandId(brandId);
                brandSubsetDao.create(b);
            }
            List<BrandSubset> exists = brandSubsetDao.findByBcId(bcId);
            assertEquals(10 - bcId + 1, exists.size());
            Set<Long> brandIds = new HashSet<>();
            for (BrandSubset exist : exists) {
                assertEquals(bcId, exist.getBcId().longValue());
                brandIds.add(exist.getBrandId());
            }
            assertEquals(10 - bcId + 1, brandIds.size());
            assertEquals(bcId, Collections.min(brandIds).longValue());
            assertEquals(10, Collections.max(brandIds).longValue());
        }
    }

    @Test
    public void testFindByBcIds() throws Exception {
        for (long bcId=1; bcId<=5; ++bcId) {
            for (long brandId = bcId; brandId <= 10; ++ brandId) {
                BrandSubset b = mock();
                b.setBcId(bcId);
                b.setBrandId(brandId);
                brandSubsetDao.create(b);
            }
            List<BrandSubset> exists = brandSubsetDao.findByBcIds(Arrays.asList(bcId));
            assertEquals(10 - bcId + 1, exists.size());
            Set<Long> brandIds = new HashSet<>();
            for (BrandSubset exist : exists) {
                assertEquals(bcId, exist.getBcId().longValue());
                brandIds.add(exist.getBrandId());
            }
            assertEquals(10 - bcId + 1, brandIds.size());
            assertEquals(bcId, Collections.min(brandIds).longValue());
            assertEquals(10, Collections.max(brandIds).longValue());
        }
    }

    @Test
    public void testFindByBcIdAndBrandId() throws Exception {
        for (long bcId=1; bcId<=5; ++bcId) {
            for (long brandId = bcId; brandId <= 10; ++ brandId) {
                BrandSubset b = mock();
                b.setBcId(bcId);
                b.setBrandId(brandId);
                brandSubsetDao.create(b);
            }
        }

        for (long bcId=1; bcId<=5; ++bcId) {
            for (long brandId = bcId; brandId <= 10; ++ brandId) {
                BrandSubset exist = brandSubsetDao.findByBcIdAndBrandId(bcId, brandId).get();
                assertEquals(bcId, exist.getBcId().longValue());
                assertEquals(brandId, exist.getBrandId().longValue());
            }
        }
    }
}