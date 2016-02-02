/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package io.terminus.parana.category.dao.mysql;

import com.google.common.collect.ImmutableMap;
import io.terminus.parana.category.model.FrontCategory;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 前台类目Dao实现
 * Author: haolin
 * On: 8/29/14
 */
@Repository
public class FrontCategoryDao extends BaseCategoryDao<FrontCategory> {

    public List<FrontCategory> findByName(String name) {
        return getSqlSession().selectList(sqlId("findByName"), ImmutableMap.of("name", name));
    }
}
