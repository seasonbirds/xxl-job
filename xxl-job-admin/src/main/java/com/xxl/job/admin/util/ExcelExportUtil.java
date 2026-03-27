package com.xxl.job.admin.util;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.xxl.job.admin.model.XxlJobInfo;
import com.xxl.job.admin.model.XxlJobLog;
import com.xxl.job.admin.mapper.XxlJobInfoMapper;
import com.xxl.tool.core.DateTool;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Excel导出工具类，使用EasyExcel实现
 * 特点：
 * 1. 内存占用低，避免OOM
 * 2. 性能高效，支持大数据量导出
 * 3. 自动处理内存回收
 *
 * @author xuxueli
 */
@Component
public class ExcelExportUtil {

    private static final Logger logger = LoggerFactory.getLogger(ExcelExportUtil.class);

    @Resource
    private XxlJobInfoMapper xxlJobInfoMapper;

    /**
     * 导出调度日志到Excel
     *
     * @param response HTTP响应
     * @param fileName 文件名
     * @param logs     日志数据列表
     * @throws IOException IO异常
     */
    public void exportJobLogs(HttpServletResponse response, String fileName, List<XxlJobLog> logs) throws IOException {
        try {
            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            // 这里URLEncoder.encode可以防止中文乱码 当然和easyexcel没有关系
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFileName);

            // 转换日志数据到Excel模型
            List<JobLogExcelModel> excelModels = convertToExcelModels(logs);

            // 使用EasyExcel写出
            EasyExcel.write(response.getOutputStream(), JobLogExcelModel.class)
                    .autoCloseStream(Boolean.FALSE)
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .sheet("任务调度日志")
                    .doWrite(excelModels);
        } catch (Exception e) {
            logger.error("export job logs to excel error: {}", e.getMessage(), e);
            // 重置response
            response.reset();
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            response.getWriter().println(I18nUtil.getString("system_error") + ": " + e.getMessage());
        }
    }

    /**
     * 将XxlJobLog转换为JobLogExcelModel
     *
     * @param logs 日志列表
     * @return Excel模型列表
     */
    private List<JobLogExcelModel> convertToExcelModels(List<XxlJobLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量获取所有任务ID，避免N+1查询
        List<Integer> jobIds = logs.stream()
                .map(XxlJobLog::getJobId)
                .distinct()
                .collect(Collectors.toList());

        // 批量查询所有任务信息并转换为Map
        Map<Integer, XxlJobInfo> jobInfoMap = new HashMap<>();
        if (!jobIds.isEmpty()) {
            List<XxlJobInfo> jobInfos = xxlJobInfoMapper.loadByIds(jobIds);
            if (jobInfos != null && !jobInfos.isEmpty()) {
                jobInfoMap = jobInfos.stream()
                        .collect(Collectors.toMap(XxlJobInfo::getId, jobInfo -> jobInfo));
            }
        }

        return logs.stream().map(log -> {
            JobLogExcelModel model = new JobLogExcelModel();
            model.setId(log.getId());

            // 任务名称
            XxlJobInfo jobInfo = jobInfoMap.get(log.getJobId());
            String jobName = "【" + log.getJobId() + "】" + (jobInfo != null ? jobInfo.getJobDesc() : "");
            model.setJobName(jobName);

            // 调度时间
            model.setTriggerTime(log.getTriggerTime() != null ? DateTool.formatDateTime(log.getTriggerTime()) : "");

            // 调度结果
            String triggerResult = "";
            if (log.getTriggerCode() == 200) {
                triggerResult = I18nUtil.getString("system_success");
            } else if (log.getTriggerCode() > 0) {
                triggerResult = I18nUtil.getString("system_fail");
            }
            model.setTriggerCode(triggerResult);

            // 调度备注
            model.setTriggerMsg(log.getTriggerMsg() != null ? log.getTriggerMsg() : "");

            // 执行时间
            model.setHandleTime(log.getHandleTime() != null ? DateTool.formatDateTime(log.getHandleTime()) : "");

            // 执行结果
            String handleResult = "";
            if (log.getHandleCode() == 200) {
                handleResult = I18nUtil.getString("joblog_handleCode_200");
            } else if (log.getHandleCode() == 502) {
                handleResult = I18nUtil.getString("joblog_handleCode_502");
            } else if (log.getHandleCode() > 0) {
                handleResult = I18nUtil.getString("joblog_handleCode_500");
            }
            model.setHandleCode(handleResult);

            // 执行备注
            model.setHandleMsg(log.getHandleMsg() != null ? log.getHandleMsg() : "");

            return model;
        }).collect(Collectors.toList());
    }

    /**
     * Excel导出模型
     */
    public static class JobLogExcelModel {
        /**
         * 日志ID
         */
        @ExcelProperty("日志ID")
        private Long id;

        /**
         * 任务名称
         */
        @ExcelProperty("任务名称")
        private String jobName;

        /**
         * 调度时间
         */
        @ExcelProperty("调度时间")
        private String triggerTime;

        /**
         * 调度结果
         */
        @ExcelProperty("调度结果")
        private String triggerCode;

        /**
         * 调度备注
         */
        @ExcelProperty("调度备注")
        private String triggerMsg;

        /**
         * 执行时间
         */
        @ExcelProperty("执行时间")
        private String handleTime;

        /**
         * 执行结果
         */
        @ExcelProperty("执行结果")
        private String handleCode;

        /**
         * 执行备注
         */
        @ExcelProperty("执行备注")
        private String handleMsg;

        // Getters and Setters
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

        public String getTriggerTime() {
            return triggerTime;
        }

        public void setTriggerTime(String triggerTime) {
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

        public String getHandleTime() {
            return handleTime;
        }

        public void setHandleTime(String handleTime) {
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
}