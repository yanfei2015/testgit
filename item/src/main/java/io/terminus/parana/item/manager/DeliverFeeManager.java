/*
 * <!--
 *   ~ Copyright (c) 2014 杭州端点网络科技有限公司
 *   -->
 */

package io.terminus.parana.item.manager;

import io.terminus.parana.item.dao.mysql.DeliverFeeTemplateDao;
import io.terminus.parana.item.dao.mysql.SpecialDeliverDao;
import io.terminus.parana.item.model.DeliverFeeTemplate;
import io.terminus.parana.item.model.SpecialDeliver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by yangzefeng on 15/5/8
 */
@Component
public class DeliverFeeManager {

    @Autowired
    private DeliverFeeTemplateDao deliverFeeTemplateDao;

    @Autowired
    private SpecialDeliverDao specialDeliverDao;

    @Transactional
    public void createSpecialDeliver(SpecialDeliver specialDeliver) {
        specialDeliverDao.create(specialDeliver);
        updateTemplate(specialDeliver.getDeliverFeeTemplateId());
    }

    @Transactional
    public void updateSpecialDeliver(SpecialDeliver specialDeliver) {
        specialDeliverDao.update(specialDeliver);
        updateTemplate(specialDeliver.getDeliverFeeTemplateId());
    }

    @Transactional
    public void deleteSpecialDeliver(SpecialDeliver specialDeliver) {
        specialDeliverDao.delete(specialDeliver.getId());
        updateTemplate(specialDeliver.getDeliverFeeTemplateId());
    }

    /**
     * 更新运费模板的更新时间
     */
    private void updateTemplate(Long templateId) {
        DeliverFeeTemplate template = new DeliverFeeTemplate();
        template.setId(templateId);
        deliverFeeTemplateDao.update(template);
    }

    @Transactional
    public void makeDefault(Long templateId, Long userId) {
        deliverFeeTemplateDao.unDefault(userId);
        DeliverFeeTemplate template = new DeliverFeeTemplate();
        template.setId(templateId);
        template.setIsDefault(true);
        deliverFeeTemplateDao.update(template);
    }
}
