/*
 * <!--
 *   ~ Copyright (c) 2014 杭州端点网络科技有限公司
 *   -->
 */

package io.terminus.parana.item.dao.mysql;

import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.item.model.DeliverFeeTemplate;
import org.springframework.stereotype.Repository;

/**
 * Created by yangzefeng on 15/4/27
 */
@Repository
public class DeliverFeeTemplateDao extends MyBatisDao<DeliverFeeTemplate> {

    public DeliverFeeTemplate findDefaultByUserId(Long userId) {
        return getSqlSession().selectOne(sqlId("findDefaultByUserId"), userId);
    }

    public void unDefault(Long userId) {
        getSqlSession().update(sqlId("unDefault"), userId);
    }
}
