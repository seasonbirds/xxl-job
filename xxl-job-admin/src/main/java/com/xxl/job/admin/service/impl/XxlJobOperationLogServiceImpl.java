package com.xxl.job.admin.service.impl;

import com.xxl.job.admin.mapper.XxlJobGroupMapper;
import com.xxl.job.admin.mapper.XxlJobInfoMapper;
import com.xxl.job.admin.mapper.XxlJobOperationLogMapper;
import com.xxl.job.admin.mapper.XxlJobUserMapper;
import com.xxl.job.admin.model.XxlJobGroup;
import com.xxl.job.admin.model.XxlJobInfo;
import com.xxl.job.admin.model.XxlJobOperationLog;
import com.xxl.job.admin.model.XxlJobUser;
import com.xxl.job.admin.service.XxlJobOperationLogService;
import com.xxl.job.admin.util.I18nUtil;
import com.xxl.tool.core.DateTool;
import com.xxl.tool.response.PageModel;
import com.xxl.tool.response.Response;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * operation log service impl
 *
 * @author xxl-job 2024
 */
@Service
public class XxlJobOperationLogServiceImpl implements XxlJobOperationLogService {

    private static Logger logger = LoggerFactory.getLogger(XxlJobOperationLogServiceImpl.class);

    @Resource
    private XxlJobOperationLogMapper xxlJobOperationLogMapper;
    @Resource
    private XxlJobUserMapper xxlJobUserMapper;
    @Resource
    private XxlJobGroupMapper xxlJobGroupMapper;
    @Resource
    private XxlJobInfoMapper xxlJobInfoMapper;

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
    @Override
    public Response<PageModel<XxlJobOperationLog>> pageList(int offset, int pagesize, String module,
                                                             String operationType, String operator,
                                                             Date operateTimeStart, Date operateTimeEnd,
                                                             Integer jobGroupId, String targetName) {
        List<XxlJobOperationLog> list = xxlJobOperationLogMapper.pageList(offset, pagesize, module, operationType,
                operator, operateTimeStart, operateTimeEnd, jobGroupId, targetName);
        int listCount = xxlJobOperationLogMapper.pageListCount(offset, pagesize, module, operationType,
                operator, operateTimeStart, operateTimeEnd, jobGroupId, targetName);

        PageModel<XxlJobOperationLog> pageModel = new PageModel<>();
        pageModel.setData(list);
        pageModel.setTotal(listCount);

        return Response.ofSuccess(pageModel);
    }

    /**
     * add operation log
     *
     * @param log operation log
     * @return result
     */
    @Override
    public Response<String> addLog(XxlJobOperationLog log) {
        try {
            int ret = xxlJobOperationLogMapper.save(log);
            return ret > 0 ? Response.ofSuccess() : Response.ofFail();
        } catch (Exception e) {
            logger.error(">>>>>>>>>>> add operation log error: {}", e.getMessage(), e);
            return Response.ofSuccess();
        }
    }

    /**
     * clear log by type
     *
     * @param type clear type
     * @return result
     */
    @Override
    public Response<String> clearLog(int type) {
        Date clearBeforeTime = null;
        if (type == 1) {
            clearBeforeTime = DateTool.addMonths(new Date(), -1);
        } else if (type == 2) {
            clearBeforeTime = DateTool.addMonths(new Date(), -3);
        } else if (type == 3) {
            clearBeforeTime = DateTool.addMonths(new Date(), -6);
        } else if (type == 4) {
            clearBeforeTime = DateTool.addYears(new Date(), -1);
        } else if (type == 9) {
            clearBeforeTime = Calendar.getInstance().getTime();
        } else {
            return Response.ofFail(I18nUtil.getString("joblog_clean_type_unvalid"));
        }

        int ret = xxlJobOperationLogMapper.clearLog(clearBeforeTime);
        return Response.ofSuccess(I18nUtil.getString("system_success") + ":" + ret);
    }

    /**
     * find all job group
     *
     * @return job group list
     */
    @Override
    public List<XxlJobGroup> findAllJobGroup() {
        return xxlJobGroupMapper.findAll();
    }

    /**
     * load user by id
     *
     * @param userId user id
     * @return user
     */
    @Override
    public XxlJobUser loadUserById(Integer userId) {
        if (userId == null) {
            return null;
        }
        return xxlJobUserMapper.loadById(userId);
    }

    /**
     * load job group by id
     *
     * @param jobGroupId job group id
     * @return job group
     */
    @Override
    public XxlJobGroup loadJobGroupById(Integer jobGroupId) {
        if (jobGroupId == null) {
            return null;
        }
        return xxlJobGroupMapper.load(jobGroupId);
    }

    /**
     * load job info by id
     *
     * @param jobId job id
     * @return job info
     */
    @Override
    public XxlJobInfo loadJobInfoById(Integer jobId) {
        if (jobId == null) {
            return null;
        }
        return xxlJobInfoMapper.loadById(jobId);
    }
}
