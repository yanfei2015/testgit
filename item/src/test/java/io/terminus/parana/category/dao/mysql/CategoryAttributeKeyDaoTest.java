package io.terminus.parana.category.dao.mysql;

import io.terminus.parana.category.model.CategoryAttributeKey;
import io.terminus.parana.item.dao.BaseDaoTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Effet
 */
public class CategoryAttributeKeyDaoTest extends BaseDaoTest {

    @Autowired
    private CategoryAttributeKeyDao categoryAttributeKeyDao;

    private CategoryAttributeKey mock() {
        CategoryAttributeKey key = new CategoryAttributeKey();
        key.setCategoryId(1l);
        key.setKeyId(1l);
        key.setKeyName("key 1");
        key.setKeyType(CategoryAttributeKey.KeyType.ENUM.value());
        return key;
    }

    @Test
    public void testCreate() throws Exception {
        CategoryAttributeKey key = mock();
        long id = categoryAttributeKeyDao.create(key);
        assertTrue(id > 0);

        CategoryAttributeKey exist = categoryAttributeKeyDao.findById(id).get();
        // test all keys
        assertEquals(key.getCategoryId(), exist.getCategoryId());
        assertEquals(key.getKeyId(), exist.getKeyId());
        assertEquals(key.getKeyName(), exist.getKeyName());
        assertEquals(key.getKeyType(), exist.getKeyType());
    }

    @Test
    public void testDeleteById() throws Exception {
        CategoryAttributeKey key = mock();
        long id = categoryAttributeKeyDao.create(key);
        assertTrue(categoryAttributeKeyDao.findById(id).isPresent());
        categoryAttributeKeyDao.deleteById(id);
        assertFalse(categoryAttributeKeyDao.findById(id).isPresent());
    }

    private CategoryAttributeKey.KeyType other(CategoryAttributeKey.KeyType has) {
        for (CategoryAttributeKey.KeyType keyType : CategoryAttributeKey.KeyType.values()) {
            if (keyType != has) {
                return keyType;
            }
        }
        fail();
        return null;
    }

    private CategoryAttributeKey.KeyType other(Integer has) {
        return other(CategoryAttributeKey.KeyType.from(has).orNull());
    }

    @Test
    public void testFindBy() throws Exception {
        CategoryAttributeKey[] keys = new CategoryAttributeKey[]{ mock(), mock() };
        keys[1].setKeyId(keys[1].getKeyId() + 1);
        keys[1].setKeyName(keys[1].getKeyName() + "...");
        keys[1].setKeyType(other(keys[1].getKeyType()).value());

        categoryAttributeKeyDao.create(keys[0]);
        categoryAttributeKeyDao.create(keys[1]);

        List<CategoryAttributeKey> all = categoryAttributeKeyDao.findBy(keys[0].getCategoryId());
        assertTrue(all.size() == 2);

        for (int i=0; i<2; ++i) {
            CategoryAttributeKey afterKey = categoryAttributeKeyDao.findBy(keys[i].getCategoryId(), keys[i].getKeyId()).get();
            assertEquals(keys[i].getKeyName(), afterKey.getKeyName());
        }
    }
}