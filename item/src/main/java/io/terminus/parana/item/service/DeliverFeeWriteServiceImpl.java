/*
 * <!--
 *   ~ Copyright (c) 2014 杭州端点网络科技有限公司
 *   -->
 */

package io.terminus.parana.item.service;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.pampas.common.Response;
import io.terminus.parana.item.dao.mysql.DeliverFeeTemplateDao;
import io.terminus.parana.item.dao.mysql.ShipFeeDao;
import io.terminus.parana.item.dao.mysql.SpecialDeliverDao;
import io.terminus.parana.item.manager.DeliverFeeManager;
import io.terminus.parana.item.model.DeliverFeeTemplate;
import io.terminus.parana.item.model.DeliverRule;
import io.terminus.parana.item.model.SpecialDeliver;
import io.terminus.parana.user.dto.AddressForDisplay;
import io.terminus.parana.user.dto.LoginUser;
import io.terminus.parana.user.model.Address;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.terminus.common.utils.Arguments.isNull;

/**
 * Created by yangzefeng on 15/4/28
 */
@Service @Slf4j
public class DeliverFeeWriteServiceImpl implements DeliverFeeWriteService {

    @Autowired
    private DeliverFeeTemplateDao deliverFeeTemplateDao;

    @Autowired
    private SpecialDeliverDao specialDeliverDao;

    @Autowired
    private ShipFeeDao shipFeeDao;

    @Autowired
    private DeliverFeeManager deliverFeeManager;

    private final static JsonMapper JSON_MAPPER = JsonMapper.JSON_NON_DEFAULT_MAPPER;

    @Override
    public Response<Long> createTemplate(LoginUser loginUser, DeliverFeeTemplate deliverFeeTemplate) {
        Response<Long> result = new Response<>();

        try {
            checkDeliverTemplateFields(deliverFeeTemplate);
            DeliverFeeTemplate defaultTemplate = deliverFeeTemplateDao.findDefaultByUserId(loginUser.getId());
            if(isNull(defaultTemplate)) {
                deliverFeeTemplate.setIsDefault(true);
            }else {
                deliverFeeTemplate.setIsDefault(false);
            }
            deliverFeeTemplateDao.create(deliverFeeTemplate);
            result.setResult(deliverFeeTemplate.getId());
        }catch (Exception e) {
            log.error("fail to create deliver fee template by {}, cause:{}",
                    deliverFeeTemplate, Throwables.getStackTraceAsString(e));
            result.setError("deliver.fee.template.create.fail");
        }
        return result;
    }

    @Override
    public Response<Long> createSpecial(LoginUser loginUser, SpecialDeliver specialDeliver) {
        Response<Long> result = new Response<>();

        try {
            checkDeliverSpecialFields(specialDeliver);
            formatAddress(specialDeliver);
            checkSpecialTemplateId(loginUser, specialDeliver);
            deliverFeeManager.createSpecialDeliver(specialDeliver);
            result.setResult(specialDeliver.getId());
        }catch (ServiceException se) {
            result.setError(se.getMessage());
        }catch (Exception e) {
            log.error("fail to create special by {}, cause:{}",
                    specialDeliver, Throwables.getStackTraceAsString(e));
            result.setError("special.deliver.create.fail");
        }
        return result;
    }

    @Override
    public Response<Boolean> updateTemplate(LoginUser loginUser, DeliverFeeTemplate deliverFeeTemplate) {
        Response<Boolean> result = new Response<>();

        try {
            DeliverFeeTemplate exist = checkTemplateExist(deliverFeeTemplate.getId());
            checkTemplateAuth(loginUser, exist);
            checkDeliverTemplateFields(deliverFeeTemplate);
            deliverFeeTemplateDao.update(deliverFeeTemplate);
            result.setResult(Boolean.TRUE);
        }catch (ServiceException se) {
            result.setError(se.getMessage());
        }catch (Exception e) {
            log.error("fail to update template by {}, cause:{}", deliverFeeTemplate, Throwables.getStackTraceAsString(e));
            result.setError("deliver.fee.template.update.fail");
        }
        return result;
    }

