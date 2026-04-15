package com.xxl.job.admin.controller.biz;

import com.xxl.job.admin.constant.Consts;
import com.xxl.job.admin.model.XxlJobGroup;
import com.xxl.job.admin.service.XxlJobOperateLogService;
import com.xxl.job.admin.util.I18nUtil;
import com.xxl.sso.core.annotation.XxlSso;
import com.xxl.tool.response.PageModel;
import com.xxl.tool.response.Response;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/**
 * 操作日志Controller
 * @author xxl-job
 */
@Controller
@RequestMapping("/operateLog")
public class JobOperateLogController {

    @Resource
    private XxlJobOperateLogService xxlJobOperateLogService;

    @RequestMapping
    @XxlSso(role = Consts.ADMIN_ROLE)
    public String index(Model model) {
        List<XxlJobGroup> groupList = xxlJobOperateLogService.findAllGroups();
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

        return xxlJobOperateLogService.pageListWithDetail(
                offset, pagesize, module, action, operator, targetName, filterTime, targetId);
    }
}
