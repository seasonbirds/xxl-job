package com.xxl.job.admin.util;

import com.xxl.job.admin.constant.Consts;
import com.xxl.job.admin.mapper.XxlJobInfoMapper;
import com.xxl.job.admin.model.XxlJobInfo;
import com.xxl.sso.core.helper.XxlSsoHelper;
import com.xxl.sso.core.model.LoginInfo;
import jakarta.servlet.http.HttpServletRequest;
import com.xxl.tool.response.Response;
import org.springframework.stereotype.Component;

@Component
public class DataPermissionUtil {

    public static boolean isAdmin(LoginInfo loginInfo) {
        return XxlSsoHelper.hasRole(loginInfo, Consts.ADMIN_ROLE).isSuccess();
    }

    public static boolean hasJobDataPermission(LoginInfo loginInfo, String author) {
        if (isAdmin(loginInfo)) {
            return true;
        }
        return loginInfo.getUserName() != null && loginInfo.getUserName().equals(author);
    }

    public static boolean hasJobDataPermission(LoginInfo loginInfo, XxlJobInfo jobInfo) {
        if (jobInfo == null) {
            return false;
        }
        return hasJobDataPermission(loginInfo, jobInfo.getAuthor());
    }

    public static void validJobDataPermission(HttpServletRequest request, XxlJobInfoMapper xxlJobInfoMapper, int jobId) {
        Response<LoginInfo> loginInfoResponse = XxlSsoHelper.loginCheckWithAttr(request);
        if (!isAdmin(loginInfoResponse.getData())) {
            XxlJobInfo jobInfo = xxlJobInfoMapper.loadById(jobId);
            if (jobInfo == null || !hasJobDataPermission(loginInfoResponse.getData(), jobInfo)) {
                throw new RuntimeException(I18nUtil.getString("system_permission_limit") + "[username=" + loginInfoResponse.getData().getUserName() + "]");
            }
        }
    }

    public static void validJobDataPermission(LoginInfo loginInfo, XxlJobInfo jobInfo) {
        if (!hasJobDataPermission(loginInfo, jobInfo)) {
            throw new RuntimeException(I18nUtil.getString("system_permission_limit") + "[username=" + loginInfo.getUserName() + "]");
        }
    }
}
