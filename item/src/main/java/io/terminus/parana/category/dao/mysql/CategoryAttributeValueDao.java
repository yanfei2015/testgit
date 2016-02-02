package io.terminus.parana.category.dao.mysql;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import io.terminus.parana.category.model.CategoryAttributeValue;
import org.mybatis.spring.support.SqlSessionDaoSupport;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * @author Effet
 */
@Repository
public class CategoryAttributeValueDao extends SqlSessionDaoSupport {

    public long create(CategoryAttributeValue value) {
        checkNotNull(value, "cant create category attr value, cause null");
        getSqlSession().insert("CategoryAttributeValue.create", value);
        return checkNotNull(value.getId(), "eventually id null");
    }

    public boolean deleteById(long id) {
        return getSqlSession().delete("CategoryAttributeValue.deleteById", id) == 1;
    }

    public Optional<CategoryAttributeValue> findById(long id) {
        CategoryAttributeValue value = getSqlSession().selectOne("CategoryAttributeValue.findById", id);
        return Optional.fromNullable(value);
    }

    private List<CategoryAttributeValue> findBy(CategoryAttributeValue criteria) {
        if (criteria == null) {
            return ImmutableList.of();
        }
        List<CategoryAttributeValue> result =
                getSqlSession().selectList("CategoryAttributeValue.findByCriteria", criteria);
        return checkNotNull(result, "eventually result null");
    }

    public List<CategoryAttributeValue> findBy(long categoryId, long keyId) {
        CategoryAttributeValue criteria = new CategoryAttributeValue();
        criteria.setCategoryId(categoryId);
        criteria.setKeyId(keyId);
        return findBy(criteria);
    }

    public Optional<CategoryAttributeValue> findBy(long categoryId, long keyId, long valueId) {
        CategoryAttributeValue criteria = new CategoryAttributeValue();
        criteria.setCategoryId(categoryId);
        criteria.setKeyId(keyId);
        criteria.setValueId(valueId);
        List<CategoryAttributeValue> values = findBy(criteria);
        checkState(values.size() <= 1,
                "multi value exist, categoryId=%s, keyId=%s, valueId=%s", categoryId, keyId, valueId);
        return Optional.fromNullable(Iterables.getFirst(values, null));
    }
}
