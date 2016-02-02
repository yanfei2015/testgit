/*
 * <!--
 *   ~ Copyright (c) 2014 杭州端点网络科技有限公司
 *   -->
 */

package io.terminus.parana.item.service;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.pampas.common.Response;
import io.terminus.parana.item.dao.mysql.DeliverFeeTemplateDao;
import io.terminus.parana.item.dao.mysql.ItemDao;
import io.terminus.parana.item.dao.mysql.ShipFeeDao;
import io.terminus.parana.item.dao.mysql.SkuDao;
import io.terminus.parana.item.dao.mysql.SpecialDeliverDao;
import io.terminus.parana.item.dto.DeliverTemplateDto;
import io.terminus.parana.item.dto.SpecialDeliverDto;
import io.terminus.parana.item.model.DeliverFeeTemplate;
import io.terminus.parana.item.model.DeliverRule;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.ShipFee;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.item.model.SpecialDeliver;
import io.terminus.parana.user.dto.AddressForDisplay;
import io.terminus.parana.user.dto.LoginUser;
import io.terminus.parana.user.model.Address;
import io.terminus.parana.user.service.AddressReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by yangzefeng on 15/4/28
 */
@Service @Slf4j
public class DeliverFeeReadServiceImpl implements DeliverFeeReadService {

    @Autowired
    private DeliverFeeTemplateDao deliverFeeTemplateDao;

    @Autowired
    private SpecialDeliverDao specialDeliverDao;

    @Autowired
    private ShipFeeDao shipFeeDao;

    @Autowired
    private ItemDao itemDao;

    @Autowired
    private AddressReadService addressService;

    @Autowired
    private SkuDao skuDao;

    private static final JsonMapper JSON_NON_DEFAULT_MAPPER = JsonMapper.nonDefaultMapper();

    @Override
    public Response<Paging<DeliverTemplateDto>> deliverTemplatePaging(LoginUser loginUser,
                                                                      Integer pageNo, Integer size) {
        Response<Paging<DeliverTemplateDto>> result = new Response<>();

        try {
            Map<String, Object> params = composeParams(loginUser, pageNo, size);
            Paging<DeliverFeeTemplate> pagingTemplate = deliverFeeTemplateDao.paging(params);
            Paging<DeliverFeeTemplate> formatTemplate = putDefaultFirst(pagingTemplate);
            result.setResult(trans2TemplateDtos(formatTemplate));
        }catch (Exception e) {
            log.error("fail to query deliver template by user id {}, pageNo {}, size:{}, cause:{}",
                    loginUser.getId(), pageNo, size, Throwables.getStackTraceAsString(e));
            result.setError("deliver.template.query.fail");
        }
        return result;
    }

    @Override
    public Response<DeliverTemplateDto> findDeliverTemplateDetailById(Long templateId) {
        Response<DeliverTemplateDto> result = new Response<>();

        try {
            DeliverFeeTemplate template = deliverFeeTemplateDao.load(templateId);
            List<SpecialDeliver> specialDelivers = specialDeliverDao.findByTemplateIds(templateId);
            DeliverTemplateDto dto = new DeliverTemplateDto();
            dto.setDeliverFeeTemplate(template);
            dto.setSpecialDelivers(trans2SpecialDeliverDtos(specialDelivers));

            result.setResult(dto);
        }catch (Exception e) {
            log.error("fail to get deliver template detail by id {}, cause:{}",
                    templateId, Throwables.getStackTraceAsString(e));
            result.setError("deliver.template.query.fail");
        }
        return result;
    }

    @Override
    public Response<Paging<DeliverFeeTemplate>> findTemplateByUser(LoginUser loginUser, Integer pageNo, Integer size) {
        Response<Paging<DeliverFeeTemplate>> result = new Response<>();

        try {
            Map<String, Object> params = composeParams(loginUser, pageNo, size);
            Paging<DeliverFeeTemplate> pagingTemplate = deliverFeeTemplateDao.paging3(params);
            result.setResult(pagingTemplate);
        }catch (Exception e) {
            log.error("fail to find template by user id {}, cause:{}", loginUser.getId(), Throwables.getStackTraceAsString(e));
            result.setError("deliver.template.query.fail");
        }
        return result;
    }

