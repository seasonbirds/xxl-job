package com.xxl.job.admin.model;

import java.util.Date;

/**
 * xxl-job operation log, used to track user operations
 * @author system 2026-03-29
 */
public class XxlJobOperationLog {
	
	private long id;
	
	// operation info
	private String operationType;	// 操作类型：LOGIN, ADD, UPDATE, DELETE, START, STOP
	private String operationModule;	// 操作模块：LOGIN, USER, EXECUTOR, JOB
	private String operator;		// 操作人账号
	private Date operationTime;		// 操作时间
	private String operationIp;		// 操作IP地址
	
	// target info
	private Integer targetId;		// 目标对象ID（用户ID、执行器ID、任务ID等）
	private String targetName;		// 目标对象名称（用户账号、执行器名称、任务描述等）
	private String targetExtra;		// 额外信息
	private String remark;			// 备注
	
	
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}
	
	public String getOperationType() {
		return operationType;
	}
	
	public void setOperationType(String operationType) {
		this.operationType = operationType;
	}
	
	public String getOperationModule() {
		return operationModule;
	}
	
	public void setOperationModule(String operationModule) {
		this.operationModule = operationModule;
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
	
	public String getOperationIp() {
		return operationIp;
	}
	
	public void setOperationIp(String operationIp) {
		this.operationIp = operationIp;
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
	
	public String getTargetExtra() {
		return targetExtra;
	}
	
	public void setTargetExtra(String targetExtra) {
		this.targetExtra = targetExtra;
	}
	
	public String getRemark() {
		return remark;
	}
	
	public void setRemark(String remark) {
		this.remark = remark;
	}
	
}