    @Override
    public Response<Boolean> updateSpecial(LoginUser loginUser, SpecialDeliver specialDeliver) {
        Response<Boolean> result = new Response<>();

        try {
            SpecialDeliver exist = checkSpecialExist(specialDeliver.getId());
            checkSpecialAuth(loginUser, exist);
            checkSpecialTemplateId(loginUser, exist);
            checkDeliverSpecialFields(specialDeliver);
            formatAddress(specialDeliver);
            deliverFeeManager.updateSpecialDeliver(specialDeliver);
            result.setResult(Boolean.TRUE);
        }catch (ServiceException se) {
            result.setError(se.getMessage());
        }catch (Exception e) {
            log.error("fail to update special deliver by {}, cause:{}",
                    specialDeliver, Throwables.getStackTraceAsString(e));
            result.setError("special.deliver.update.fail");
        }
        return result;
    }

    private void formatAddress(SpecialDeliver specialDeliver) {
        List<Integer> addresses = new ArrayList<>();
        List<AddressForDisplay> addressForDisplays =
                JSON_MAPPER.fromJson(specialDeliver.getAddressForDisplay(), JSON_MAPPER.createCollectionType(List.class, AddressForDisplay.class));
        for(AddressForDisplay addressForDisplay : addressForDisplays) {
            if(Arguments.isNullOrEmpty(addressForDisplay.getChildren())) {
                addresses.add(addressForDisplay.getParent().getId());
            }else {
                for(Address child : addressForDisplay.getChildren()) {
                    addresses.add(child.getId());
                }
            }
        }
        specialDeliver.setAddresses(JSON_MAPPER.toJson(addresses));
    }

    @Override
    @Transactional
    public Response<Boolean> deleteTemplate(LoginUser loginUser, Long templateId) {
        Response<Boolean> result = new Response<>();

        try {
            DeliverFeeTemplate template = checkTemplateExist(templateId);
            checkTemplateAuth(loginUser, template);
            deliverFeeTemplateDao.delete(templateId);
            List<SpecialDeliver> specialDelivers = specialDeliverDao.findByTemplateIds(templateId);
            if(!isNull(specialDelivers)&&!Objects.equals(specialDelivers.size(),0)) {
                specialDeliverDao.deletes(Lists.transform(specialDelivers, new Function<SpecialDeliver, Long>() {
                    @Override
                    public Long apply(SpecialDeliver input) {
                        return input.getId();
                    }
                }));
            }
            //delete ship fee bind with this template
            shipFeeDao.deleteByTemplateId(templateId);
            result.setResult(Boolean.TRUE);
        }catch (ServiceException se) {
            result.setError(se.getMessage());
        }catch (Exception e) {
            log.error("fail to delete template by template id {}, cause:{}",
                    templateId, Throwables.getStackTraceAsString(e));
            result.setError("deliver.template.delete.fail");
        }
        return result;
    }

    @Override
    public Response<Boolean> deleteSpecial(LoginUser loginUser, Long specialId) {
        Response<Boolean> result = new Response<>();

        try {
            SpecialDeliver specialDeliver = checkSpecialExist(specialId);
            checkSpecialAuth(loginUser, specialDeliver);
            deliverFeeManager.deleteSpecialDeliver(specialDeliver);
            result.setResult(Boolean.TRUE);
        }catch (ServiceException se) {
            result.setError(se.getMessage());
        }catch (Exception e) {
            log.error("fail to delete special deliver by id {}, cause:{}",
                    specialId, Throwables.getStackTraceAsString(e));
            result.setError("special.deliver.id.invalid");
        }
        return result;
    }

