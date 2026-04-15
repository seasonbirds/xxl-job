package com.xxl.job.admin.model;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;

import java.util.Date;

public class XxlJobLogExcel {

    @ExcelProperty(value = "日志ID", index = 0)
    @ColumnWidth(15)
    private long id;

    @ExcelProperty(value = "执行器名称", index = 1)
    @ColumnWidth(20)
    private String jobGroupName;

    @ExcelProperty(value = "任务名称", index = 2)
    @ColumnWidth(20)
    private String jobName;

    @ExcelProperty(value = "执行器地址", index = 3)
    @ColumnWidth(25)
    private String executorAddress;

    @ExcelProperty(value = "执行器Handler", index = 4)
    @ColumnWidth(25)
    private String executorHandler;

    @ExcelProperty(value = "任务参数", index = 5)
    @ColumnWidth(30)
    private String executorParam;

    @ExcelProperty(value = "调度时间", index = 6)
    @ColumnWidth(20)
    private Date triggerTime;

    @ExcelProperty(value = "调度结果", index = 7)
    @ColumnWidth(15)
    private String triggerResult;

    @ExcelProperty(value = "调度备注", index = 8)
    @ColumnWidth(50)
    private String triggerMsg;

    @ExcelProperty(value = "执行时间", index = 9)
    @ColumnWidth(20)
    private Date handleTime;

    @ExcelProperty(value = "执行结果", index = 10)
    @ColumnWidth(15)
    private String handleResult;

    @ExcelProperty(value = "执行备注", index = 11)
    @ColumnWidth(50)
    private String handleMsg;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getJobGroupName() {
        return jobGroupName;
    }

    public void setJobGroupName(String jobGroupName) {
        this.jobGroupName = jobGroupName;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getExecutorAddress() {
        return executorAddress;
    }

    public void setExecutorAddress(String executorAddress) {
        this.executorAddress = executorAddress;
    }

    public String getExecutorHandler() {
        return executorHandler;
    }

    public void setExecutorHandler(String executorHandler) {
        this.executorHandler = executorHandler;
    }

    public String getExecutorParam() {
        return executorParam;
    }

    public void setExecutorParam(String executorParam) {
        this.executorParam = executorParam;
    }

    public Date getTriggerTime() {
        return triggerTime;
    }

    public void setTriggerTime(Date triggerTime) {
        this.triggerTime = triggerTime;
    }

    public String getTriggerResult() {
        return triggerResult;
    }

    public void setTriggerResult(String triggerResult) {
        this.triggerResult = triggerResult;
    }

    public String getTriggerMsg() {
        return triggerMsg;
    }

    public void setTriggerMsg(String triggerMsg) {
        this.triggerMsg = triggerMsg;
    }

    public Date getHandleTime() {
        return handleTime;
    }

    public void setHandleTime(Date handleTime) {
        this.handleTime = handleTime;
    }

    public String getHandleResult() {
        return handleResult;
    }

    public void setHandleResult(String handleResult) {
        this.handleResult = handleResult;
    }

    public String getHandleMsg() {
        return handleMsg;
    }

    public void setHandleMsg(String handleMsg) {
        this.handleMsg = handleMsg;
    }
}
