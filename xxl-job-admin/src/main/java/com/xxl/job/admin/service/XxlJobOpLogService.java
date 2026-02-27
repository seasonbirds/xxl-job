package com.xxl.job.admin.service;

import com.xxl.job.admin.model.XxlJobOpLog;
import com.xxl.tool.response.PageModel;
import com.xxl.tool.response.Response;

import java.util.Date;

/**
 * 操作日志服务接口
 * @author xxl-job
 */
public interface XxlJobOpLogService {

    /**
     * 分页查询操作日志
     *
     * @param offset      偏移量
     * @param pagesize    每页大小
     * @param module      操作模块
     * @param type        操作类型
     * @param operator    操作人
     * @param jobGroup    执行器ID
     * @param opTimeStart 操作时间开始
     * @param opTimeEnd   操作时间结束
     * @return 分页结果
     */
    Response<PageModel<XxlJobOpLog>> pageList(int offset, int pagesize,
                                               String module, String type, String operator, Integer jobGroup,
                                               Date opTimeStart, Date opTimeEnd);

    /**
     * 异步记录操作日志
     * 采用异步方式，最大程度减少对业务代码的影响
     *
     * @param module     操作模块
     * @param type       操作类型
     * @param operator   操作人
     * @param opIp       操作IP
     * @param targetId   被操作对象ID
     * @param targetName 被操作对象名称
     * @param content    操作内容
     */
    void asyncLog(String module, String type, String operator, String opIp,
                  String targetId, String targetName, String content);

    /**
     * 异步记录操作日志（带执行器ID）
     * 采用异步方式，最大程度减少对业务代码的影响
     *
     * @param module     操作模块
     * @param type       操作类型
     * @param operator   操作人
     * @param opIp       操作IP
     * @param targetId   被操作对象ID
     * @param targetName 被操作对象名称
     * @param content    操作内容
     * @param jobGroup   执行器ID
     */
    void asyncLog(String module, String type, String operator, String opIp,
                  String targetId, String targetName, String content, Integer jobGroup);

    /**
     * 同步记录操作日志
     *
     * @param xxlJobOpLog 操作日志对象
     * @return 记录结果
     */
    long save(XxlJobOpLog xxlJobOpLog);

}
