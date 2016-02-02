/*
 * <!--
 *   ~ Copyright (c) 2014 杭州端点网络科技有限公司
 *   -->
 */

package io.terminus.parana.item.dao.mysql;

import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.item.model.SpecialDeliver;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by yangzefeng on 15/4/27
 */
@Repository
public class SpecialDeliverDao extends MyBatisDao<SpecialDeliver> {

    public List<SpecialDeliver> findByTemplateIds(Long... templateIds) {
        return getSqlSession().selectList(sqlId("findByTemplateIds"), templateIds);
    }
}
