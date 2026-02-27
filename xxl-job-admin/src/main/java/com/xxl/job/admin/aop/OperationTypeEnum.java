package com.xxl.job.admin.aop;

import com.xxl.job.admin.util.I18nUtil;

/**
 * operation type enum
 *
 * @author xxl-job
 */
public enum OperationTypeEnum {

    LOGIN("operation_log_type_login"),
    LOGOUT("operation_log_type_logout"),
    ADD("operation_log_type_add"),
    UPDATE("operation_log_type_update"),
    DELETE("operation_log_type_delete"),
    START("operation_log_type_start"),
    STOP("operation_log_type_stop");

    private final String i18nKey;

    OperationTypeEnum(String i18nKey) {
        this.i18nKey = i18nKey;
    }

    public String getTitle() {
        return I18nUtil.getString(i18nKey);
    }

}
