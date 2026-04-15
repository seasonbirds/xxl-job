package com.xxl.job.admin.controller.biz;

import com.xxl.job.admin.constant.Consts;
import com.xxl.job.admin.mapper.XxlJobGroupMapper;
import com.xxl.job.admin.mapper.XxlJobInfoMapper;
import com.xxl.job.admin.model.XxlJobGroup;
import com.xxl.job.admin.model.XxlJobInfo;
import com.xxl.job.admin.model.XxlJobOperateLog;
import com.xxl.job.admin.service.XxlJobOperateLogService;
import com.xxl.job.admin.util.I18nUtil;
import com.xxl.sso.core.annotation.XxlSso;
import com.xxl.tool.core.CollectionTool;
import com.xxl.tool.response.PageModel;
import com.xxl.tool.response.Response;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

/**
 * 操作日志Controller
 * @author xxl-job
 */
@Controller
@RequestMapping("/operateLog")
public class JobOperateLogController {

    @Resource
    private XxlJobOperateLogService xxlJobOperateLogService;

    @Resource
    private XxlJobGroupMapper xxlJobGroupMapper;

    @Resource
    private XxlJobInfoMapper xxlJobInfoMapper;

    @RequestMapping
    @XxlSso(role = Consts.ADMIN_ROLE)
    public String index(Model model) {
        List<XxlJobGroup> groupList = xxlJobGroupMapper.findAll();
        model.addAttribute("groupList", groupList);
        return "biz/operateLog.list";
    }

    @RequestMapping("/pageList")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<PageModel<Map<String, Object>>> pageList(
            @RequestParam(required = false, defaultValue = "0") int offset,
            @RequestParam(required = false, defaultValue = "10") int pagesize,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String targetName,
            @RequestParam(required = false) String filterTime,
            @RequestParam(required = false) Integer jobGroup) {

        Integer targetId = null;
        if (jobGroup != null && jobGroup > 0) {
            targetId = jobGroup;
        }

        Response<PageModel<XxlJobOperateLog>> result = xxlJobOperateLogService.pageList(
                offset, pagesize, module, action, operator, targetName, filterTime, targetId);

        if (!result.isSuccess()) {
            return Response.ofFail(result.getMsg());
        }

        PageModel<XxlJobOperateLog> pageModel = result.getData();
        List<Map<String, Object>> resultList = new ArrayList<>();

        List<XxlJobGroup> allGroups = xxlJobGroupMapper.findAll();
        Map<Integer, XxlJobGroup> groupMap = new HashMap<>();
        if (CollectionTool.isNotEmpty(allGroups)) {
            for (XxlJobGroup group : allGroups) {
                groupMap.put(group.getId(), group);
            }
        }

        List<XxlJobInfo> allJobs = new ArrayList<>();
        Map<Integer, XxlJobInfo> jobMap = new HashMap<>();
        if (CollectionTool.isNotEmpty(allGroups)) {
            for (XxlJobGroup group : allGroups) {
                List<XxlJobInfo> jobs = xxlJobInfoMapper.getJobsByGroup(group.getId());
                if (CollectionTool.isNotEmpty(jobs)) {
                    allJobs.addAll(jobs);
                    for (XxlJobInfo job : jobs) {
                        jobMap.put(job.getId(), job);
                    }
                }
            }
        }

        if (CollectionTool.isNotEmpty(pageModel.getData())) {
            for (XxlJobOperateLog log : pageModel.getData()) {
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

        PageModel<Map<String, Object>> resultPageModel = new PageModel<>();
        resultPageModel.setData(resultList);
        resultPageModel.setTotal(pageModel.getTotal());

        return Response.ofSuccess(resultPageModel);
    }
}
