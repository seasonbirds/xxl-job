package com.xxl.job.admin.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解，用于标记需要记录日志的方法
 * @author system 2026-03-29
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 操作模块
     */
    String module() default "";

    /**
     * 操作类型
     */
    String type() default "";

    /**
     * 操作描述
     */
    String description() default "";
}
