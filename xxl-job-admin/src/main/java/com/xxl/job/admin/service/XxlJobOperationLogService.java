package com.xxl.job.admin.service;

import com.xxl.job.admin.model.XxlJobOperationLog;
import com.xxl.tool.response.PageModel;
import com.xxl.tool.response.Response;

import java.util.Date;

public interface XxlJobOperationLogService {
    
    Response<PageModel<XxlJobOperationLog>> pageList(int offset, int pagesize, String operationModule, String operationType, String operator, Date startTime, Date endTime, Integer jobGroup);
    
    Response<String> add(XxlJobOperationLog operationLog);
    
}
