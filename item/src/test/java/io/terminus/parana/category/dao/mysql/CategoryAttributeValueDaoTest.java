package io.terminus.parana.category.dao.mysql;

import io.terminus.parana.category.model.CategoryAttributeValue;
import io.terminus.parana.item.dao.BaseDaoTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Effet
 */
public class CategoryAttributeValueDaoTest extends BaseDaoTest {

    @Autowired
    private CategoryAttributeValueDao categoryAttributeValueDao;

    private CategoryAttributeValue mock() {
        CategoryAttributeValue value = new CategoryAttributeValue();
        value.setCategoryId(1l);
        value.setKeyId(1l);
        value.setValueId(1l);
        value.setValue("red");
        value.setLogo("red logo");
        return value;
    }

    @Test
    public void testCreate() throws Exception {
        CategoryAttributeValue value = mock();
        long id = categoryAttributeValueDao.create(value);
        assertTrue(id > 0);

        CategoryAttributeValue exist = categoryAttributeValueDao.findById(id).get();

        assertEquals(value.getCategoryId(), exist.getCategoryId());
        assertEquals(value.getKeyId(), exist.getKeyId());
        assertEquals(value.getValueId(), exist.getValueId());
        assertEquals(value.getValue(), exist.getValue());
        assertEquals(value.getLogo(), exist.getLogo());
    }

    @Test
    public void testDeleteById() throws Exception {
        CategoryAttributeValue value = mock();
        long id = categoryAttributeValueDao.create(value);
        assertTrue(categoryAttributeValueDao.findById(id).isPresent());
        categoryAttributeValueDao.deleteById(id);
        assertFalse(categoryAttributeValueDao.findById(id).isPresent());
    }

    @Test
    public void testFindBy() throws Exception {
        CategoryAttributeValue[] values = new CategoryAttributeValue[8];
        for (int i=0; i<8; ++i) {
            values[i] = new CategoryAttributeValue();
            values[i].setCategoryId((long) ((i >> 2) & 1));
            values[i].setKeyId((long) ((i >> 1) & 1));
            long valueId = i & 1;
            values[i].setValueId(valueId);
            values[i].setValue("value " + valueId);
            values[i].setLogo("logo " + valueId);
            categoryAttributeValueDao.create(values[i]);
        }
        for (int i=0; i<2; ++i) {
            for (int j=0; j<2; ++j) {
                List<CategoryAttributeValue> vs = categoryAttributeValueDao.findBy(i, j);
                assertEquals(2, vs.size());
                assertNotEquals(vs.get(0).getValueId().intValue(), vs.get(1).getValueId().intValue());
                if (vs.get(0).getValueId() > vs.get(1).getValueId()) {
                    Collections.swap(vs, 0, 1);
                }
                for (int k=0; k<2; ++k) {
                    CategoryAttributeValue v = categoryAttributeValueDao.findBy(i, j, k).get();
                    assertEquals(vs.get(k).getValueId(), v.getValueId());
                    assertEquals(vs.get(k).getValue(), v.getValue());
                    assertEquals(vs.get(k).getLogo(), v.getLogo());
                }
            }
        }
    }
}