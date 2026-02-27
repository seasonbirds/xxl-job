package com.xxl.job.admin.controller.biz;

import com.xxl.job.admin.constant.Consts;
import com.xxl.job.admin.model.XxlJobGroup;
import com.xxl.job.admin.model.XxlJobOpLog;
import com.xxl.job.admin.service.XxlJobOpLogService;
import com.xxl.job.admin.util.I18nUtil;
import com.xxl.sso.core.annotation.XxlSso;
import com.xxl.tool.core.DateTool;
import com.xxl.tool.core.StringTool;
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
 * 操作日志控制器
 * @author xxl-job
 */
@Controller
@RequestMapping("/oplog")
public class JobOpLogController {

    @Resource
    private XxlJobOpLogService xxlJobOpLogService;

    @RequestMapping
    @XxlSso(role = Consts.ADMIN_ROLE)
    public String index(Model model) {
        return "biz/oplog.list";
    }

    @RequestMapping("/pageList")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<PageModel<XxlJobOpLog>> pageList(
            @RequestParam(required = false, defaultValue = "0") int offset,
            @RequestParam(required = false, defaultValue = "10") int pagesize,
            @RequestParam(required = false, defaultValue = "") String module,
            @RequestParam(required = false, defaultValue = "") String type,
            @RequestParam(required = false, defaultValue = "") String operator,
            @RequestParam(required = false, defaultValue = "0") int jobGroup,
            @RequestParam(required = false, defaultValue = "") String filterTime) {

        // parse param
        Date opTimeStart = null;
        Date opTimeEnd = null;
        if (StringTool.isNotBlank(filterTime)) {
            String[] temp = filterTime.split(" - ");
            if (temp.length == 2) {
                opTimeStart = DateTool.parseDateTime(temp[0]);
                opTimeEnd = DateTool.parseDateTime(temp[1]);
            }
        }

        // page list
        return xxlJobOpLogService.pageList(offset, pagesize, module, type, operator, jobGroup > 0 ? jobGroup : null, opTimeStart, opTimeEnd);
    }

}
