package com.xxl.job.admin.aop;

import com.xxl.job.admin.annotation.OperationLog;
import com.xxl.job.admin.constant.OperationModule;
import com.xxl.job.admin.constant.OperationType;
import com.xxl.job.admin.model.XxlJobGroup;
import com.xxl.job.admin.model.XxlJobInfo;
import com.xxl.job.admin.model.XxlJobOperationLog;
import com.xxl.job.admin.model.XxlJobUser;
import com.xxl.job.admin.service.XxlJobOperationLogService;
import com.xxl.job.admin.util.WebUtil;
import com.xxl.sso.core.helper.XxlSsoHelper;
import com.xxl.sso.core.model.LoginInfo;
import com.xxl.tool.response.Response;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;

/**
 * operation log aspect
 * 
 * <p>
 *     This aspect is used to record operation logs automatically.
 *     It intercepts methods annotated with @OperationLog and records the operation details.
 *     The log recording is designed to be non-intrusive and will not affect the business logic.
 * </p>
 *
 * @author xxl-job 2024
 */
@Aspect
@Component
public class OperationLogAspect {

    private static Logger logger = LoggerFactory.getLogger(OperationLogAspect.class);

    @Resource
    private XxlJobOperationLogService xxlJobOperationLogService;

    /**
     * around advice for @OperationLog annotation
     *
     * @param joinPoint proceeding join point
     * @return method result
     * @throws Throwable exception
     */
    @Around("@annotation(com.xxl.job.admin.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = null;
        Exception exception = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            try {
                recordOperationLog(joinPoint, result, exception);
            } catch (Exception e) {
                logger.error(">>>>>>>>>>> record operation log error: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * record operation log
     *
     * @param joinPoint proceeding join point
     * @param result    method result
     * @param exception exception if any
     */
    private void recordOperationLog(ProceedingJoinPoint joinPoint, Object result, Exception exception) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OperationLog operationLogAnnotation = method.getAnnotation(OperationLog.class);

        if (operationLogAnnotation == null) {
            return;
        }

        String module = operationLogAnnotation.module();
        String operationType = operationLogAnnotation.operationType();

        HttpServletRequest request = getRequest();
        if (request == null) {
            return;
        }

        String ip = WebUtil.getClientIp(request);

        XxlJobOperationLog log = new XxlJobOperationLog();
        log.setModule(module);
        log.setOperationType(operationType);
        log.setOperateTime(new Date());
        log.setIp(ip);

        Object[] args = joinPoint.getArgs();

        if (OperationModule.LOGIN.equals(module)) {
            fillLoginLog(log, args, result);
        } else {
            Response<LoginInfo> loginInfoResponse = XxlSsoHelper.loginCheckWithAttr(request);
            if (!loginInfoResponse.isSuccess()) {
                return;
            }
            LoginInfo loginInfo = loginInfoResponse.getData();
            log.setOperator(loginInfo.getUserName());
            fillLogDetail(log, module, operationType, args, result);
        }

        xxlJobOperationLogService.addLog(log);
    }

    /**
     * fill log detail based on module and operation type
     *
     * @param log           operation log
     * @param module        module
     * @param operationType operation type
     * @param args          method arguments
     * @param result        method result
     */
    private void fillLogDetail(XxlJobOperationLog log, String module, String operationType, Object[] args, Object result) {
        try {
            if (OperationModule.USER.equals(module)) {
                fillUserLog(log, operationType, args);
            } else if (OperationModule.JOBGROUP.equals(module)) {
                fillJobGroupLog(log, operationType, args);
            } else if (OperationModule.JOBINFO.equals(module)) {
                fillJobInfoLog(log, operationType, args);
            }
        } catch (Exception e) {
            logger.error(">>>>>>>>>>> fill log detail error: {}", e.getMessage(), e);
        }
    }

    /**
     * fill login log details
     *
     * @param log    operation log
     * @param args   method arguments
     * @param result method result
     */
    private void fillLoginLog(XxlJobOperationLog log, Object[] args, Object result) {
        String userName = null;
        if (args != null && args.length > 0) {
            for (Object arg : args) {
                if (arg instanceof String) {
                    String str = (String) arg;
                    if (str != null && str.length() >= 4 && str.length() <= 20) {
                        userName = str;
                        break;
                    }
                }
            }
        }

        if (userName != null) {
            log.setOperator(userName);
            log.setTargetName(userName);
        }

        if (result instanceof Response) {
            Response<?> response = (Response<?>) result;
            if (!response.isSuccess()) {
                return;
            }
        }
    }

    /**
     * fill user management log details
     *
     * @param log           operation log
     * @param operationType operation type
     * @param args          method arguments
     */
    private void fillUserLog(XxlJobOperationLog log, String operationType, Object[] args) {
        if (args == null || args.length == 0) {
            return;
        }

        for (Object arg : args) {
            if (arg instanceof XxlJobUser) {
                XxlJobUser user = (XxlJobUser) arg;
                log.setTargetId(user.getId());
                log.setTargetName(user.getUsername());
                break;
            }
        }

        if (OperationType.DELETE.equals(operationType) && args.length > 0) {
            if (args[0] instanceof List) {
                List<?> ids = (List<?>) args[0];
                if (!ids.isEmpty()) {
                    Integer userId = (Integer) ids.get(0);
                    XxlJobUser user = xxlJobOperationLogService.loadUserById(userId);
                    if (user != null) {
                        log.setTargetId(userId);
                        log.setTargetName(user.getUsername());
                    }
                }
            }
        }
    }

    /**
     * fill job group log details
     *
     * @param log           operation log
     * @param operationType operation type
     * @param args          method arguments
     */
    private void fillJobGroupLog(XxlJobOperationLog log, String operationType, Object[] args) {
        if (args == null || args.length == 0) {
            return;
        }

        for (Object arg : args) {
            if (arg instanceof XxlJobGroup) {
                XxlJobGroup group = (XxlJobGroup) arg;
                log.setTargetId(group.getId());
                log.setTargetName(group.getTitle());
                break;
            }
        }

        if (OperationType.DELETE.equals(operationType) && args.length > 0) {
            if (args[0] instanceof List) {
                List<?> ids = (List<?>) args[0];
                if (!ids.isEmpty()) {
                    Integer groupId = (Integer) ids.get(0);
                    XxlJobGroup group = xxlJobOperationLogService.loadJobGroupById(groupId);
                    if (group != null) {
                        log.setTargetId(groupId);
                        log.setTargetName(group.getTitle());
                    }
                }
            }
        }
    }

    /**
     * fill job info log details
     *
     * @param log           operation log
     * @param operationType operation type
     * @param args          method arguments
     */
    private void fillJobInfoLog(XxlJobOperationLog log, String operationType, Object[] args) {
        if (args == null || args.length == 0) {
            return;
        }

        for (Object arg : args) {
            if (arg instanceof XxlJobInfo) {
                XxlJobInfo jobInfo = (XxlJobInfo) arg;
                log.setTargetId(jobInfo.getId());
                log.setTargetName(jobInfo.getJobDesc());
                log.setJobGroupId(jobInfo.getJobGroup());
                if (jobInfo.getJobGroup() > 0) {
                    XxlJobGroup group = xxlJobOperationLogService.loadJobGroupById(jobInfo.getJobGroup());
                    if (group != null) {
                        log.setJobGroupName(group.getTitle());
                    }
                }
                break;
            }
        }

        if ((OperationType.DELETE.equals(operationType) || OperationType.START.equals(operationType) || OperationType.STOP.equals(operationType)) && args.length > 0) {
            if (args[0] instanceof List) {
                List<?> ids = (List<?>) args[0];
                if (!ids.isEmpty()) {
                    Integer jobId = (Integer) ids.get(0);
                    XxlJobInfo jobInfo = xxlJobOperationLogService.loadJobInfoById(jobId);
                    if (jobInfo != null) {
                        log.setTargetId(jobId);
                        log.setTargetName(jobInfo.getJobDesc());
                        log.setJobGroupId(jobInfo.getJobGroup());
                        if (jobInfo.getJobGroup() > 0) {
                            XxlJobGroup group = xxlJobOperationLogService.loadJobGroupById(jobInfo.getJobGroup());
                            if (group != null) {
                                log.setJobGroupName(group.getTitle());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * get current http servlet request
     *
     * @return HttpServletRequest
     */
    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}
