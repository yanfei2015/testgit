/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package io.terminus.parana.category.manager;

import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.pampas.common.Response;
import io.terminus.parana.category.dao.mysql.BackCategoryDao;
import io.terminus.parana.category.dao.mysql.CategoryBindingDao;
import io.terminus.parana.category.dao.mysql.FrontCategoryDao;
import io.terminus.parana.category.dao.mysql.SpuDao;
import io.terminus.parana.category.dao.redis.AttributeIndexDao;
import io.terminus.parana.category.dao.redis.SpuAttributeDao;
import io.terminus.parana.category.dto.AttributeDto;
import io.terminus.parana.category.dto.CatImportData;
import io.terminus.parana.category.model.*;
import io.terminus.parana.category.service.CategoryWriteServiceImpl;
import io.terminus.parana.common.util.Iters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * 类目管理
 * Author: haolin
 * On: 8/29/14
 */
@Slf4j
@Component
public class CategoryManager {

    @Autowired
    private BackCategoryDao backCategoryDao;

    @Autowired
    private FrontCategoryDao frontCategoryDao;

    @Autowired
    private SpuDao spuDao;

    @Autowired
    private AttributeIndexDao attributeIndexDao;

    @Autowired
    private SpuAttributeDao spuAttributeDao;

    @Autowired
    private CategoryBindingDao categoryBindingDao ;

    /**
     * 创建后台类目及更新其父类目hasChildren
     * @param category 后台类目
     */
    @Transactional
    public void create(BackCategory category){
        BackCategory parent = backCategoryDao.load(category.getPid());
        // category.level = parent.level+1
        category.setLevel(parent.getLevel() + 1);
        checkState(backCategoryDao.create(category), "back.category.persist.fail");
        backCategoryDao.setHasChildren(category.getPid(), Boolean.TRUE);
    }

    /**
     * 创建前台类目及更新其父类目hasChildren
     * @param category 后台类目
     */
    @Transactional
    public void create(FrontCategory category){
        FrontCategory parent = frontCategoryDao.load(category.getPid());
        // category.level = parent.level+1
        category.setLevel(parent.getLevel() + 1);
        checkState(frontCategoryDao.create(category), "front.category.persist.fail");
        frontCategoryDao.setHasChildren(category.getPid(), Boolean.TRUE);
    }

    /**
     * 禁用后台类目, 需更新父级类目hasChildren=false
     * @param category 后台类目
     */
    @Transactional
    public void disable(BackCategory category) {
        BackCategory criteria = new BackCategory();
        criteria.setPid(category.getPid());
        criteria.setStatus(BackCategory.Status.ENABLED.value());
        List<BackCategory> brothers = backCategoryDao.list(criteria);
        if (brothers.size() == 1){
            // 有且仅有改子类目, 设置其父类目hasChildren=false
            backCategoryDao.setHasChildren(category.getPid(), Boolean.FALSE);
        }
        backCategoryDao.setStatus(category.getId(), BackCategory.Status.DISABLED.value());

    }

    /**
     * 启用后台类目, 需要更新父级类目hasChildren=true
     * @param category 后台类目
     */
    @Transactional
    public void enable(BackCategory category) {
        backCategoryDao.setStatus(category.getId(), BackCategory.Status.ENABLED.value());
        backCategoryDao.setHasChildren(category.getPid(), Boolean.TRUE);
    }

    /**
     * 删除前台类目, 需要更新父级类目hasChildren=false
     * @param category 前台类目
     */
    @Transactional
    public void delete(FrontCategory category) {
        FrontCategory criteria = new FrontCategory();
        criteria.setPid(category.getPid());
        List<FrontCategory> brothers = frontCategoryDao.list(criteria);
        if (brothers.size() == 1){
            // 有且仅有该子类目, 设置其父类目hasChildren=false
            frontCategoryDao.setHasChildren(category.getPid(), Boolean.FALSE);
        }
        frontCategoryDao.delete(category.getId());
    }

    /**
     * 启用SPU, 需设置后台叶子类目hasSpu=true
     * @param spu SPU
     */
    public void enable(Spu spu){
        spuDao.setStatus(spu.getId(), Spu.Status.ENABLED.value());
        backCategoryDao.setHasSpu(spu.getCategoryId(), Boolean.TRUE);
    }

    /**
     * 创建SPU，并设置其后台叶子类目hasSpu=true
     * @param spu SPU
     */
    public void create(Spu spu) {
        checkState(spuDao.create(spu), "spu.persist.fail");
        backCategoryDao.setHasSpu(spu.getCategoryId(), Boolean.TRUE);
    }

