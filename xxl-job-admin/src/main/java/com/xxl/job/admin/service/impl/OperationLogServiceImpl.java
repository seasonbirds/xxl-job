package com.xxl.job.admin.service.impl;

import com.xxl.job.admin.mapper.XxlJobOperationLogMapper;
import com.xxl.job.admin.model.XxlJobOperationLog;
import com.xxl.job.admin.service.OperationLogService;
import com.xxl.tool.response.PageModel;
import com.xxl.tool.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * operation log service impl
 *
 * @author xxl job
 */
@Service
public class OperationLogServiceImpl implements OperationLogService {
    private static Logger logger = LoggerFactory.getLogger(OperationLogServiceImpl.class);

    @Resource
    private XxlJobOperationLogMapper xxlJobOperationLogMapper;

    @Override
    public Response<PageModel<XxlJobOperationLog>> pageList(int offset, int pagesize,
                                                           String operationModule,
                                                           String operationType,
                                                           String operator,
                                                           Date startTime,
                                                           Date endTime,
                                                           String targetName) {

        PageModel<XxlJobOperationLog> pageModel = new PageModel<XxlJobOperationLog>();
        try {
            List<XxlJobOperationLog> list = xxlJobOperationLogMapper.pageList(offset, pagesize,
                    operationModule, operationType, operator, startTime, endTime, targetName);
            int list_count = xxlJobOperationLogMapper.pageListCount(offset, pagesize,
                    operationModule, operationType, operator, startTime, endTime, targetName);

            pageModel.setData(list);
            pageModel.setTotalCount(list_count);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Response.makeFail(e.getMessage());
        }

        return Response.makeSuccess(pageModel);
    }

    @Override
    public Response<Void> save(XxlJobOperationLog operationLog) {
        try {
            int ret = xxlJobOperationLogMapper.save(operationLog);
            if (ret != 1) {
                return Response.makeFail("save operation log fail");
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Response.makeFail(e.getMessage());
        }
        return Response.makeSuccess();
    }

    @Override
    public Response<Void> deleteByTime(Date startTime, Date endTime) {
        try {
            xxlJobOperationLogMapper.deleteByTime(startTime, endTime);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Response.makeFail(e.getMessage());
        }
        return Response.makeSuccess();
    }

    @Override
    public Response<List<String>> getOperationModules() {
        List<String> modules = new ArrayList<>();
        modules.add("LOGIN");
        modules.add("USER");
        modules.add("EXECUTOR");
        modules.add("JOB");
        return Response.makeSuccess(modules);
    }

    @Override
    public Response<List<String>> getOperationTypes() {
        List<String> types = new ArrayList<>();
        types.add("LOGIN");
        types.add("ADD");
        types.add("UPDATE");
        types.add("DELETE");
        types.add("START");
        types.add("STOP");
        return Response.makeSuccess(types);
    }
}
