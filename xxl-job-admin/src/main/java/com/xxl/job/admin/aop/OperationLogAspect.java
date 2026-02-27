package com.xxl.job.admin.aop;

import com.xxl.job.admin.model.XxlJobGroup;
import com.xxl.job.admin.model.XxlJobInfo;
import com.xxl.job.admin.model.XxlJobOperationLog;
import com.xxl.job.admin.model.XxlJobUser;
import com.xxl.job.admin.service.OperationLogService;
import com.xxl.job.admin.util.HttpServletUtil;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.sso.core.helper.XxlSsoHelper;
import com.xxl.sso.core.model.LoginInfo;
import com.xxl.tool.response.Response;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;
import java.util.List;

/**
 * operation log aspect
 * intercept methods with @OperationLog annotation and record operation logs
 *
 * @author xxl-job
 */
@Aspect
@Component
public class OperationLogAspect {

    private static Logger logger = LoggerFactory.getLogger(OperationLogAspect.class);

    @Resource
    private OperationLogService operationLogService;

    @Pointcut("@annotation(com.xxl.job.admin.aop.OperationLog)")
    public void operationLogPointcut() {
    }

    @Around("operationLogPointcut() && @annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        Object result = joinPoint.proceed();

        try {
            saveOperationLog(joinPoint, operationLog, result);
        } catch (Exception e) {
            logger.error("save operation log error", e);
        }

        return result;
    }

    private void saveOperationLog(ProceedingJoinPoint joinPoint, OperationLog operationLog, Object result) {
        if (!isSuccess(result)) {
            return;
        }

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String operator = getOperator(request);
        String ip = HttpServletUtil.getClientIp(request);
        String targetId = null;
        String targetName = null;
        Integer jobGroup = null;
        String extraInfo = null;

        Object[] args = joinPoint.getArgs();
        OperationModuleEnum module = operationLog.module();
        OperationTypeEnum type = operationLog.type();

        switch (module) {
            case LOGIN:
                if (type == OperationTypeEnum.LOGIN) {
                    operator = getLoginUsername(args);
                    targetId = operator;
                    targetName = operator;
                }
                break;
            case USER:
                for (Object arg : args) {
                    if (arg instanceof XxlJobUser) {
                        XxlJobUser user = (XxlJobUser) arg;
                        if (type == OperationTypeEnum.DELETE) {
                            targetId = String.valueOf(user.getId());
                            XxlJobUser existUser = operationLogService.getUserById(user.getId());
                            targetName = existUser != null ? existUser.getUsername() : null;
                        } else {
                            targetId = String.valueOf(user.getId());
                            targetName = user.getUsername();
                        }
                        break;
                    } else if (arg instanceof List && type == OperationTypeEnum.DELETE) {
                        List<?> ids = (List<?>) arg;
                        if (!ids.isEmpty() && ids.get(0) instanceof Integer) {
                            Integer id = (Integer) ids.get(0);
                            targetId = String.valueOf(id);
                            XxlJobUser existUser = operationLogService.getUserById(id);
                            targetName = existUser != null ? existUser.getUsername() : null;
                        }
                    }
                }
                break;
            case GROUP:
                for (Object arg : args) {
                    if (arg instanceof XxlJobGroup) {
                        XxlJobGroup group = (XxlJobGroup) arg;
                        if (type == OperationTypeEnum.DELETE) {
                            targetId = String.valueOf(group.getId());
                            XxlJobGroup existGroup = operationLogService.getJobGroupById(group.getId());
                            targetName = existGroup != null ? existGroup.getTitle() : null;
                        } else {
                            targetId = String.valueOf(group.getId());
                            targetName = group.getTitle();
                        }
                        break;
                    } else if (arg instanceof List && type == OperationTypeEnum.DELETE) {
                        List<?> ids = (List<?>) arg;
                        if (!ids.isEmpty() && ids.get(0) instanceof Integer) {
                            Integer id = (Integer) ids.get(0);
                            targetId = String.valueOf(id);
                            XxlJobGroup existGroup = operationLogService.getJobGroupById(id);
                            targetName = existGroup != null ? existGroup.getTitle() : null;
                        }
                    }
                }
                break;
            case JOB:
                Integer jobId = null;
                for (Object arg : args) {
                    if (arg instanceof XxlJobInfo) {
                        XxlJobInfo jobInfo = (XxlJobInfo) arg;
                        jobId = jobInfo.getId();
                        targetId = String.valueOf(jobId);
                        targetName = jobInfo.getJobDesc();
                        jobGroup = jobInfo.getJobGroup();
                        break;
                    } else if (arg instanceof List) {
                        List<?> ids = (List<?>) arg;
                        if (!ids.isEmpty() && ids.get(0) instanceof Integer) {
                            jobId = (Integer) ids.get(0);
                            targetId = String.valueOf(jobId);
                        }
                    }
                }
                if (jobId != null && (type == OperationTypeEnum.DELETE || type == OperationTypeEnum.START || type == OperationTypeEnum.STOP)) {
                    XxlJobInfo jobInfo = operationLogService.getJobInfoById(jobId);
                    if (jobInfo != null) {
                        targetName = jobInfo.getJobDesc();
                        jobGroup = jobInfo.getJobGroup();
                    }
                }
                break;
        }

        XxlJobOperationLog log = new XxlJobOperationLog();
        log.setModule(module.name());
        log.setOperationType(type.name());
        log.setOperator(operator);
        log.setOperationTime(new Date());
        log.setIp(ip);
        log.setTargetId(targetId);
        log.setTargetName(targetName);
        log.setJobGroup(jobGroup);
        log.setExtraInfo(extraInfo);

        operationLogService.saveLogAsync(log);
    }

    private boolean isSuccess(Object result) {
        if (result instanceof Response) {
            return ((Response<?>) result).isSuccess();
        }
        if (result instanceof ReturnT) {
            return ReturnT.SUCCESS_CODE == ((ReturnT<?>) result).getCode();
        }
        return true;
    }

    private String getOperator(HttpServletRequest request) {
        try {
            Response<LoginInfo> loginInfoResponse = XxlSsoHelper.loginCheckWithAttr(request);
            if (loginInfoResponse.isSuccess() && loginInfoResponse.getData() != null) {
                return loginInfoResponse.getData().getUserName();
            }
        } catch (Exception e) {
            logger.warn("get operator from session error", e);
        }
        return "system";
    }

    private String getLoginUsername(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof String) {
                return (String) arg;
            }
        }
        return null;
    }

}
