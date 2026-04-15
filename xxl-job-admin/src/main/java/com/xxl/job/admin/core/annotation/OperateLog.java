package com.xxl.job.admin.core.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 用于标记需要记录操作日志的方法
 * @author xxl-job
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperateLog {

    /**
     * 模块名称
     */
    String module();

    /**
     * 操作类型
     */
    String action();

    /**
     * 描述
     */
    String description() default "";

    /**
     * 是否记录返回值
     */
    boolean logResult() default false;
}
