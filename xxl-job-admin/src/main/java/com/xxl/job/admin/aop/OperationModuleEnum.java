package com.xxl.job.admin.aop;

import com.xxl.job.admin.util.I18nUtil;

/**
 * operation module enum
 *
 * @author xxl-job
 */
public enum OperationModuleEnum {

    LOGIN("operation_log_module_login"),      // 登录模块
    USER("operation_log_module_user"),        // 用户管理模块
    GROUP("operation_log_module_group"),      // 执行器管理模块
    JOB("operation_log_module_job");          // 任务管理模块

    private final String i18nKey;

    OperationModuleEnum(String i18nKey) {
        this.i18nKey = i18nKey;
    }

    public String getTitle() {
        return I18nUtil.getString(i18nKey);
    }

}
