package com.xxl.job.admin.model.enums;

public enum OperationType {
    
    LOGIN("login", "登录"),
    LOGOUT("logout", "登出"),
    ADD("add", "新增"),
    EDIT("edit", "编辑"),
    DELETE("delete", "删除"),
    START("start", "启动"),
    STOP("stop", "停止");
    
    private final String code;
    private final String desc;
    
    OperationType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDesc() {
        return desc;
    }
    
    public static OperationType match(String code, OperationType defaultItem) {
        if (code != null) {
            for (OperationType item : OperationType.values()) {
                if (item.getCode().equals(code)) {
                    return item;
                }
            }
        }
        return defaultItem;
    }
}
