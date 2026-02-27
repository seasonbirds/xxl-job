package com.xxl.job.admin.service;

import com.xxl.job.admin.model.XxlJobGroup;
import com.xxl.job.admin.model.XxlJobInfo;
import com.xxl.job.admin.model.XxlJobOperationLog;
import com.xxl.job.admin.model.XxlJobUser;
import com.xxl.tool.response.PageModel;
import com.xxl.tool.response.Response;

import java.util.Date;
import java.util.List;

/**
 * operation log service
 *
 * @author xxl-job 2024
 */
public interface XxlJobOperationLogService {

    /**
     * page list
     *
     * @param offset          offset
     * @param pagesize        pagesize
     * @param module          module
     * @param operationType   operation type
     * @param operator        operator
     * @param operateTimeStart operate time start
     * @param operateTimeEnd   operate time end
     * @param jobGroupId      job group id
     * @param targetName      target name
     * @return page model
     */
    Response<PageModel<XxlJobOperationLog>> pageList(int offset, int pagesize, String module,
                                                      String operationType, String operator,
                                                      Date operateTimeStart, Date operateTimeEnd,
                                                      Integer jobGroupId, String targetName);

    /**
     * add operation log
     *
     * @param log operation log
     * @return result
     */
    Response<String> addLog(XxlJobOperationLog log);

    /**
     * clear log by type
     *
     * @param type clear type
     * @return result
     */
    Response<String> clearLog(int type);

    /**
     * find all job group
     *
     * @return job group list
     */
    List<XxlJobGroup> findAllJobGroup();

    /**
     * load user by id
     *
     * @param userId user id
     * @return user
     */
    XxlJobUser loadUserById(Integer userId);

    /**
     * load job group by id
     *
     * @param jobGroupId job group id
     * @return job group
     */
    XxlJobGroup loadJobGroupById(Integer jobGroupId);

    /**
     * load job info by id
     *
     * @param jobId job id
     * @return job info
     */
    XxlJobInfo loadJobInfoById(Integer jobId);
}
