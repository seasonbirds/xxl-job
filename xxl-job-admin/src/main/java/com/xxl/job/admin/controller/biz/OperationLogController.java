package com.xxl.job.admin.controller.biz;

import com.xxl.job.admin.model.XxlJobOperationLog;
import com.xxl.job.admin.service.OperationLogService;
import com.xxl.sso.core.annotation.XxlSso;
import com.xxl.sso.core.helper.XxlSsoHelper;
import com.xxl.sso.core.model.LoginInfo;
import com.xxl.tool.response.PageModel;
import com.xxl.tool.response.Response;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * operation log controller
 *
 * @author xxl job
 */
@Controller
@RequestMapping("/operationLog")
public class OperationLogController {

    @Resource
    private OperationLogService operationLogService;

    @RequestMapping("")
    public String index(Model model) {
        // 获取当前登录用户
        LoginInfo loginInfo = SsoLoginUtil.getLoginInfo();
        if (loginInfo == null) {
            return "redirect:/login";
        }

        // 获取操作模块和类型列表
        Response<List<String>> modulesResponse = operationLogService.getOperationModules();
        Response<List<String>> typesResponse = operationLogService.getOperationTypes();

        model.addAttribute("modules", modulesResponse.getContent());
        model.addAttribute("types", typesResponse.getContent());

        return "biz/operation.log";
    }

    @RequestMapping("/pageList")
    @ResponseBody
    public Response<PageModel<XxlJobOperationLog>> pageList(@RequestParam(required = false, defaultValue = "0") int start,
                                                            @RequestParam(required = false, defaultValue = "10") int length,
                                                            @RequestParam(required = false) String operationModule,
                                                            @RequestParam(required = false) String operationType,
                                                            @RequestParam(required = false) String operator,
                                                            @RequestParam(required = false) String startTime,
                                                            @RequestParam(required = false) String endTime,
                                                            @RequestParam(required = false) String targetName) {

        // 转换时间格式
        Date startDate = null;
        Date endDate = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            if (startTime != null && !startTime.isEmpty()) {
                startDate = sdf.parse(startTime);
            }
            if (endTime != null && !endTime.isEmpty()) {
                endDate = sdf.parse(endTime);
            }
        } catch (ParseException e) {
            // ignore
        }

        return operationLogService.pageList(start, length, operationModule, operationType,
                operator, startDate, endDate, targetName);
    }

    @RequestMapping("/deleteByTime")
    @ResponseBody
    public Response<Void> deleteByTime(@RequestParam(required = false) String startTime,
                                       @RequestParam(required = false) String endTime) {
        // 转换时间格式
        Date startDate = null;
        Date endDate = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            if (startTime != null && !startTime.isEmpty()) {
                startDate = sdf.parse(startTime);
            }
            if (endTime != null && !endTime.isEmpty()) {
                endDate = sdf.parse(endTime);
            }
        } catch (ParseException e) {
            return Response.makeFail("时间格式错误");
        }

        return operationLogService.deleteByTime(startDate, endDate);
    }
}
