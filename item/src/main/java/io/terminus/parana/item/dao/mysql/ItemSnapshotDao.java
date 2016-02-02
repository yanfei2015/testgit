/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package io.terminus.parana.item.dao.mysql;

import com.google.common.collect.ImmutableMap;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.item.model.ItemSnapshot;
import org.springframework.stereotype.Repository;

import java.util.Date;

/**
 * Mail: xiao@terminus.io <br>
 * Date: 2014-11-19 9:40 AM  <br>
 * Author: xiao
 */
@Repository
public class ItemSnapshotDao extends MyBatisDao<ItemSnapshot> {

    public ItemSnapshot loadByDate(Long itemId, Date date) {
        return getSqlSession().selectOne(sqlId("loadByDate"),
                ImmutableMap.of("itemId", itemId, "date", date));
    }
}
