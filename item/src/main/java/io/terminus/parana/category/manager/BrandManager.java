/*
 * Copyright (c) 2015 杭州端点网络科技有限公司
 */

package io.terminus.parana.category.manager;

import io.terminus.parana.category.dao.mysql.BrandSubsetDao;
import io.terminus.parana.category.model.BrandSubset;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * @author Effet
 */
@Slf4j
@Component
public class BrandManager {

    @Autowired
    private BrandSubsetDao brandSubsetDao;

    @Transactional
    public void brandSubsetIncrDecr(long bcId, Set<Long> toCreateBrandIds, Set<Long> toDeleteIds) {
        // create
        for (Long brandId : toCreateBrandIds) {
            BrandSubset subset = new BrandSubset();
            subset.setBcId(bcId);
            subset.setBrandId(brandId);
            brandSubsetDao.create(subset);
        }

        // delete
        for (Long id : toDeleteIds) {
            brandSubsetDao.delete(id);
        }
    }
}
