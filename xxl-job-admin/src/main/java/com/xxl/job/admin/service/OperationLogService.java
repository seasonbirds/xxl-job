package com.xxl.job.admin.service;

import com.xxl.job.admin.model.XxlJobGroup;
import com.xxl.job.admin.model.XxlJobInfo;
import com.xxl.job.admin.model.XxlJobOperationLog;
import com.xxl.job.admin.model.XxlJobUser;
import com.xxl.tool.response.PageModel;
import com.xxl.tool.response.Response;

import java.util.Date;

/**
 * operation log service
 *
 * @author xxl-job
 */
public interface OperationLogService {

    /**
     * page list
     */
    Response<PageModel<XxlJobOperationLog>> pageList(int offset, int pagesize,
                                                     String module,
                                                     String operationType,
                                                     String operator,
                                                     String targetName,
                                                     Integer jobGroup,
                                                     Date startTime,
                                                     Date endTime);

    /**
     * async save log
     */
    void saveLogAsync(XxlJobOperationLog operationLog);

    /**
     * get user by id
     */
    XxlJobUser getUserById(int id);

    /**
     * get job group by id
     */
    XxlJobGroup getJobGroupById(int id);

    /**
     * get job info by id
     */
    XxlJobInfo getJobInfoById(int id);

}
