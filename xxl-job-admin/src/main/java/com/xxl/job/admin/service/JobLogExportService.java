package com.xxl.job.admin.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.xxl.job.admin.mapper.XxlJobGroupMapper;
import com.xxl.job.admin.mapper.XxlJobInfoMapper;
import com.xxl.job.admin.mapper.XxlJobLogMapper;
import com.xxl.job.admin.model.XxlJobGroup;
import com.xxl.job.admin.model.XxlJobInfo;
import com.xxl.job.admin.model.XxlJobLog;
import com.xxl.job.admin.model.XxlJobLogExcel;
import com.xxl.job.admin.util.I18nUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 调度日志导出服务
 * 提供基于EasyExcel的大数据量日志导出功能，包含并发控制、数据量限制、时间范围限制等安全措施
 *
 * @author xuxueli
 */
@Service
public class JobLogExportService {
    private static final Logger logger = LoggerFactory.getLogger(JobLogExportService.class);

    /**
     * 最大允许导出的日志数量：200万条
     * 超过此数量将拒绝导出，提示用户缩小查询条件
     */
    private static final int MAX_EXPORT_COUNT = 2000000;

    /**
     * 分批查询的批次大小：1万条
     * 采用分批查询+流式写入的方式，避免内存溢出
     */
    private static final int BATCH_SIZE = 10000;

    /**
     * 最大允许的时间范围：366天（约一年，考虑闰年）
     * 超过此范围将拒绝导出
     */
    private static final int MAX_DAYS_RANGE = 366;

    /**
     * 导出操作并发控制锁
     * 使用AtomicBoolean确保同一时间只有一个导出操作在执行
     * true表示正在导出，false表示空闲
     */
    private final AtomicBoolean exporting = new AtomicBoolean(false);

    @Resource
    private XxlJobLogMapper xxlJobLogMapper;

    @Resource
    private XxlJobInfoMapper xxlJobInfoMapper;

    @Resource
    private XxlJobGroupMapper xxlJobGroupMapper;

    /**
     * 尝试获取导出锁
     *
     * @return true-获取锁成功，false-获取锁失败（已有导出操作在进行）
     */
    public boolean tryLock() {
        return exporting.compareAndSet(false, true);
    }

    /**
     * 释放导出锁
     */
    public void unlock() {
        exporting.set(false);
    }

    /**
     * 检查是否正在导出
     *
     * @return true-正在导出，false-空闲
     */
    public boolean isExporting() {
        return exporting.get();
    }

