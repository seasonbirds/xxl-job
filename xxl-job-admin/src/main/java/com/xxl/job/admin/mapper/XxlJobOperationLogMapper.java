package com.xxl.job.admin.mapper;

import com.xxl.job.admin.model.XxlJobOperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface XxlJobOperationLogMapper {

    List<XxlJobOperationLog> pageList(@Param("offset") int offset,
                                       @Param("pagesize") int pagesize,
                                       @Param("operationModule") String operationModule,
                                       @Param("operationType") String operationType,
                                       @Param("operator") String operator,
                                       @Param("startTime") Date startTime,
                                       @Param("endTime") Date endTime,
                                       @Param("jobGroup") Integer jobGroup);

    int pageListCount(@Param("offset") int offset,
                      @Param("pagesize") int pagesize,
                      @Param("operationModule") String operationModule,
                      @Param("operationType") String operationType,
                      @Param("operator") String operator,
                      @Param("startTime") Date startTime,
                      @Param("endTime") Date endTime,
                      @Param("jobGroup") Integer jobGroup);

    int save(XxlJobOperationLog xxlJobOperationLog);

}
