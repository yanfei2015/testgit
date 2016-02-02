package io.terminus.parana.category.dao.mysql;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import io.terminus.parana.category.model.CategoryAttributeKey;
import org.mybatis.spring.support.SqlSessionDaoSupport;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * @author Effet
 */
@Repository
public class CategoryAttributeKeyDao extends SqlSessionDaoSupport {

    public long create(CategoryAttributeKey key) {
        checkNotNull(key, "cant create category attr key, cause null");
        getSqlSession().insert("CategoryAttributeKey.create", key);
        return checkNotNull(key.getId(), "eventually id null");
    }

    public boolean deleteById(long id) {
        return getSqlSession().delete("CategoryAttributeKey.deleteById", id) == 1;
    }

    public Optional<CategoryAttributeKey> findById(long id) {
        CategoryAttributeKey key = getSqlSession().selectOne("CategoryAttributeKey.findById", id);
        return Optional.fromNullable(key);
    }

    private List<CategoryAttributeKey> findBy(CategoryAttributeKey criteria) {
        if (criteria == null) {
            return ImmutableList.of();
        }
        List<CategoryAttributeKey> result =
                getSqlSession().selectList("CategoryAttributeKey.findByCriteria", criteria);
        return checkNotNull(result, "eventually result null");
    }

    public List<CategoryAttributeKey> findBy(long categoryId) {
        CategoryAttributeKey criteria = new CategoryAttributeKey();
        criteria.setCategoryId(categoryId);
        return findBy(criteria);
    }

    public Optional<CategoryAttributeKey> findBy(long categoryId, long keyId) {
        CategoryAttributeKey criteria = new CategoryAttributeKey();
        criteria.setCategoryId(categoryId);
        criteria.setKeyId(keyId);
        List<CategoryAttributeKey> result = findBy(criteria);
        checkState(result.size() <= 1,
                "multi category attr key exist, categoryId=%s, keyId=%s", categoryId, keyId);
        return Optional.fromNullable(Iterables.getFirst(result, null));
    }
}