    /**
     * 导出调度日志到Excel
     * 采用EasyExcel流式写入，分批查询数据，有效控制内存使用
     *
     * @param response         HTTP响应对象，用于写入Excel文件
     * @param jobGroup         执行器ID
     * @param jobId            任务ID
     * @param logStatus        日志状态（-1=全部，1=成功，2=失败，3=运行中）
     * @param triggerTimeStart 调度时间起始
     * @param triggerTimeEnd   调度时间结束
     * @throws IOException IO异常
     */
    public void exportLog(HttpServletResponse response,
                          int jobGroup,
                          int jobId,
                          int logStatus,
                          Date triggerTimeStart,
                          Date triggerTimeEnd) throws IOException {

        // 1. 并发控制：检查是否已有导出操作在进行
        if (!tryLock()) {
            throw new RuntimeException(I18nUtil.getString("joblog_export_processing"));
        }

        try {
            // 2. 时间范围校验：必须提供时间范围且不超过一年
            if (triggerTimeStart == null || triggerTimeEnd == null) {
                throw new RuntimeException(I18nUtil.getString("joblog_export_timerange_limit"));
            }
            long diffDays = (triggerTimeEnd.getTime() - triggerTimeStart.getTime()) / (1000 * 60 * 60 * 24);
            if (diffDays > MAX_DAYS_RANGE) {
                throw new RuntimeException(I18nUtil.getString("joblog_export_timerange_limit"));
            }

            // 3. 数据量校验：查询符合条件的总记录数，超过200万拒绝导出
            int totalCount = xxlJobLogMapper.pageListCount(0, 1, jobGroup, jobId, triggerTimeStart, triggerTimeEnd, logStatus);
            if (totalCount > MAX_EXPORT_COUNT) {
                throw new RuntimeException(I18nUtil.getString("joblog_export_too_much"));
            }

            // 4. 构建导出文件名：${执行器名称}-${任务名称}-${datetime}.xlsx
            XxlJobInfo jobInfo = xxlJobInfoMapper.loadById(jobId);
            XxlJobGroup jobGroupObj = xxlJobGroupMapper.load(jobGroup);

            String jobGroupName = jobGroupObj != null ? jobGroupObj.getTitle() : "unknown";
            String jobName = jobInfo != null ? jobInfo.getJobDesc() : "unknown";
            String datetime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String fileName = jobGroupName + "-" + jobName + "-" + datetime + ".xlsx";

            // 5. 设置HTTP响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            // 兼容旧版浏览器使用filename，新版浏览器使用filename*
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"; filename*=utf-8''" + encodedFileName);

            // 6. 使用EasyExcel流式写入Excel
            // 采用分批查询+流式写入的方式，避免大量数据占用内存
            try (ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream(), XxlJobLogExcel.class).build()) {
                WriteSheet writeSheet = EasyExcel.writerSheet("调度日志").build();

                int offset = 0;
                while (offset < totalCount) {
                    // 分批查询数据，每次查询BATCH_SIZE条
                    List<XxlJobLog> logList = xxlJobLogMapper.pageList(offset, BATCH_SIZE, jobGroup, jobId, triggerTimeStart, triggerTimeEnd, logStatus);
                    if (logList == null || logList.isEmpty()) {
                        break;
                    }

                    // 转换为Excel导出模型并写入
                    List<XxlJobLogExcel> excelList = convertToExcelList(logList, jobGroupName, jobName);
                    excelWriter.write(excelList, writeSheet);

                    offset += BATCH_SIZE;
                }
            }

            logger.info("Export job log success, jobGroup={}, jobId={}, count={}", jobGroup, jobId, totalCount);
        } finally {
            // 确保释放锁（使用try-finally保证异常时也能释放）
            unlock();
        }
    }

    /**
     * 将XxlJobLog实体转换为Excel导出模型
     *
     * @param logList       原始日志列表
     * @param jobGroupName  执行器名称
     * @param jobName       任务名称
     * @return Excel导出模型列表
     */
    private List<XxlJobLogExcel> convertToExcelList(List<XxlJobLog> logList, String jobGroupName, String jobName) {
        List<XxlJobLogExcel> excelList = new ArrayList<>(logList.size());
        for (XxlJobLog log : logList) {
            XxlJobLogExcel excel = new XxlJobLogExcel();
            excel.setId(log.getId());
            excel.setJobGroupName(jobGroupName);
            excel.setJobName(jobName);
            excel.setExecutorAddress(log.getExecutorAddress());
            excel.setExecutorHandler(log.getExecutorHandler());
            excel.setExecutorParam(log.getExecutorParam());
            excel.setTriggerTime(log.getTriggerTime());
            excel.setTriggerResult(formatTriggerCode(log.getTriggerCode()));
            excel.setTriggerMsg(log.getTriggerMsg());
            excel.setHandleTime(log.getHandleTime());
            excel.setHandleResult(formatHandleCode(log.getHandleCode()));
            excel.setHandleMsg(log.getHandleMsg());
            excelList.add(excel);
        }
        return excelList;
    }

    /**
     * 格式化调度结果码
     *
     * @param code 调度结果码
     * @return 格式化后的结果字符串（成功/失败）
     */
    private String formatTriggerCode(int code) {
        if (code == 200) {
            return I18nUtil.getString("system_success");
        } else if (code > 0) {
            return I18nUtil.getString("system_fail");
        } else {
            return "";
        }
    }

    /**
     * 格式化执行结果码
     *
     * @param code 执行结果码
     * @return 格式化后的结果字符串（成功/失败/超时）
     */
    private String formatHandleCode(int code) {
        if (code == 200) {
            return I18nUtil.getString("joblog_handleCode_200");
        } else if (code == 502) {
            return I18nUtil.getString("joblog_handleCode_502");
        } else if (code > 0) {
            return I18nUtil.getString("joblog_handleCode_500");
        } else {
            return "";
        }
    }
}
