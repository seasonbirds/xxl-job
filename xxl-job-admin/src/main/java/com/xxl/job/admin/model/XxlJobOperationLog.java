package com.xxl.job.admin.model;

import java.util.Date;

/**
 * operation log entity
 * 
 * <p>
 *     This entity represents an operation log record in the system.
 *     It stores information about user operations such as login, user management,
 *     executor management, and task management.
 * </p>
 *
 * @author xxl-job 2024
 */
public class XxlJobOperationLog {

    /**
     * primary key id
     */
    private long id;

    /**
     * operation module: LOGIN, USER, JOBGROUP, JOBINFO
     */
    private String module;

    /**
     * operation type: 登录, 新增, 编辑, 删除, 启动, 停止
     */
    private String operationType;

    /**
     * operator username
     */
    private String operator;

    /**
     * operate time
     */
    private Date operateTime;

    /**
     * client ip address
     */
    private String ip;

    /**
     * target object id (user id, job group id, job info id)
     */
    private Integer targetId;

    /**
     * target object name (username, job group title, job desc)
     */
    private String targetName;

    /**
     * job group id (only for JOBINFO module)
     */
    private Integer jobGroupId;

    /**
     * job group name (only for JOBINFO module)
     */
    private String jobGroupName;

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

    public Integer getJobGroupId() {
        return jobGroupId;
    }

    public void setJobGroupId(Integer jobGroupId) {
        this.jobGroupId = jobGroupId;
    }

    public String getJobGroupName() {
        return jobGroupName;
    }

    public void setJobGroupName(String jobGroupName) {
        this.jobGroupName = jobGroupName;
    }
}
