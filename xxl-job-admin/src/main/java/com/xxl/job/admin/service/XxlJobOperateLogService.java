package com.xxl.job.admin.service;

import com.xxl.job.admin.mapper.XxlJobGroupMapper;
import com.xxl.job.admin.mapper.XxlJobInfoMapper;
import com.xxl.job.admin.mapper.XxlJobOperateLogMapper;
import com.xxl.job.admin.model.XxlJobGroup;
import com.xxl.job.admin.model.XxlJobInfo;
import com.xxl.job.admin.model.XxlJobOperateLog;
import com.xxl.tool.core.CollectionTool;
import com.xxl.tool.core.DateTool;
import com.xxl.tool.response.PageModel;
import com.xxl.tool.response.Response;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 操作日志Service
 * @author xxl-job
 */
@Service
public class XxlJobOperateLogService {
    private static final Logger logger = LoggerFactory.getLogger(XxlJobOperateLogService.class);

    @Resource
    private XxlJobOperateLogMapper xxlJobOperateLogMapper;

    @Resource
    private XxlJobGroupMapper xxlJobGroupMapper;

    @Resource
    private XxlJobInfoMapper xxlJobInfoMapper;

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

    public List<XxlJobGroup> findAllGroups() {
        return xxlJobGroupMapper.findAll();
    }

    public Response<PageModel<Map<String, Object>>> pageListWithDetail(int offset, int pagesize,
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

        List<XxlJobGroup> allGroups = xxlJobGroupMapper.findAll();
        Map<Integer, XxlJobGroup> groupMap = new HashMap<>();
        if (CollectionTool.isNotEmpty(allGroups)) {
            for (XxlJobGroup group : allGroups) {
                groupMap.put(group.getId(), group);
            }
        }

        Map<Integer, XxlJobInfo> jobMap = new HashMap<>();
        if (CollectionTool.isNotEmpty(allGroups)) {
            for (XxlJobGroup group : allGroups) {
                List<XxlJobInfo> jobs = xxlJobInfoMapper.getJobsByGroup(group.getId());
                if (CollectionTool.isNotEmpty(jobs)) {
                    for (XxlJobInfo job : jobs) {
                        jobMap.put(job.getId(), job);
                    }
                }
            }
        }

        List<Map<String, Object>> resultList = new ArrayList<>();
        if (CollectionTool.isNotEmpty(list)) {
            for (XxlJobOperateLog log : list) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", log.getId());
                item.put("module", log.getModule());
                item.put("action", log.getAction());
                item.put("operator", log.getOperator());
                item.put("operateTime", log.getOperateTime());
                item.put("ip", log.getIp());
                item.put("targetId", log.getTargetId());
                item.put("targetName", log.getTargetName());
                item.put("extraData", log.getExtraData());

                if (XxlJobOperateLog.Module.JOB_INFO.getCode().equals(log.getModule()) && log.getTargetId() != null) {
                    XxlJobInfo jobInfo = jobMap.get(log.getTargetId());
                    if (jobInfo != null) {
                        XxlJobGroup group = groupMap.get(jobInfo.getJobGroup());
                        if (group != null) {
                            item.put("groupName", group.getTitle());
                        }
                        item.put("jobDesc", jobInfo.getJobDesc());
                    }
                } else if (XxlJobOperateLog.Module.JOB_GROUP.getCode().equals(log.getModule()) && log.getTargetId() != null) {
                    XxlJobGroup group = groupMap.get(log.getTargetId());
                    if (group != null) {
                        item.put("groupName", group.getTitle());
                    }
                }

                resultList.add(item);
            }
        }

        PageModel<Map<String, Object>> pageModel = new PageModel<>();
        pageModel.setData(resultList);
        pageModel.setTotal(total);

        return Response.ofSuccess(pageModel);
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
