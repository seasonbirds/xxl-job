package com.xxl.job.admin.service;

import com.xxl.job.admin.mapper.XxlJobOperateLogMapper;
import com.xxl.job.admin.model.XxlJobOperateLog;
import com.xxl.tool.core.DateTool;
import com.xxl.tool.response.PageModel;
import com.xxl.tool.response.Response;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 操作日志Service
 * @author xxl-job
 */
@Service
public class XxlJobOperateLogService {
    private static final Logger logger = LoggerFactory.getLogger(XxlJobOperateLogService.class);

    @Resource
    private XxlJobOperateLogMapper xxlJobOperateLogMapper;

    public void save(XxlJobOperateLog log) {
        try {
            if (log.getOperateTime() == null) {
                log.setOperateTime(new Date());
            }
            xxlJobOperateLogMapper.save(log);
        } catch (Exception e) {
            logger.error("Save operate log error: {}", e.getMessage(), e);
        }
    }

    public Response<PageModel<XxlJobOperateLog>> pageList(int offset, int pagesize,
                                                            String module,
                                                            String action,
                                                            String operator,
                                                            String targetName,
                                                            String filterTime,
                                                            Integer targetId) {
        Date startTime = null;
        Date endTime = null;
        if (filterTime != null && !filterTime.isEmpty()) {
            String[] temp = filterTime.split(" - ");
            if (temp.length == 2) {
                startTime = DateTool.parseDateTime(temp[0]);
                endTime = DateTool.parseDateTime(temp[1]);
            }
        }

        List<XxlJobOperateLog> list = xxlJobOperateLogMapper.pageList(offset, pagesize,
                module, action, operator, targetName, startTime, endTime, targetId);
        int total = xxlJobOperateLogMapper.pageListCount(offset, pagesize,
                module, action, operator, targetName, startTime, endTime, targetId);

        PageModel<XxlJobOperateLog> pageModel = new PageModel<>();
        pageModel.setData(list);
        pageModel.setTotal(total);

        return Response.ofSuccess(pageModel);
    }

    public XxlJobOperateLog loadById(int id) {
        return xxlJobOperateLogMapper.loadById(id);
    }

    public int delete(int id) {
        return xxlJobOperateLogMapper.delete(id);
    }

    public int deleteByTime(Date deleteBeforeTime) {
        return xxlJobOperateLogMapper.deleteByTime(deleteBeforeTime);
    }
}
