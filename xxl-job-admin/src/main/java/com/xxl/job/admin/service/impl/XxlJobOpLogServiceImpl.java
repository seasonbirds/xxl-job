package com.xxl.job.admin.service.impl;

import com.xxl.job.admin.mapper.XxlJobOpLogMapper;
import com.xxl.job.admin.model.XxlJobOpLog;
import com.xxl.job.admin.service.XxlJobOpLogService;
import com.xxl.tool.response.PageModel;
import com.xxl.tool.response.Response;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 操作日志服务实现类
 * @author xxl-job
 */
@Service
public class XxlJobOpLogServiceImpl implements XxlJobOpLogService {

    private static final Logger logger = LoggerFactory.getLogger(XxlJobOpLogServiceImpl.class);

    @Resource
    private XxlJobOpLogMapper xxlJobOpLogMapper;

    /**
     * 异步记录日志的线程池
     * 使用单线程线程池保证日志顺序记录，同时避免影响主业务
     */
    private final ExecutorService logExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "xxl-job-oplog-thread");
        t.setDaemon(true);
        return t;
    });

    @Override
    public Response<PageModel<XxlJobOpLog>> pageList(int offset, int pagesize,
                                                     String module, String type, String operator, Integer jobGroup,
                                                     Date opTimeStart, Date opTimeEnd) {
        // page list
        List<XxlJobOpLog> list = xxlJobOpLogMapper.pageList(offset, pagesize, module, type, operator, jobGroup, opTimeStart, opTimeEnd);
        int listCount = xxlJobOpLogMapper.pageListCount(offset, pagesize, module, type, operator, jobGroup, opTimeStart, opTimeEnd);

        // package result
        PageModel<XxlJobOpLog> pageModel = new PageModel<>();
        pageModel.setData(list);
        pageModel.setTotal(listCount);

        return Response.ofSuccess(pageModel);
    }

    @Override
    public void asyncLog(String module, String type, String operator, String opIp,
                         String targetId, String targetName, String content) {
        asyncLog(module, type, operator, opIp, targetId, targetName, content, null);
    }

    @Override
    public void asyncLog(String module, String type, String operator, String opIp,
                         String targetId, String targetName, String content, Integer jobGroup) {
        try {
            XxlJobOpLog opLog = new XxlJobOpLog();
            opLog.setModule(module);
            opLog.setType(type);
            opLog.setOperator(operator);
            opLog.setOpIp(opIp);
            opLog.setOpTime(new Date());
            opLog.setTargetId(targetId);
            opLog.setTargetName(targetName);
            opLog.setContent(content);
            opLog.setJobGroup(jobGroup);

            // 异步执行，不阻塞主业务
            logExecutor.execute(() -> {
                try {
                    xxlJobOpLogMapper.save(opLog);
                } catch (Exception e) {
                    logger.error("保存操作日志失败: {}", e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            // 日志记录失败不影响主业务
            logger.error("异步记录操作日志失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public long save(XxlJobOpLog xxlJobOpLog) {
        if (xxlJobOpLog.getOpTime() == null) {
            xxlJobOpLog.setOpTime(new Date());
        }
        return xxlJobOpLogMapper.save(xxlJobOpLog);
    }

}
