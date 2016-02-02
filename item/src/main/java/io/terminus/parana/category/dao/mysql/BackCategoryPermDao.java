/*
 * Copyright (c) 2015 杭州端点网络科技有限公司
 */

package io.terminus.parana.category.dao.mysql;

import com.google.common.base.Optional;
import io.terminus.parana.category.model.BackCategoryPerm;
import org.mybatis.spring.support.SqlSessionDaoSupport;
import org.springframework.stereotype.Repository;

/**
 * @author Effet
 */
@Repository
public class BackCategoryPermDao extends SqlSessionDaoSupport {

    private static String NAMESPACE = "BackCategoryPerm.";

    public Optional<Long> create(BackCategoryPerm perm) {
        if (perm == null) {
            return Optional.absent();
        }
        int cnt = getSqlSession().insert(NAMESPACE + "create", perm);
        if (cnt <= 0) {
            return Optional.absent();
        }
        return Optional.of(perm.getId());
    }

    public boolean update(BackCategoryPerm perm) {
        if (perm == null || perm.getId() == null) {
            return false;
        }
        int cnt = getSqlSession().insert(NAMESPACE + "update", perm);
        return cnt >= 1;
    }

    public Optional<BackCategoryPerm> get(Long id) {
        if (id == null || id <= 0) {
            return Optional.absent();
        }
        return Optional.fromNullable(getSqlSession().<BackCategoryPerm>selectOne(NAMESPACE + "get", id));
    }

    public Optional<BackCategoryPerm> findOne(Long userId, Integer userType) {
        if (userId == null || userType == null) {
            return Optional.absent();
        }
        BackCategoryPerm criteria = new BackCategoryPerm();
        criteria.setUserId(userId);
        criteria.setUserType(userType);
        return Optional.fromNullable(getSqlSession().<BackCategoryPerm>selectOne(NAMESPACE + "search", criteria));
    }
}
