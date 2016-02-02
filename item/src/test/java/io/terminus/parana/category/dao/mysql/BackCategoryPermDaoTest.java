/*
 * Copyright (c) 2015 杭州端点网络科技有限公司
 */

package io.terminus.parana.category.dao.mysql;

import com.google.common.base.Predicate;
import io.terminus.parana.category.model.BackCategoryPerm;
import io.terminus.parana.item.dao.BaseDaoTest;
import io.terminus.parana.user.model.User;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

import static org.junit.Assert.assertTrue;

/**
 * @author Effet
 */
public class BackCategoryPermDaoTest extends BaseDaoTest {

    @Autowired
    private BackCategoryPermDao backCategoryPermDao;

    private BackCategoryPerm mock() {
        BackCategoryPerm perm = new BackCategoryPerm();
        perm.setUserId(1l);
        perm.setUserType(User.TYPE.ADMIN.toNumber());
        perm.setAllow("0,1,2");
        perm.setDeny("2");
        perm.setBrandAllow("1,3,7");
        return perm;
    }

    private Predicate<BackCategoryPerm> getChecker(final BackCategoryPerm criteria) {
        return new Predicate<BackCategoryPerm>() {
            @Override
            public boolean apply(BackCategoryPerm input) {
                return Objects.equals(criteria.getUserId(), input.getUserId()) &&
                        Objects.equals(criteria.getUserType(), input.getUserType()) &&
                        Objects.equals(criteria.getAllow(), input.getAllow()) &&
                        Objects.equals(criteria.getDeny(), input.getDeny()) &&
                        Objects.equals(criteria.getBrandAllow(), input.getBrandAllow());
            }
        };
    }

    @Test
    public void testCreate() throws Exception {
        Long id = backCategoryPermDao.create(mock()).get();
        assertTrue(getChecker(mock()).apply(backCategoryPermDao.get(id).get()));
    }

    @Test
    public void testUpdate() throws Exception {
        Long id = backCategoryPermDao.create(mock()).get();

        BackCategoryPerm toUpdate = new BackCategoryPerm();
        toUpdate.setId(id);
        toUpdate.setAllow(mock().getAllow() + ",3");
        toUpdate.setDeny(mock().getDeny() + ",4");
        toUpdate.setBrandAllow(mock().getBrandAllow() + ",9");

        assertTrue(backCategoryPermDao.update(toUpdate));

        BackCategoryPerm criteria = mock();
        criteria.setAllow(toUpdate.getAllow());
        criteria.setDeny(toUpdate.getDeny());
        criteria.setBrandAllow(toUpdate.getBrandAllow());
        assertTrue(getChecker(criteria).apply(backCategoryPermDao.get(id).get()));
    }

    @Test
    public void testGet() throws Exception {
        Long id = backCategoryPermDao.create(mock()).get();
        assertTrue(getChecker(mock()).apply(backCategoryPermDao.get(id).get()));
    }

    @Test
    public void testFindOne() throws Exception {
        int acc = 0;
        for (Long userId : new Long[]{-1l, 0l, 1l, 2l}) {
            for (User.TYPE type : User.TYPE.values()) {
                ++ acc;
                BackCategoryPerm toCreate = mock();
                toCreate.setUserId(userId);
                toCreate.setUserType(type.toNumber());
                toCreate.setAllow(toCreate.getAllow() + acc);
                toCreate.setDeny(toCreate.getDeny() + acc);
                toCreate.setBrandAllow(toCreate.getBrandAllow() + acc);
                assertTrue(backCategoryPermDao.create(toCreate).isPresent());
            }
        }

        acc = 0;
        for (Long userId : new Long[]{-1l, 0l, 1l, 2l}) {
            for (User.TYPE type : User.TYPE.values()) {
                ++ acc;
                BackCategoryPerm toCreate = mock();
                toCreate.setUserId(userId);
                toCreate.setUserType(type.toNumber());
                toCreate.setAllow(toCreate.getAllow() + acc);
                toCreate.setDeny(toCreate.getDeny() + acc);
                toCreate.setBrandAllow(toCreate.getBrandAllow() + acc);

                assertTrue(getChecker(toCreate).apply(backCategoryPermDao.findOne(userId, type.toNumber()).get()));
            }
        }
    }
}