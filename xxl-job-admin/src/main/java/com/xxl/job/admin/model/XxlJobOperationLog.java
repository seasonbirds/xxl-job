package com.xxl.job.admin.model;

import java.util.Date;

/**
 * operation log entity
 *
 * @author xxl-job
 */
public class XxlJobOperationLog {

    private int id;
    private String module;			// 模块：LOGIN-登录, USER-用户管理, GROUP-执行器管理, JOB-任务管理
    private String operationType;	// 操作类型：LOGIN-登录, ADD-新增, UPDATE-编辑, DELETE-删除, START-启动, STOP-停止
    private String operator;		// 操作人账号
    private Date operationTime;		// 操作时间
    private String ip;				// 操作IP
    private String targetId;		// 目标ID（登录时为账号，其他模块为主键ID）
    private String targetName;		// 目标名称（账号、执行器名称、任务描述等）
    private Integer jobGroup;		// 执行器ID（任务管理时记录所属执行器）
    private String extraInfo;		// 扩展信息

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

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Date getOperationTime() {
        return operationTime;
    }

    public void setOperationTime(Date operationTime) {
        this.operationTime = operationTime;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
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

    public Integer getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(Integer jobGroup) {
        this.jobGroup = jobGroup;
    }

    public String getExtraInfo() {
        return extraInfo;
    }

    public void setExtraInfo(String extraInfo) {
        this.extraInfo = extraInfo;
    }

}
