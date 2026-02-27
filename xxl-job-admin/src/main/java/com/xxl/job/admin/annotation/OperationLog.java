package com.xxl.job.admin.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * operation log annotation
 * 
 * <p>
 *     This annotation is used to mark methods that need to be logged.
 *     When a method is annotated with @OperationLog, the OperationLogAspect
 *     will automatically record the operation details to the database.
 * </p>
 * 
 * <p>
 *     Usage example:
 *     <pre>
 *     &#64;OperationLog(module = OperationModule.USER, operationType = OperationType.ADD)
 *     public Response&lt;String&gt; add(XxlJobUser xxlJobUser) {
 *         // method implementation
 *     }
 *     </pre>
 * </p>
 *
 * @author xxl-job 2024
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /**
     * operation module, use constants from OperationModule class
     *
     * @return module name
     */
    String module();

    /**
     * operation type, use constants from OperationType class
     *
     * @return operation type
     */
    String operationType();
}