    private Paging<DeliverFeeTemplate> putDefaultFirst(Paging<DeliverFeeTemplate> templatePaging) {
        Paging<DeliverFeeTemplate> format = new Paging<>();
        format.setTotal(templatePaging.getTotal());
        List<DeliverFeeTemplate> deliverFeeTemplates = new ArrayList<>();
        DeliverFeeTemplate defaultTemplate = findDefaultTemplate(templatePaging.getData());
        deliverFeeTemplates.add(defaultTemplate);
        for(DeliverFeeTemplate deliverFeeTemplate : templatePaging.getData()) {
            if(!Objects.equals(defaultTemplate.getId(), deliverFeeTemplate.getId())) {
                deliverFeeTemplates.add(deliverFeeTemplate);
            }
        }
        format.setData(deliverFeeTemplates);
        return format;
    }

    private DeliverFeeTemplate findDefaultTemplate(List<DeliverFeeTemplate> deliverFeeTemplates) {
        for(DeliverFeeTemplate template : deliverFeeTemplates) {
            if(template.getIsDefault()) {
                return template;
            }
        }
        return null;
    }

    private Map<String, Object> composeParams(LoginUser loginUser, Integer pageNo, Integer size) {
        Map<String, Object> param = PageInfo.of(pageNo, size).toMap();
        param.put("userId", loginUser.getId());
        return param;
    }

    private Paging<DeliverTemplateDto> trans2TemplateDtos(Paging<DeliverFeeTemplate> templatePaging) {
        Paging<DeliverTemplateDto> result = new Paging<>();
        result.setTotal(templatePaging.getTotal());

        List<Long> templateIds = getTemplateIds(templatePaging.getData());

        Map<Long, List<SpecialDeliver>> map = getSpecialDeliverMap(templateIds);

        List<DeliverTemplateDto> dtos = new ArrayList<>();
        for(DeliverFeeTemplate deliverFeeTemplate : templatePaging.getData()) {
            try {
                DeliverTemplateDto dto = new DeliverTemplateDto();

                List<SpecialDeliver> specialDelivers = map.get(deliverFeeTemplate.getId());

                dto.setDeliverFeeTemplate(deliverFeeTemplate);
                if(!Arguments.isNullOrEmpty(specialDelivers)) {
                    dto.setSpecialDelivers(trans2SpecialDeliverDtos(specialDelivers));
                }

                dtos.add(dto);
            }catch (Exception e) {
                log.error("fail to trans deliver fee template {} to dto, cause:{}, skip",
                        deliverFeeTemplate, Throwables.getStackTraceAsString(e));
            }
        }

        result.setData(dtos);
        return result;
    }


    public List<SpecialDeliverDto> trans2SpecialDeliverDtos(List<SpecialDeliver> specialDelivers) {
        List<SpecialDeliverDto> result = new ArrayList<>();
        for(SpecialDeliver specialDeliver : specialDelivers) {
            try {
                SpecialDeliverDto specialDeliverDto = new SpecialDeliverDto();
                specialDeliverDto.setSpecialDeliver(specialDeliver);
                List<AddressForDisplay> addressForDisplays =
                        JSON_NON_DEFAULT_MAPPER.fromJson(specialDeliver.getAddressForDisplay(), JSON_NON_DEFAULT_MAPPER.createCollectionType(List.class, AddressForDisplay.class));
                specialDeliverDto.setAddressForDisplays(addressForDisplays);
                result.add(specialDeliverDto);
            }catch (Exception e) {
                log.error("fail to tran special deliver to dto by {}, cause:{}, skip",
                        specialDeliver, Throwables.getStackTraceAsString(e));
            }
        }
        return result;
    }

    public List<Long> getTemplateIds(List<DeliverFeeTemplate> templates) {
        return Lists.transform(templates, new Function<DeliverFeeTemplate, Long>() {
            @Override
            public Long apply(DeliverFeeTemplate input) {
                return input.getId();
            }
        });
    }

