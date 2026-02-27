package com.xxl.job.admin.mapper;

import com.xxl.job.admin.model.XxlJobOperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * operation log mapper
 *
 * @author xxl-job
 */
@Mapper
public interface XxlJobOperationLogMapper {

    public List<XxlJobOperationLog> pageList(@Param("offset") int offset,
                                             @Param("pagesize") int pagesize,
                                             @Param("module") String module,
                                             @Param("operationType") String operationType,
                                             @Param("operator") String operator,
                                             @Param("targetName") String targetName,
                                             @Param("jobGroup") Integer jobGroup,
                                             @Param("startTime") Date startTime,
                                             @Param("endTime") Date endTime);

    public int pageListCount(@Param("module") String module,
                             @Param("operationType") String operationType,
                             @Param("operator") String operator,
                             @Param("targetName") String targetName,
                             @Param("jobGroup") Integer jobGroup,
                             @Param("startTime") Date startTime,
                             @Param("endTime") Date endTime);

    public int save(XxlJobOperationLog operationLog);

}
