package com.xxl.job.admin.mapper;

import com.xxl.job.admin.model.XxlJobOperateLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 操作日志Mapper
 * @author xxl-job
 */
@Mapper
public interface XxlJobOperateLogMapper {

    int save(XxlJobOperateLog xxlJobOperateLog);

    List<XxlJobOperateLog> pageList(@Param("offset") int offset,
                                      @Param("pagesize") int pagesize,
                                      @Param("module") String module,
                                      @Param("action") String action,
                                      @Param("operator") String operator,
                                      @Param("targetName") String targetName,
                                      @Param("startTime") Date startTime,
                                      @Param("endTime") Date endTime,
                                      @Param("targetId") Integer targetId);

    int pageListCount(@Param("offset") int offset,
                       @Param("pagesize") int pagesize,
                       @Param("module") String module,
                       @Param("action") String action,
                       @Param("operator") String operator,
                       @Param("targetName") String targetName,
                       @Param("startTime") Date startTime,
                       @Param("endTime") Date endTime,
                       @Param("targetId") Integer targetId);

    XxlJobOperateLog loadById(@Param("id") int id);

    int delete(@Param("id") int id);

    int deleteByTime(@Param("deleteBeforeTime") Date deleteBeforeTime);
}
