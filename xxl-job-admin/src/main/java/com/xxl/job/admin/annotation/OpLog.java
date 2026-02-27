package com.xxl.job.admin.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 用于标记需要记录操作日志的方法
 *
 * @author xxl-job
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OpLog {

    /**
     * 操作模块
     */
    String module();

    /**
     * 操作类型
     */
    String type();

    /**
     * 获取目标对象ID的SpEL表达式
     * 例如：#xxlJobUser.id、#ids[0]
     */
    String targetId() default "";

    /**
     * 获取目标对象名称的SpEL表达式
     * 例如：#xxlJobUser.username、#jobInfo.jobDesc
     */
    String targetName() default "";

    /**
     * 操作内容描述，支持SpEL表达式
     */
    String content() default "";

    /**
     * 获取执行器ID的SpEL表达式（仅任务管理模块使用）
     * 例如：#jobInfo.jobGroup
     */
    String jobGroup() default "";

}
