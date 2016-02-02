package io.terminus.parana.item.dao.mysql;

import com.google.common.collect.ImmutableMap;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.item.model.FavoriteItem;
import org.springframework.stereotype.Repository;

/**
 * Created by zhanghecheng on 15/8/11.
 */
@Repository
public class FavoriteItemDao extends MyBatisDao<FavoriteItem> {

    public Integer deleteByItemIdAndUserId(Long itemId, Long userId){
        return getSqlSession().delete(sqlId("deleteByItemIdAndUserId"), ImmutableMap.of("itemId", itemId, "userId", userId));
    }

    public Long count(Long itemId, Long userId){
        return getSqlSession().selectOne(sqlId("count"),  ImmutableMap.of("itemId", itemId, "userId", userId));
    }

}
