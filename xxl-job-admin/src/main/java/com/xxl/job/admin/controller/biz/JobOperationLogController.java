package com.xxl.job.admin.controller.biz;

import com.xxl.job.admin.model.XxlJobGroup;
import com.xxl.job.admin.model.XxlJobOperationLog;
import com.xxl.job.admin.model.enums.OperationModule;
import com.xxl.job.admin.model.enums.OperationType;
import com.xxl.job.admin.service.XxlJobOperationLogService;
import com.xxl.job.admin.util.I18nUtil;
import com.xxl.job.admin.mapper.XxlJobGroupMapper;
import com.xxl.sso.core.annotation.XxlSso;
import com.xxl.tool.core.StringTool;
import com.xxl.tool.response.PageModel;
import com.xxl.tool.response.Response;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/joboperationlog")
public class JobOperationLogController {

    @Resource
    private XxlJobOperationLogService xxlJobOperationLogService;

    @Resource
    private XxlJobGroupMapper xxlJobGroupMapper;

    @RequestMapping
    @XxlSso
    public String index(Model model) {
        List<XxlJobGroup> jobGroupList = xxlJobGroupMapper.findAll();
        model.addAttribute("JobGroupList", jobGroupList);
        model.addAttribute("OperationModuleEnum", OperationModule.values());
        model.addAttribute("OperationTypeEnum", OperationType.values());
        return "biz/log.operation";
    }

    @RequestMapping("/pageList")
    @ResponseBody
    @XxlSso
    public Response<PageModel<XxlJobOperationLog>> pageList(@RequestParam(required = false, defaultValue = "0") int offset,
                                                             @RequestParam(required = false, defaultValue = "10") int pagesize,
                                                             @RequestParam(required = false) String operationModule,
                                                             @RequestParam(required = false) String operationType,
                                                             @RequestParam(required = false) String operatorName,
                                                             @RequestParam(required = false) String operationTimeStart,
                                                             @RequestParam(required = false) String operationTimeEnd,
                                                             @RequestParam(required = false) Integer jobGroup) {

        PageModel<XxlJobOperationLog> pageModel = xxlJobOperationLogService.pageList(
                offset, pagesize, operationModule, operationType, operatorName, operationTimeStart, operationTimeEnd, jobGroup);
        return Response.ofSuccess(pageModel);
    }
}