    @Transactional
    public long create(Spu spu, List<AttributeDto> skuAttrs, List<SpuAttribute> spuAttrs) throws ServiceException {
        Spu criteria = new Spu();
        criteria.setCategoryId(checkNotNull(spu.getCategoryId()));
        criteria.setName(checkNotNull(spu.getName()));
        // 该类目下是否存在name的SPU
        Spu exist = spuDao.findBy(criteria);

        long spuId;
        if (exist == null) {
            // 创建SPU并设置后台叶子类目hasSpu=true
            spu.setStatus(Spu.Status.ENABLED.value());
            create(spu);
            spuId = spu.getId();
        } else {
            if (Spu.isEnable(exist)) {
                log.warn("spu({}) is existed.", exist);
                throw new ServiceException("spu.is.existed");
            } else {
                // 直接启用SPU
                enable(exist);
                spuId = exist.getId();
            }
        }

        // 销售属性
        checkState(skuAttrs != null && skuAttrs.size() > 0 && skuAttrs.size() <= 2,
                "only 1~2 sku attrs allowed");
        attributeIndexDao.addSkuKeys(spuId, skuAttrs);

        // spu 属性
        if (Iters.emptyToNull(spuAttrs) != null) {
            for (SpuAttribute spuAttr : spuAttrs) {
                spuAttr.setSpuId(spuId);
            }
            spuAttributeDao.create(spuId, spuAttrs);
        }

        return spuId;
    }

    /**
     * 禁用SPU, 更新叶子类目hasSpu=false
     * @param spu SPU
     */
    public void disable(Spu spu) {
        Spu criteria = new Spu();
        criteria.setCategoryId(spu.getCategoryId());
        criteria.setStatus(Spu.Status.ENABLED.value());
        List<Spu> brothers = spuDao.list(criteria);
        if (brothers.size() == 1){
            // 有且仅有该SPU, 修改其叶子类目hasSpu=false
            backCategoryDao.setHasSpu(spu.getCategoryId(), Boolean.FALSE);
        }
        spuDao.setStatus(spu.getId(), Spu.Status.DISABLED.value());
    }

    @Transactional
    public long createByPath(String[] categories) {
        long pid = 0l;
        boolean end = false;
        int lv = 1;
        for (String category : categories) {
            if (end) {
                throw new ServiceException("category.create.fail.has.spu");
            }
            BackCategory bc = backCategoryDao.findByNameAndPid(pid, category);
            if (bc == null) {
                bc = new BackCategory();
                bc.setName(category);
                bc.setStatus(BackCategory.Status.ENABLED.value());
                bc.setHasSpu(false);
                bc.setHasChildren(false);
                bc.setPid(pid);
                bc.setLevel(lv);
                backCategoryDao.create(bc);
                if (pid != 0) {
                    backCategoryDao.setHasChildren(pid, true);
                }
            }
            if (!BackCategory.isEnable(bc)) {
                backCategoryDao.setStatus(bc.getId(), BackCategory.Status.ENABLED.value());
                backCategoryDao.setHasChildren(pid, Boolean.TRUE);
            }
            pid = bc.getId();
            end = bc.getHasSpu();
            ++ lv;
        }
        return pid;
    }

    @Transactional
    public long createFontCatByPath (String[] fontCategories,String[] backCategories){
        long pid = 0l ;
        int level = 1 ;
        for (String category: fontCategories){
            FrontCategory fc = frontCategoryDao.findByNameAndPid(pid,category);
            if (fc == null){
                fc = new FrontCategory();
                fc.setPid(pid);
                fc.setName(category);
                fc.setLevel(level);
                fc.setHasChildren(false);
                frontCategoryDao.create(fc);
                if (pid != 0){
                    frontCategoryDao.setHasChildren(pid,true);
                }
            }
            pid = fc.getId();
            ++ level;
        }
        long backId = 0l;
        int lv = 1;
        for (String backCategory : backCategories){
            BackCategory bc = backCategoryDao.findByNameAndPid(backId,backCategory);
            if (bc == null) {
//                bc = new BackCategory();
//                bc.setName(backCategory);
//                bc.setStatus(BackCategory.Status.ENABLED.value());
//                bc.setHasSpu(false);
//                bc.setHasChildren(false);
//                bc.setPid(backId);
//                bc.setLevel(lv);
//                backCategoryDao.create(bc);
//                if (pid != 0) {
//                    backCategoryDao.setHasChildren(pid, true);
//                }
                log.error("back category is not exist");
                throw new ServiceException("category.not.exist");
            } else {
                backId = bc.getId();
            }
            ++ lv;
        }
        //binding
        CategoryBinding categoryBinding = new CategoryBinding();
        categoryBinding.setFrontCategoryId(pid);
        categoryBinding.setBackCategoryId(backId);
        CategoryBinding exist  = categoryBindingDao.findByFrontBackCategoryId(pid,backId);
        if (exist == null){
            categoryBindingDao.create(categoryBinding);
        }  else {
            log.error("category binding is exists");
            throw new ServiceException("category.binding.is.exist");
        }
        return pid;
    }
}
