package com.xxl.job.admin.model;

import java.util.Date;

/**
 * 操作日志实体类
 * @author xxl-job
 */
public class XxlJobOpLog {

    private long id;

    /**
     * 操作模块：LOGIN-登录、USER-用户管理、JOB_GROUP-执行器管理、JOB_INFO-任务管理
     */
    private String module;

    /**
     * 操作类型
     */
    private String type;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 操作时间
     */
    private Date opTime;

    /**
     * 操作IP地址
     */
    private String opIp;

    /**
     * 被操作对象ID
     */
    private String targetId;

    /**
     * 被操作对象名称
     */
    private String targetName;

    /**
     * 操作内容描述
     */
    private String content;

    /**
     * 执行器ID（仅任务管理模块使用）
     */
    private Integer jobGroup;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Date getOpTime() {
        return opTime;
    }

    public void setOpTime(Date opTime) {
        this.opTime = opTime;
    }

    public String getOpIp() {
        return opIp;
    }

    public void setOpIp(String opIp) {
        this.opIp = opIp;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(Integer jobGroup) {
        this.jobGroup = jobGroup;
    }
}