    public Map<Long, List<SpecialDeliver>> getSpecialDeliverMap(Collection<Long> templateIds) {
        if(templateIds == null || templateIds.isEmpty()) {
            return new HashMap<>();
        }
        Map<Long, List<SpecialDeliver>> result = new HashMap<>();

        List<SpecialDeliver> specialDelivers =
                specialDeliverDao.findByTemplateIds(templateIds.toArray(new Long[templateIds.size()]));

        for(SpecialDeliver specialDeliver : specialDelivers) {
            List<SpecialDeliver> exist = result.get(specialDeliver.getDeliverFeeTemplateId());
            if(Arguments.isNullOrEmpty(exist)) {
                exist = new ArrayList<>();
                exist.add(specialDeliver);

            }else {
                exist.add(specialDeliver);
            }
            result.put(specialDeliver.getDeliverFeeTemplateId(), exist);
        }

        return result;
    }


    @Override
    public Response<Integer> getDeliverFeeByItemIdAndAddressId(Long itemId, Integer addressId, Integer quantity) {
        Response<Integer> result = new Response<>();

        try {
            result.setResult(calculateDeliverFee(itemId, addressId, quantity));
        }catch (Exception e) {
            log.error("fail to get deliver fee by item id {}, address id, quantity {}, cause:{}",
                    itemId, addressId, quantity, Throwables.getStackTraceAsString(e));
            result.setError("deliver.fee.calculate.fail");
        }
        return result;
    }

    private Integer calculateDeliverFee(Long itemId, Integer addressId, Integer quantity) {
        Item item = itemDao.load(itemId);
        if(Arguments.isNull(item)) {
            throw new ServiceException("item.not.found");
        }

        // 如果商品没有绑定运费模板，使用默认模板
        ShipFee shipFee = shipFeeDao.findByItemId(itemId);
        DeliverFeeTemplate deliverFeeTemplate;
        if(shipFee == null || shipFee.getDeliverFeeTemplateId() == null) {
            deliverFeeTemplate = deliverFeeTemplateDao.findDefaultByUserId(item.getUserId());
        }else {
            deliverFeeTemplate = deliverFeeTemplateDao.load(shipFee.getDeliverFeeTemplateId());
        }

        if(deliverFeeTemplate == null) {
            return 0;
        }

        List<SpecialDeliver> specialDelivers = specialDeliverDao.findByTemplateIds(deliverFeeTemplate.getId());
        Response<List<Address>> ancestorR = addressService.ancestorOfAddresses(addressId);
        if(!ancestorR.isSuccess()) {
            log.warn("fail to find ancestor of address id {} when calculate deliver fee", addressId);
            throw new ServiceException("address.query.fail");
        }
        List<Address> ancestors = ancestorR.getResult();

        Integer deliverFee = null;
        for (SpecialDeliver specialDeliver : specialDelivers) {
            List<Integer> addressIds = getAddressIds(specialDeliver.getAddresses());
            if(addressMatch(ancestors, addressIds)) {
                //特殊区域计算
                deliverFee = nativeCalculate(specialDeliver.getValuation(), specialDeliver.getFee(),
                        specialDeliver.getLowPrice(), specialDeliver.getHighPrice(),
                        specialDeliver.getLowFee(), specialDeliver.getHighFee(),
                        specialDeliver.getMiddleFee(), specialDeliver.getFirstFee(),
                        specialDeliver.getPerFee(), quantity, item.getPrice() * quantity);
            }
        }
        if(Arguments.isNull(deliverFee)) {
            //模板计算
            deliverFee = nativeCalculate(deliverFeeTemplate.getValuation(), deliverFeeTemplate.getFee(),
                    deliverFeeTemplate.getLowPrice(), deliverFeeTemplate.getHighPrice(),
                    deliverFeeTemplate.getLowFee(), deliverFeeTemplate.getHighFee(),
                    deliverFeeTemplate.getMiddleFee(), deliverFeeTemplate.getFirstFee(),
                    deliverFeeTemplate.getPerFee(), quantity, item.getPrice() * quantity);
        }

        return deliverFee;
    }

    private List<Integer> getAddressIds(String address) {
        List<String> addresses = JSON_NON_DEFAULT_MAPPER.fromJson(address, JSON_NON_DEFAULT_MAPPER.createCollectionType(List.class, String.class));
        return Lists.transform(addresses, new Function<String, Integer>() {
            @Override
            public Integer apply(String input) {
                return Integer.valueOf(input);
            }
        });
    }

