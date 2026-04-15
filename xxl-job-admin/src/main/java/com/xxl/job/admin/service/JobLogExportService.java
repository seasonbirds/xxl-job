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

@Service
public class JobLogExportService {
    private static final Logger logger = LoggerFactory.getLogger(JobLogExportService.class);

    private static final int MAX_EXPORT_COUNT = 2000000;
    private static final int BATCH_SIZE = 10000;
    private static final int MAX_DAYS_RANGE = 366;

    private final AtomicBoolean exporting = new AtomicBoolean(false);

    @Resource
    private XxlJobLogMapper xxlJobLogMapper;

    @Resource
    private XxlJobInfoMapper xxlJobInfoMapper;

    @Resource
    private XxlJobGroupMapper xxlJobGroupMapper;

    public boolean tryLock() {
        return exporting.compareAndSet(false, true);
    }

    public void unlock() {
        exporting.set(false);
    }

    public boolean isExporting() {
        return exporting.get();
    }

    public void exportLog(HttpServletResponse response,
                          int jobGroup,
                          int jobId,
                          int logStatus,
                          Date triggerTimeStart,
                          Date triggerTimeEnd) throws IOException {

        if (!tryLock()) {
            throw new RuntimeException(I18nUtil.getString("joblog_export_processing"));
        }

        try {
            if (triggerTimeStart != null && triggerTimeEnd != null) {
                long diffDays = (triggerTimeEnd.getTime() - triggerTimeStart.getTime()) / (1000 * 60 * 60 * 24);
                if (diffDays > MAX_DAYS_RANGE) {
                    throw new RuntimeException(I18nUtil.getString("joblog_export_timerange_limit"));
                }
            }

            int totalCount = xxlJobLogMapper.pageListCount(0, 1, jobGroup, jobId, triggerTimeStart, triggerTimeEnd, logStatus);
            if (totalCount > MAX_EXPORT_COUNT) {
                throw new RuntimeException(I18nUtil.getString("joblog_export_too_much"));
            }

            XxlJobInfo jobInfo = xxlJobInfoMapper.loadById(jobId);
            XxlJobGroup jobGroupObj = xxlJobGroupMapper.load(jobGroup);

            String jobGroupName = jobGroupObj != null ? jobGroupObj.getTitle() : "unknown";
            String jobName = jobInfo != null ? jobInfo.getJobDesc() : "unknown";
            String datetime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String fileName = jobGroupName + "-" + jobName + "-" + datetime + ".xlsx";

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFileName);

            try (ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream(), XxlJobLogExcel.class).build()) {
                WriteSheet writeSheet = EasyExcel.writerSheet("调度日志").build();

                int offset = 0;
                while (offset < totalCount) {
                    List<XxlJobLog> logList = xxlJobLogMapper.pageList(offset, BATCH_SIZE, jobGroup, jobId, triggerTimeStart, triggerTimeEnd, logStatus);
                    if (logList == null || logList.isEmpty()) {
                        break;
                    }

                    List<XxlJobLogExcel> excelList = convertToExcelList(logList, jobGroupName, jobName);
                    excelWriter.write(excelList, writeSheet);

                    offset += BATCH_SIZE;
                }
            }

            logger.info("Export job log success, jobGroup={}, jobId={}, count={}", jobGroup, jobId, totalCount);
        } finally {
            unlock();
        }
    }

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

    private String formatTriggerCode(int code) {
        if (code == 200) {
            return I18nUtil.getString("system_success");
        } else if (code > 0) {
            return I18nUtil.getString("system_fail");
        } else {
            return "";
        }
    }

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
