package com.xxl.job.admin.model;

import java.util.Date;

/**
 * 操作日志实体类
 * @author xxl-job
 */
public class XxlJobOperateLog {

    private int id;

    private String module;

    private String action;

    private String operator;

    private Date operateTime;

    private String ip;

    private Integer targetId;

    private String targetName;

    private String extraData;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Date getOperateTime() {
        return operateTime;
    }

    public void setOperateTime(Date operateTime) {
        this.operateTime = operateTime;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getTargetId() {
        return targetId;
    }

    public void setTargetId(Integer targetId) {
        this.targetId = targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public String getExtraData() {
        return extraData;
    }

    public void setExtraData(String extraData) {
        this.extraData = extraData;
    }

    public enum Module {
        LOGIN("LOGIN", "登录"),
        USER("USER", "用户管理"),
        JOB_GROUP("JOB_GROUP", "执行器管理"),
        JOB_INFO("JOB_INFO", "任务管理");

        private String code;
        private String desc;

        Module(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public String getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }
    }

    public enum Action {
        ADD("ADD", "新增"),
        UPDATE("UPDATE", "编辑"),
        DELETE("DELETE", "删除"),
        START("START", "启动"),
        STOP("STOP", "停止"),
        LOGIN("LOGIN", "登录"),
        LOGOUT("LOGOUT", "注销");

        private String code;
        private String desc;

        Action(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public String getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }
    }
}