    @Override
    public Response<Boolean> makeDefault(LoginUser loginUser, Long templateId) {
        Response<Boolean> result = new Response<>();

        try {
            DeliverFeeTemplate exist = checkTemplateExist(templateId);
            checkTemplateAuth(loginUser, exist);
            deliverFeeManager.makeDefault(templateId, loginUser.getId());
            result.setResult(Boolean.TRUE);
        }catch (Exception e) {
            log.error("fail to make default deliver template by id {}, cause:{}",
                    templateId, Throwables.getStackTraceAsString(e));
            result.setError("make.deliver.template.fail");
        }
        return result;
    }

    private DeliverFeeTemplate checkTemplateExist(Long id) {
        DeliverFeeTemplate exist = deliverFeeTemplateDao.load(id);
        if(isNull(exist)) {
            throw new ServiceException("deliver.template.not.exist");
        }
        return exist;
    }

    private SpecialDeliver checkSpecialExist(Long id) {
        SpecialDeliver exist = specialDeliverDao.load(id);
        if(isNull(exist)) {
            throw new ServiceException("special.deliver.not.exist");
        }
        return exist;
    }

    private void checkTemplateAuth(LoginUser loginUser, DeliverFeeTemplate exist) {
        if(!Arguments.equalWith(loginUser.getId(), exist.getUserId())) {
            throw new ServiceException("authorize.fail");
        }
    }

    private void checkSpecialAuth(LoginUser loginUser, SpecialDeliver exist) {
        if(!Arguments.equalWith(loginUser.getId(), exist.getUserId())) {
            throw new ServiceException("authorize.fail");
        }
    }

    private void checkSpecialTemplateId(LoginUser loginUser, SpecialDeliver exist) {
        DeliverFeeTemplate template = deliverFeeTemplateDao.load(exist.getDeliverFeeTemplateId());
        if(!Arguments.equalWith(template.getUserId(), loginUser.getId())) {
            throw new ServiceException("template.id.illegal");
        }
    }

    private void checkDeliverRuleFields(DeliverRule deliverRule) {
        if (!isNull(deliverRule.getHighPrice())
                && !isNull(deliverRule.getLowPrice())) {
            if (deliverRule.getHighPrice() < deliverRule.getLowPrice()) {
                throw new ServiceException("deliver.rule.price.invalid");
            }
        }
    }

    private void checkDeliverTemplateFields(DeliverFeeTemplate deliverRule) {
        if (!isNull(deliverRule.getHighPrice())
                && !isNull(deliverRule.getLowPrice())) {
            if (deliverRule.getHighPrice() < deliverRule.getLowPrice()) {
                throw new ServiceException("deliver.rule.price.invalid");
            }
        }
    }

    private void checkDeliverSpecialFields(SpecialDeliver deliverRule) {
        if (!isNull(deliverRule.getHighPrice())
                && !isNull(deliverRule.getLowPrice())) {
            if (deliverRule.getHighPrice() < deliverRule.getLowPrice()) {
                throw new ServiceException("deliver.rule.price.invalid");
            }
        }
    }

    @Override
    public Response<Long> createDefault(Long userId) {
        Response<Long> result = new Response<>();

        try {
            DeliverFeeTemplate deliverFeeTemplate = new DeliverFeeTemplate();
            deliverFeeTemplate.setUserId(userId);
            deliverFeeTemplate.setName("默认运费模板");
            deliverFeeTemplate.setMethod(DeliverFeeTemplate.Method.NORMAL.value());
            deliverFeeTemplate.setIsDefault(Boolean.TRUE);
            deliverFeeTemplate.setValuation(DeliverRule.Valuation.DETERMINATE.value());
            deliverFeeTemplate.setFee(0);
            deliverFeeTemplateDao.create(deliverFeeTemplate);

            result.setResult(deliverFeeTemplate.getId());
        }catch (Exception e) {
            log.error("fail to create default deliver fee template by user id {}, cause:{}",
                    userId, Throwables.getStackTraceAsString(e));
            result.setError("create.default.template.fail");
        }
        return result;
    }
}
