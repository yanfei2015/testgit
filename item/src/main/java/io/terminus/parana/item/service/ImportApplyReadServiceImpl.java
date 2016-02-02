package io.terminus.parana.item.service;

import com.google.common.base.Throwables;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.utils.BeanMapper;
import io.terminus.pampas.common.BaseUser;
import io.terminus.pampas.common.Response;
import io.terminus.parana.item.dao.mysql.ImportApplyDao;
import io.terminus.parana.item.dto.ImportApplyCriteria;
import io.terminus.parana.item.model.ImportApply;
import io.terminus.parana.user.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Created by zhanghecheng on 15/10/13.
 */
@Service
@Slf4j
public class ImportApplyReadServiceImpl implements ImportApplyReadService {

    @Autowired
    private ImportApplyDao importApplyDao;

    @Override
    public Response<Paging<ImportApply>> paging(BaseUser user, ImportApplyCriteria criteria) {

        if(user==null||!user.getType().equals(User.TYPE.SELLER.toNumber())){
            throw new JsonResponseException("user.type.not.seller");
        }
        criteria.setSellerId(user.getId());

        return adminPaging(criteria);
    }

    @Override
    public Response<Paging<ImportApply>> adminPaging(ImportApplyCriteria criteria) {
        Response<Paging<ImportApply>> pagingResponse=new Response<>();
        try{
            pagingResponse.setResult(importApplyDao.paging(BeanMapper.map(criteria, Map.class)));
        }catch (Exception ex){
            log.error("paging import apply failed, criteria={}, cause={}",criteria,Throwables.getStackTraceAsString(ex));
            pagingResponse.setError("paging.import.apply.failed");
        }
        return pagingResponse;
    }

    @Override
    public Response<ImportApply> findByApplyId(BaseUser user, Long applyId) {
        Response<ImportApply> response=new Response<>();
        try{
            ImportApply apply=importApplyDao.load(applyId);
            response.setResult(apply);
        }catch (Exception ex){
            log.error("findImportApplyById failed, applyId={},cause={}",applyId, Throwables.getStackTraceAsString(ex));
            response.setError("find.import.apply.by.id.failed");
        }
        return response;
    }
}