    private Boolean addressMatch(List<Address> ancestors, List<Integer> addressIds) {
        for(Address ancestor : ancestors) {
            if(addressIds.contains(ancestor.getId())){
                return true;
            }
        }
        return false;
    }

    private Integer nativeCalculate(DeliverRule deliverRule, Integer quantity, Integer totalFee) {
        int deliverFee = 0;
        switch (DeliverRule.Valuation.from(deliverRule.getValuation())) {
            case DETERMINATE:
                deliverFee = deliverRule.getFee();
                break;
            case MONEY:
                if(totalFee < deliverRule.getLowPrice()) {
                    deliverFee = deliverRule.getLowFee();
                }else if(totalFee > deliverRule.getHighPrice()) {
                    deliverFee = deliverRule.getHighFee();
                }else {
                    deliverFee = deliverRule.getMiddleFee();
                }
                break;
            case NUMBER:
                deliverFee = deliverRule.getFirstFee() + (quantity - 1) * deliverRule.getPerFee();
                break;
            default:
                break;
        }
        return deliverFee;
    }

    private Integer nativeCalculate(Integer valuation, Integer fee, Integer lowPrice,
                                    Integer highPrice, Integer lowFee, Integer highFee,
                                    Integer middleFee, Integer firstFee, Integer perFee,
                                    Integer quantity, Integer totalFee) {
        int deliverFee = 0;
        switch (DeliverRule.Valuation.from(valuation)) {
            case DETERMINATE:
                deliverFee = fee;
                break;
            case MONEY:
                if(totalFee < lowPrice) {
                    deliverFee = lowFee;
                }else if(totalFee >= highPrice) {
                    deliverFee = highFee;
                }else {
                    deliverFee = middleFee;
                }
                break;
            case NUMBER:
                deliverFee = firstFee + (quantity - 1) * perFee;
                break;
            default:
                break;
        }
        return deliverFee;
    }

    @Override
    public Response<Integer> getOrderDeliverFee(Map<Long, Integer> skuIdAndQuantity, Integer addressId) {
        Response<Integer> result = new Response<>();

        try {
            if (Arguments.isNull(addressId)) {
                return Response.ok(0);
            }
            List<Long> skuIds = new ArrayList<>();
            skuIds.addAll(skuIdAndQuantity.keySet());
            List<Sku> skus = skuDao.loads(skuIds);
            Multimap<Long, Sku> itemIdAndSkus = getItemIdAndSkus(skus);

            List<Long> itemIds = getItemIdsFromSkus(skus);

            List<ShipFee> shipFees = shipFeeDao.findByItemIds(itemIds.toArray(new Long[itemIds.size()]));
            Map<Long, ShipFee> itemIdAndShipFeeMap = getItemIdAndShipFeeMap(shipFees);
            Multimap<Long, Sku> templateIdAndSkus = getTemplateIdAndSkus(itemIdAndShipFeeMap, itemIdAndSkus);

            Response<List<Address>> ancestorR = addressService.ancestorOfAddresses(addressId);
            if(!ancestorR.isSuccess()) {
                log.warn("fail to find ancestor of address id {} when calculate deliver fee", addressId);
                throw new ServiceException("address.query.fail");
            }
            List<Address> ancestors = ancestorR.getResult();

            int totalFee = 0;

            Map<Long, DeliverFeeTemplate> deliverFeeTemplateMap = getDeliverFeeTemplateMap(templateIdAndSkus.keys());
            Map<Long, List<SpecialDeliver>> templateIdAndSpecialDeliverMap = getSpecialDeliverMap(templateIdAndSkus.keys());
            for(Long templateId : templateIdAndSkus.keySet()) {
                DeliverFeeTemplate template = deliverFeeTemplateMap.get(templateId);
                List<SpecialDeliver> specialDelivers = templateIdAndSpecialDeliverMap.get(templateId);
                int deliverFee = groupOrderDeliverFee(template, specialDelivers, templateIdAndSkus, ancestors, skuIdAndQuantity);
                totalFee += deliverFee;
            }

            result.setResult(totalFee);
        }catch (Exception e) {
            log.error("fail to get order deliver fee by sku id {}, quantity {}, cause:{}",
                    skuIdAndQuantity.keySet(), skuIdAndQuantity.values(), Throwables.getStackTraceAsString(e));
            result.setError("deliver.fee.query.fail");
        }
        return result;
    }

