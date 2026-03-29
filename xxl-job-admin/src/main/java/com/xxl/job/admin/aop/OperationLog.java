package com.xxl.job.admin.aop;

import com.xxl.job.admin.model.enums.OperationModule;
import com.xxl.job.admin.model.enums.OperationType;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    OperationModule module();

    OperationType type();

}
