package com.xxl.job.admin.mapper;

import com.xxl.job.admin.model.XxlJobOpLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 操作日志 Mapper
 * @author xxl-job
 */
@Mapper
public interface XxlJobOpLogMapper {

    /**
     * 分页查询操作日志
     */
    List<XxlJobOpLog> pageList(@Param("offset") int offset,
                               @Param("pagesize") int pagesize,
                               @Param("module") String module,
                               @Param("type") String type,
                               @Param("operator") String operator,
                               @Param("jobGroup") Integer jobGroup,
                               @Param("opTimeStart") Date opTimeStart,
                               @Param("opTimeEnd") Date opTimeEnd);

    /**
     * 分页查询操作日志总数
     */
    int pageListCount(@Param("offset") int offset,
                      @Param("pagesize") int pagesize,
                      @Param("module") String module,
                      @Param("type") String type,
                      @Param("operator") String operator,
                      @Param("jobGroup") Integer jobGroup,
                      @Param("opTimeStart") Date opTimeStart,
                      @Param("opTimeEnd") Date opTimeEnd);

    /**
     * 保存操作日志
     */
    long save(XxlJobOpLog xxlJobOpLog);

    /**
     * 根据ID查询
     */
    XxlJobOpLog load(@Param("id") long id);

}
