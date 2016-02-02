package io.terminus.parana.item.dao.mysql;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import io.terminus.parana.item.model.ItemAutoOnShelf;
import org.mybatis.spring.support.SqlSessionDaoSupport;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Effet
 */
@Deprecated
@Repository
public class ItemAutoOnShelfDao extends SqlSessionDaoSupport {

    public int closeBy(List<Long> itemIds) {
        return getSqlSession().update("ItemAutoOnShelf.closeByItemIds", itemIds);
    }

    public int closeBy(Date left, Date right) {
        return getSqlSession().update("ItemAutoOnShelf.closeByRange", ImmutableMap.of("left", left, "right", right));
    }

    public int openBy(Date left, Date right) {
        return getSqlSession().update("ItemAutoOnShelf.openByRange", ImmutableMap.of("left", left, "right", right));
    }

    public boolean checkDup(Date left, Date right, List<Long> itemIds) {
        Long cnt = getSqlSession().selectOne("ItemAutoOnShelf.checkDup", ImmutableMap.of("left", left, "right", right, "itemIds", itemIds));
        return cnt > 0;
    }

    public int setting(List<Long> itemIds, int current, Date left, Date right) {
        List<ItemAutoOnShelf> list = new ArrayList<>();
        for (Long itemId : itemIds) {
            ItemAutoOnShelf autoOnShelf = new ItemAutoOnShelf();
            autoOnShelf.setItemId(itemId);
            autoOnShelf.setCurrent(current);
            autoOnShelf.setLeft(left);
            autoOnShelf.setRight(right);
            list.add(autoOnShelf);
        }
        return getSqlSession().insert("ItemAutoOnShelf.setting", list);
    }

    public Optional<ItemAutoOnShelf> getIfPresent(long itemId) {
        ItemAutoOnShelf itemAutoOnShelf = getSqlSession().selectOne("ItemAutoOnShelf.getIfPresent", itemId);
        return Optional.fromNullable(itemAutoOnShelf);
    }
}
