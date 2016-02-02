package io.terminus.parana.item.service;

import com.google.common.base.Preconditions;
import io.terminus.common.exception.ServiceException;
import io.terminus.pampas.common.Response;
import io.terminus.parana.item.dao.mysql.ImportApplyDao;
import io.terminus.parana.item.model.ImportApply;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by zhanghecheng on 15/10/13.
 */
@Service
@Slf4j
public class ImportApplyWriteServiceImpl implements ImportApplyWriteService {

    @Autowired
    private ImportApplyDao importApplyDao;

    @Override
    public Response<Boolean> applyForImport(ImportApply apply) {
        Response<Boolean> response=new Response<>();
        try{
            apply.setStatus(ImportApply.Status.INIT.value());
            importApplyDao.create(apply);
            response.setResult(Boolean.TRUE);
        }catch (Exception ex){
            log.error("apply for import failed, apply={}, cause={}", apply, ex.getMessage());
            response.setError("apply.for.import.failed");
        }
        return response;
    }

    @Override
    public Response<Boolean> handleApply(Long applyId, Integer status, String checkResult) {

        Response<Boolean> response=new Response<>();
        try {
            Preconditions.checkNotNull(applyId);
            Preconditions.checkNotNull(status);

            ImportApply apply = importApplyDao.load(applyId);
            if (apply == null) {
                throw new ServiceException("apply.not.exists");
            }

            ImportApply toUpdated=new ImportApply();
            toUpdated.setId(applyId);
            toUpdated.setStatus(status);
            toUpdated.setCheckResult(checkResult);
            importApplyDao.update(toUpdated);

            response.setResult(Boolean.TRUE);

        }catch (ServiceException ex){
            log.error("handle apply failed, applyId={}, status={}, checkResult={}, cause={}", applyId, status, checkResult, ex.getMessage());
            response.setError(ex.getMessage());
        }catch (Exception ex){
            log.error("handle apply failed, applyId={}, status={}, checkResult={}, cause={}", applyId, status, checkResult, ex.getMessage());
            response.setError("handle.import.apply.failed");
        }
        return response;
    }
}
