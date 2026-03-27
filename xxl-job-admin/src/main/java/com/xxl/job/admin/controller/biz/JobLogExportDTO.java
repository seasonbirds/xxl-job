package com.xxl.job.admin.controller.biz;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;

import java.util.Date;

/**
 * 任务日志导出DTO
 *
 * @author xuxueli
 */
@HeadRowHeight(20)
@ContentRowHeight(18)
public class JobLogExportDTO {

    @ExcelProperty("日志ID")
    @ColumnWidth(15)
    private Long id;

    @ExcelProperty("任务")
    @ColumnWidth(30)
    private String jobName;

    @ExcelProperty("调度时间")
    @ColumnWidth(25)
    private Date triggerTime;

    @ExcelProperty("调度结果")
    @ColumnWidth(15)
    private String triggerCode;

    @ExcelProperty("调度备注")
    @ColumnWidth(30)
    private String triggerMsg;

    @ExcelProperty("执行时间")
    @ColumnWidth(25)
    private Date handleTime;

    @ExcelProperty("执行结果")
    @ColumnWidth(15)
    private String handleCode;

    @ExcelProperty("执行备注")
    @ColumnWidth(30)
    private String handleMsg;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public Date getTriggerTime() {
        return triggerTime;
    }

    public void setTriggerTime(Date triggerTime) {
        this.triggerTime = triggerTime;
    }

    public String getTriggerCode() {
        return triggerCode;
    }

    public void setTriggerCode(String triggerCode) {
        this.triggerCode = triggerCode;
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

    public String getHandleCode() {
        return handleCode;
    }

    public void setHandleCode(String handleCode) {
        this.handleCode = handleCode;
    }

    public String getHandleMsg() {
        return handleMsg;
    }

    public void setHandleMsg(String handleMsg) {
        this.handleMsg = handleMsg;
    }
}
