package com.xxl.job.admin.mapper;

import com.xxl.job.admin.model.XxlJobOperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * operation log mapper
 * 
 * <p>
 *     This mapper provides database operations for operation logs.
 *     It supports pagination query, save, and clear operations.
 * </p>
 *
 * @author xxl-job 2024
 */
@Mapper
public interface XxlJobOperationLogMapper {

    /**
     * page list query
     *
     * @param offset          offset
     * @param pagesize        pagesize
     * @param module          module
     * @param operationType   operation type
     * @param operator        operator
     * @param operateTimeStart operate time start
     * @param operateTimeEnd   operate time end
     * @param jobGroupId      job group id
     * @param targetName      target name
     * @return operation log list
     */
    List<XxlJobOperationLog> pageList(@Param("offset") int offset,
                                      @Param("pagesize") int pagesize,
                                      @Param("module") String module,
                                      @Param("operationType") String operationType,
                                      @Param("operator") String operator,
                                      @Param("operateTimeStart") Date operateTimeStart,
                                      @Param("operateTimeEnd") Date operateTimeEnd,
                                      @Param("jobGroupId") Integer jobGroupId,
                                      @Param("targetName") String targetName);

    /**
     * page list count
     *
     * @param offset          offset
     * @param pagesize        pagesize
     * @param module          module
     * @param operationType   operation type
     * @param operator        operator
     * @param operateTimeStart operate time start
     * @param operateTimeEnd   operate time end
     * @param jobGroupId      job group id
     * @param targetName      target name
     * @return total count
     */
    int pageListCount(@Param("offset") int offset,
                      @Param("pagesize") int pagesize,
                      @Param("module") String module,
                      @Param("operationType") String operationType,
                      @Param("operator") String operator,
                      @Param("operateTimeStart") Date operateTimeStart,
                      @Param("operateTimeEnd") Date operateTimeEnd,
                      @Param("jobGroupId") Integer jobGroupId,
                      @Param("targetName") String targetName);

    /**
     * save operation log
     *
     * @param xxlJobOperationLog operation log
     * @return affected rows
     */
    int save(XxlJobOperationLog xxlJobOperationLog);

    /**
     * clear log before specified time
     *
     * @param clearBeforeTime clear before time
     * @return affected rows
     */
    int clearLog(@Param("clearBeforeTime") Date clearBeforeTime);
}
