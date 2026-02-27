package com.xxl.job.admin.service.impl;

import com.xxl.job.admin.mapper.XxlJobGroupMapper;
import com.xxl.job.admin.mapper.XxlJobInfoMapper;
import com.xxl.job.admin.mapper.XxlJobOperationLogMapper;
import com.xxl.job.admin.mapper.XxlJobUserMapper;
import com.xxl.job.admin.model.XxlJobGroup;
import com.xxl.job.admin.model.XxlJobInfo;
import com.xxl.job.admin.model.XxlJobOperationLog;
import com.xxl.job.admin.model.XxlJobUser;
import com.xxl.job.admin.service.OperationLogService;
import com.xxl.tool.response.PageModel;
import com.xxl.tool.response.Response;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * operation log service impl
 *
 * @author xxl-job
 */
@Service
public class OperationLogServiceImpl implements OperationLogService {

    private static Logger logger = LoggerFactory.getLogger(OperationLogServiceImpl.class);

    @Resource
    private XxlJobOperationLogMapper xxlJobOperationLogMapper;
    @Resource
    private XxlJobUserMapper xxlJobUserMapper;
    @Resource
    private XxlJobGroupMapper xxlJobGroupMapper;
    @Resource
    private XxlJobInfoMapper xxlJobInfoMapper;

    @Override
    public Response<PageModel<XxlJobOperationLog>> pageList(int offset, int pagesize,
                                                            String module,
                                                            String operationType,
                                                            String operator,
                                                            String targetName,
                                                            Integer jobGroup,
                                                            Date startTime,
                                                            Date endTime) {
        List<XxlJobOperationLog> list = xxlJobOperationLogMapper.pageList(offset, pagesize,
                module, operationType, operator, targetName, jobGroup, startTime, endTime);
        int list_count = xxlJobOperationLogMapper.pageListCount(
                module, operationType, operator, targetName, jobGroup, startTime, endTime);

        PageModel<XxlJobOperationLog> pageModel = new PageModel<>();
        pageModel.setData(list);
        pageModel.setTotal(list_count);

        return Response.ofSuccess(pageModel);
    }

    @Override
    @Async
    public void saveLogAsync(XxlJobOperationLog operationLog) {
        try {
            xxlJobOperationLogMapper.save(operationLog);
        } catch (Exception e) {
            logger.error("save operation log error", e);
        }
    }

    @Override
    public XxlJobUser getUserById(int id) {
        return xxlJobUserMapper.loadById(id);
    }

    @Override
    public XxlJobGroup getJobGroupById(int id) {
        return xxlJobGroupMapper.load(id);
    }

    @Override
    public XxlJobInfo getJobInfoById(int id) {
        return xxlJobInfoMapper.loadById(id);
    }

}
