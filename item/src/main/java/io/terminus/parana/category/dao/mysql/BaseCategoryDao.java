/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package io.terminus.parana.category.dao.mysql;

import com.google.common.collect.ImmutableMap;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.category.model.BaseCategory;
import io.terminus.parana.common.util.Iters;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * 类目基础Dao
 * Author: haolin
 * On: 8/29/14
 */
public abstract class BaseCategoryDao<T extends BaseCategory> extends MyBatisDao<T> {

    /**
     * 通过名称及其父级ID唯一查询类目
     * @param name 类目名称
     * @return 类目
     */
    public T findByNameAndPid(Long pid, String name) {
        return getSqlSession().selectOne(sqlId("findByNameAndPid"), ImmutableMap.of("name", name, "pid", pid));
    }

    /**
     * 设置类目状态
     * @param id 类目id
     * @param status 状态, 1启用, -1禁用
     * @return 更新记录数
     */
    public Integer setStatus(Long id, int status) {
        return getSqlSession().update(sqlId("setStatus"), ImmutableMap.of("id", id, "status", status));
    }

    /**
     * 设置类目是否有孩子类目
     * @param id 类目id
     * @param hasChildren 是否有孩子类目
     * @return 更新记录数
     */
    public Integer setHasChildren(Long id, Boolean hasChildren) {
        return getSqlSession().update(sqlId("setHasChildren"), ImmutableMap.of("id", id, "hasChildren", hasChildren));
    }

    /**
     * 设置类目是否有SPU挂载
     * @param id 类目id
     * @param hasSpu 是否有SPU挂载
     * @return 更新记录数
     */
    public Integer setHasSpu(Long id, Boolean hasSpu) {
        return getSqlSession().update(sqlId("setHasSpu"), ImmutableMap.of("id", id, "hasSpu", hasSpu));
    }

    /**
     * 通过pid查询类目列表
     * @param id 类目id
     * @return 类目列表
     */
    public List<T> findByPid(Long id){
        return getSqlSession().selectList(sqlId("findByPid"), id);
    }

    /**
     * 通过 pids 批量查询类目
     * @param pids 父节点 id 列表
     * @return 类目列表
     */
    public List<T> findInPids(List<Long> pids) {
        return getSqlSession().selectList(sqlId("findInPids"), checkNotNull(Iters.emptyToNull(pids), "pids null or empty"));
    }

    /**
     * 通过level查询类目列表
     * @param id 类目id
     * @return 类目列表
     */
    public List<T> findByLevel(Long id){
        return getSqlSession().selectList(sqlId("findByLevel"), id);
    }
}
