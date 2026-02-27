package com.xxl.job.admin.controller.biz;

import com.xxl.job.admin.constant.Consts;
import com.xxl.job.admin.model.XxlJobGroup;
import com.xxl.job.admin.model.XxlJobOperationLog;
import com.xxl.job.admin.service.XxlJobOperationLogService;
import com.xxl.sso.core.annotation.XxlSso;
import com.xxl.tool.response.PageModel;
import com.xxl.tool.response.Response;
import jakarta.annotation.Resource;
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
 * @author xxl-job 2024
 */
@Controller
@RequestMapping("/operationlog")
public class OperationLogController {

    @Resource
    private XxlJobOperationLogService xxlJobOperationLogService;

    /**
     * operation log page
     *
     * @param model model
     * @return page path
     */
    @RequestMapping
    @XxlSso(role = Consts.ADMIN_ROLE)
    public String index(Model model) {
        List<XxlJobGroup> groupList = xxlJobOperationLogService.findAllJobGroup();
        model.addAttribute("groupList", groupList);
        return "biz/operationlog.list";
    }

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
    @RequestMapping("/pageList")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<PageModel<XxlJobOperationLog>> pageList(
            @RequestParam(required = false, defaultValue = "0") int offset,
            @RequestParam(required = false, defaultValue = "10") int pagesize,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) Date operateTimeStart,
            @RequestParam(required = false) Date operateTimeEnd,
            @RequestParam(required = false, defaultValue = "-1") int jobGroupId,
            @RequestParam(required = false) String targetName) {

        return xxlJobOperationLogService.pageList(offset, pagesize, module, operationType,
                operator, operateTimeStart, operateTimeEnd, jobGroupId > 0 ? jobGroupId : null, targetName);
    }

    /**
     * clear log
     *
     * @param type clear type
     * @return result
     */
    @RequestMapping("/clearLog")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<String> clearLog(@RequestParam int type) {
        return xxlJobOperationLogService.clearLog(type);
    }
}
