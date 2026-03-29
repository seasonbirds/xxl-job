package com.xxl.job.admin.service;

import com.xxl.job.admin.model.XxlJobOperationLog;
import com.xxl.tool.response.PageModel;
import com.xxl.tool.response.Response;

import java.util.Date;
import java.util.List;

/**
 * operation log service
 *
 * @author xxl job
 */
public interface OperationLogService {

    /**
     * page list
     */
    Response<PageModel<XxlJobOperationLog>> pageList(int offset, int pagesize,
                                                    String operationModule,
                                                    String operationType,
                                                    String operator,
                                                    Date startTime,
                                                    Date endTime,
                                                    String targetName);

    /**
     * save operation log
     */
    Response<Void> save(XxlJobOperationLog operationLog);

    /**
     * delete by time
     */
    Response<Void> deleteByTime(Date startTime, Date endTime);

    /**
     * get all operation modules
     */
    Response<List<String>> getOperationModules();

    /**
     * get all operation types
     */
    Response<List<String>> getOperationTypes();
}
