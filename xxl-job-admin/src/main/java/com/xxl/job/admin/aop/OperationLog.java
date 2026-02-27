package com.xxl.job.admin.aop;

import java.lang.annotation.*;

/**
 * operation log annotation
 * used to mark methods that need to record operation logs
 *
 * @author xxl-job
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    OperationModuleEnum module();

    OperationTypeEnum type();

}
