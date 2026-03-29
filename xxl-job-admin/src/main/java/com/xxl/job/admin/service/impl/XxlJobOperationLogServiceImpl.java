package com.xxl.job.admin.service.impl;

import com.xxl.job.admin.mapper.XxlJobOperationLogMapper;
import com.xxl.job.admin.model.XxlJobOperationLog;
import com.xxl.job.admin.service.XxlJobOperationLogService;
import com.xxl.tool.response.PageModel;
import com.xxl.tool.response.Response;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class XxlJobOperationLogServiceImpl implements XxlJobOperationLogService {
    private static Logger logger = LoggerFactory.getLogger(XxlJobOperationLogServiceImpl.class);

    @Resource
    private XxlJobOperationLogMapper xxlJobOperationLogMapper;

    @Override
    public Response<PageModel<XxlJobOperationLog>> pageList(int offset, int pagesize, String operationModule, String operationType, String operator, Date startTime, Date endTime, Integer jobGroup) {
        List<XxlJobOperationLog> list = xxlJobOperationLogMapper.pageList(offset, pagesize, operationModule, operationType, operator, startTime, endTime, jobGroup);
        int list_count = xxlJobOperationLogMapper.pageListCount(offset, pagesize, operationModule, operationType, operator, startTime, endTime, jobGroup);

        PageModel<XxlJobOperationLog> pageModel = new PageModel<>();
        pageModel.setData(list);
        pageModel.setTotal(list_count);

        return Response.ofSuccess(pageModel);
    }

    @Override
    public Response<String> add(XxlJobOperationLog operationLog) {
        try {
            operationLog.setCreateTime(new Date());
            xxlJobOperationLogMapper.save(operationLog);
            return Response.ofSuccess("SUCCESS");
        } catch (Exception e) {
            logger.error("Add operation log error", e);
            return Response.ofFail(e.getMessage());
        }
    }
}