    @Override
    public Response<Integer> computerItemDetailDeliverFee(Long itemId, Integer quantity, Integer price) {
        Response<Integer> res = new Response<>();

        try {
            Item item = itemDao.load(itemId);
            ShipFee shipFee = shipFeeDao.findByItemId(itemId);

            Long templateId = getTemplateId(shipFee, item);

            if(null == templateId) {
                log.error("template id not found when computer item detail deliver fee, params: item id {}, quantity {}, price {}",
                        itemId, quantity, price);
                res.setResult(0);
                return res;
            }

            DeliverFeeTemplate template = deliverFeeTemplateDao.load(templateId);
            if(null == template) {
                log.error("template id {} not found", templateId);
                res.setResult(0);
                return res;
            }

            Integer deliverFee = nativeCalculate(template.getValuation(), template.getFee(),
                    template.getLowPrice(), template.getHighPrice(),
                    template.getLowFee(), template.getHighFee(),
                    template.getMiddleFee(), template.getFirstFee(),
                    template.getPerFee(), quantity, price);

            res.setResult(deliverFee);
        }catch (Exception e) {
            log.error("fail to computer item detail delvier fee by item id {}, quantity {}, price {}, cause:{}",
                    itemId, quantity, price, Throwables.getStackTraceAsString(e));
            res.setError("computer.item.detail.deliver.fee.fail");
        }
        return res;
    }

    private Map<Long, DeliverFeeTemplate> getDeliverFeeTemplateMap(Collection<Long> ids) {
        if(ids == null || ids.isEmpty()) {
            return new HashMap<>();
        }
        List<Long> templateIds = new ArrayList<>();
        templateIds.addAll(ids);
        List<DeliverFeeTemplate> templates = deliverFeeTemplateDao.loads(templateIds);
        Map<Long, DeliverFeeTemplate> result = new HashMap<>();
        for(DeliverFeeTemplate deliverFeeTemplate : templates) {
            result.put(deliverFeeTemplate.getId(), deliverFeeTemplate);
        }
        return result;
    }

    private Map<Long, ShipFee> getItemIdAndShipFeeMap(List<ShipFee> shipFees) {
        Map<Long, ShipFee> map = new HashMap<>();
        for(ShipFee shipFee : shipFees) {
            map.put(shipFee.getItemId(), shipFee);
        }
        return map;
    }

    private Multimap<Long, Sku> getItemIdAndSkus(List<Sku> skus) {
        Multimap<Long, Sku> itemIdAndSkus = HashMultimap.create();
        for(Sku sku : skus) {
            itemIdAndSkus.put(sku.getItemId(), sku);
        }
        return itemIdAndSkus;
    }

    private List<Long> getItemIdsFromSkus(List<Sku> skus) {
        return Lists.transform(skus, new Function<Sku, Long>() {
            @Override
            public Long apply(Sku input) {
                return input.getItemId();
            }
        });
    }

    /**
     * 把一个订单中的sku根据运费模板分组
     */
    private Multimap<Long, Sku> getTemplateIdAndSkus(Map<Long, ShipFee> itemIdAndShipFeeMap, Multimap<Long, Sku> itemIdAndSkus) {
        Multimap<Long, Sku> templateIdAndSkus = HashMultimap.create();

        Collection<Long> itemIds = itemIdAndSkus.keys();
        List<Long> itemIdList = new ArrayList<>();
        itemIdList.addAll(itemIds);
        List<Item> items = itemDao.loads(itemIdList);
        Map<Long, Item> itemMap = Maps.uniqueIndex(items, new Function<Item, Long>() {
            @Override
            public Long apply(Item input) {
                return input.getId();
            }
        });

        for(Long itemId : itemIdAndSkus.keySet()) {
            Iterable<Sku> skuIterable = itemIdAndSkus.get(itemId);
            ShipFee shipFee = itemIdAndShipFeeMap.get(itemId);

            Long templateId = getTemplateId(shipFee, itemMap.get(itemId));

            if(!Arguments.isNull(templateId)) {
                templateIdAndSkus.putAll(templateId, skuIterable);
            }
        }
        return templateIdAndSkus;
    }

