package com.xxl.job.admin.controller.biz;

import com.xxl.job.admin.aop.OperationModuleEnum;
import com.xxl.job.admin.aop.OperationTypeEnum;
import com.xxl.job.admin.mapper.XxlJobGroupMapper;
import com.xxl.job.admin.model.XxlJobGroup;
import com.xxl.job.admin.model.XxlJobOperationLog;
import com.xxl.job.admin.service.OperationLogService;
import com.xxl.job.admin.util.I18nUtil;
import com.xxl.tool.core.DateTool;
import com.xxl.tool.core.StringTool;
import com.xxl.tool.response.PageModel;
import com.xxl.tool.response.Response;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;
import java.util.List;

/**
 * operation log controller
 *
 * @author xxl-job
 */
@Controller
@RequestMapping("/operationLog")
public class OperationLogController {

    @Resource
    private XxlJobGroupMapper xxlJobGroupMapper;
    @Resource
    private OperationLogService operationLogService;

    @RequestMapping
    public String index(Model model) {
        List<XxlJobGroup> jobGroupList = xxlJobGroupMapper.findAll();
        model.addAttribute("JobGroupList", jobGroupList);
        model.addAttribute("moduleList", OperationModuleEnum.values());
        model.addAttribute("operationTypeList", OperationTypeEnum.values());
        return "biz/operationLog.list";
    }

    @RequestMapping("/pageList")
    @ResponseBody
    public Response<PageModel<XxlJobOperationLog>> pageList(
            @RequestParam(required = false, defaultValue = "0") int offset,
            @RequestParam(required = false, defaultValue = "10") int pagesize,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String targetName,
            @RequestParam(required = false, defaultValue = "0") Integer jobGroup,
            @RequestParam(required = false) String filterTime) {

        Date startTime = null;
        Date endTime = null;
        if (StringTool.isNotBlank(filterTime)) {
            String[] temp = filterTime.split(" - ");
            if (temp.length == 2) {
                startTime = DateTool.parseDateTime(temp[0]);
                endTime = DateTool.parseDateTime(temp[1]);
            }
        }

        if (jobGroup != null && jobGroup <= 0) {
            jobGroup = null;
        }

        return operationLogService.pageList(offset, pagesize,
                module, operationType, operator, targetName, jobGroup, startTime, endTime);
    }

}
