/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package io.terminus.parana.category.dao.mysql;

import com.google.common.collect.ImmutableMap;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.pampas.common.Response;
import io.terminus.parana.category.model.CategoryBinding;
import org.apache.ibatis.session.ResultHandler;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 类目绑定Dao实现
 * Author: haolin
 * On: 8/29/14
 */
@Repository
public class CategoryBindingDao extends MyBatisDao<CategoryBinding> {

    /**
     * 根据后台类目id查询其绑定列表
     * @param backCategoryId 后台类目id
     * @return 后台类目id的绑定列表
     */
    public List<CategoryBinding> findByBackCategoryId(Long backCategoryId){
        return getSqlSession().selectList(sqlId("findByBackCategoryId"), backCategoryId);
    }

    /**
     * 根据前台类目id查询其绑定列表
     * @param frontCategoryId 前台类目id
     * @return 前台类目id的绑定列表
     */
    public List<CategoryBinding> findByFrontCategoryId(Long frontCategoryId){
        return getSqlSession().selectList(sqlId("findByFrontCategoryId"), frontCategoryId);
    }

    /**
     * 根据前台类目ID和后台类目ID查询绑定关系
     * @param frontCategoryId 前台ID
     * @param backCategoryId 后台id
     * @return 指定前后台绑定数量
     */
    public CategoryBinding findByFrontBackCategoryId(Long frontCategoryId,Long backCategoryId){
        return getSqlSession().selectOne(sqlId("findByFrontBackCategoryId"),ImmutableMap.of("frontCategoryId",frontCategoryId,"backCategoryId",backCategoryId));
    }
}
