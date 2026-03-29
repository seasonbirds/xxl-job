package com.xxl.job.admin.model.enums;

public enum OperationModule {
    
    LOGIN("login", "登录"),
    USER_MANAGE("user_manage", "用户管理"),
    EXECUTOR_MANAGE("executor_manage", "执行器管理"),
    TASK_MANAGE("task_manage", "任务管理");
    
    private final String code;
    private final String desc;
    
    OperationModule(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDesc() {
        return desc;
    }
    
    public static OperationModule match(String code, OperationModule defaultItem) {
        if (code != null) {
            for (OperationModule item : OperationModule.values()) {
                if (item.getCode().equals(code)) {
                    return item;
                }
            }
        }
        return defaultItem;
    }
}