    private Long getTemplateId(ShipFee shipFee, Item item) {
        Long templateId = null;
        if(Arguments.isNull(shipFee) || Arguments.isNull(shipFee.getDeliverFeeTemplateId())) {
            if(Arguments.isNull(item)) {
                throw new ServiceException("item.not.found");
            }
            DeliverFeeTemplate defaultTemplate = deliverFeeTemplateDao.findDefaultByUserId(item.getUserId());
            if(!Arguments.isNull(defaultTemplate)) {
                templateId = defaultTemplate.getId();
            }
        }else {
            templateId = shipFee.getDeliverFeeTemplateId();
        }
        return templateId;
    }

    /**
     * 计算同个运费模板的所有sku运费
     */
    private Integer groupOrderDeliverFee(DeliverFeeTemplate deliverFeeTemplate, List<SpecialDeliver> specialDelivers, Multimap<Long, Sku> templateIdAndSkus,
                                       List<Address> ancestors, Map<Long, Integer> skuIdAndQuantity) {

        Iterable<Sku> skuIterable = templateIdAndSkus.get(deliverFeeTemplate.getId());
        int totalQuantity = 0;
        int totalPrice = 0;
        for(Sku sku : skuIterable) {
            int quantity = skuIdAndQuantity.get(sku.getId());
            totalQuantity += quantity;
            totalPrice += sku.getPrice() * quantity;
        }
        Integer deliverFee = null;
        if(!Arguments.isNullOrEmpty(specialDelivers)) {
            for (SpecialDeliver specialDeliver : specialDelivers) {
                List<Integer> addressIds = getAddressIds(specialDeliver.getAddresses());
                if (addressMatch(ancestors, addressIds)) {
                    //特殊区域计算
                    deliverFee = nativeCalculate(specialDeliver.getValuation(), specialDeliver.getFee(),
                            specialDeliver.getLowPrice(), specialDeliver.getHighPrice(),
                            specialDeliver.getLowFee(), specialDeliver.getHighFee(),
                            specialDeliver.getMiddleFee(), specialDeliver.getFirstFee(),
                            specialDeliver.getPerFee(), totalQuantity, totalPrice);
                }
            }
        }
        if(Arguments.isNull(deliverFee)) {
            //模板计算
            deliverFee = nativeCalculate(deliverFeeTemplate.getValuation(), deliverFeeTemplate.getFee(),
                    deliverFeeTemplate.getLowPrice(), deliverFeeTemplate.getHighPrice(),
                    deliverFeeTemplate.getLowFee(), deliverFeeTemplate.getHighFee(),
                    deliverFeeTemplate.getMiddleFee(), deliverFeeTemplate.getFirstFee(),
                    deliverFeeTemplate.getPerFee(), totalQuantity, totalPrice);
        }
        return deliverFee;
    }

    @Override
    public Response<DeliverTemplateDto> findItemDeliverTemplate(Long itemId) {
        Response<DeliverTemplateDto> res = new Response<>();
        try {
            ShipFee shipFee = shipFeeDao.findByItemId(itemId);
            Item item = itemDao.load(itemId);
            Long templateId = null;
            if(null == shipFee || null == shipFee.getDeliverFeeTemplateId()) {
                DeliverFeeTemplate defaultTemplate = deliverFeeTemplateDao.findDefaultByUserId(item.getUserId());
                if(null != defaultTemplate) {
                    templateId = defaultTemplate.getId();
                }
            }else {
                templateId = shipFee.getDeliverFeeTemplateId();
            }

            if (null != templateId) {
                return findDeliverTemplateDetailById(templateId);
            }else {
                return Response.ok(new DeliverTemplateDto());
            }
        }catch (Exception e) {
            log.error("fail to find item deliver template by item id {}, cause:{}",
                    itemId, Throwables.getStackTraceAsString(e));
            res.setError("deliver.template.query.fail");
        }
        return res;
    }
}
