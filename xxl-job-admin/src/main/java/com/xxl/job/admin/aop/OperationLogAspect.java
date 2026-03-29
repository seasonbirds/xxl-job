package com.xxl.job.admin.aop;

import com.xxl.job.admin.model.XxlJobGroup;
import com.xxl.job.admin.model.XxlJobInfo;
import com.xxl.job.admin.model.XxlJobOperationLog;
import com.xxl.job.admin.model.XxlJobUser;
import com.xxl.job.admin.model.enums.OperationModule;
import com.xxl.job.admin.model.enums.OperationType;
import com.xxl.job.admin.service.XxlJobOperationLogService;
import com.xxl.sso.core.helper.XxlSsoHelper;
import com.xxl.sso.core.model.LoginInfo;
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

@Aspect
@Component
public class OperationLogAspect {
    private static Logger logger = LoggerFactory.getLogger(OperationLogAspect.class);

    @Resource
    private XxlJobOperationLogService xxlJobOperationLogService;

    @Pointcut("@annotation(com.xxl.job.admin.aop.OperationLog)")
    public void logPointCut() {
    }

    @Around("logPointCut() && @annotation(operationLogAnnotation)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLogAnnotation) throws Throwable {
        long beginTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long time = System.currentTimeMillis() - beginTime;

        try {
            saveOperationLog(joinPoint, operationLogAnnotation, time);
        } catch (Exception e) {
            logger.error("Save operation log error", e);
        }

        return result;
    }

    private void saveOperationLog(ProceedingJoinPoint joinPoint, OperationLog operationLogAnnotation, long time) {
        OperationModule module = operationLogAnnotation.module();
        OperationType type = operationLogAnnotation.type();

        XxlJobOperationLog operationLog = new XxlJobOperationLog();
        operationLog.setOperationModule(module.getCode());
        operationLog.setOperationType(type.getCode());
        operationLog.setOperationTime(new Date());

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String ip = getIpAddr(request);
        operationLog.setLoginIp(ip);

        if (module == OperationModule.LOGIN) {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                for (Object arg : args) {
                    if (arg instanceof String && "userName".equals(arg)) {
                        operationLog.setOperator((String) arg);
                        break;
                    }
                }
            }
        } else {
            try {
                LoginInfo loginInfo = XxlSsoHelper.loginCheckWithAttr(request).getData();
                operationLog.setOperator(loginInfo.getUserName());
            } catch (Exception e) {
                logger.warn("Get login user info error", e);
            }
        }

        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            for (Object arg : args) {
                if (arg instanceof XxlJobUser) {
                    XxlJobUser user = (XxlJobUser) arg;
                    operationLog.setTargetAccount(user.getUsername());
                } else if (arg instanceof XxlJobGroup) {
                    XxlJobGroup group = (XxlJobGroup) arg;
                    operationLog.setExecutorName(group.getTitle());
                } else if (arg instanceof XxlJobInfo) {
                    XxlJobInfo jobInfo = (XxlJobInfo) arg;
                    operationLog.setJobId(jobInfo.getId());
                    operationLog.setJobGroup(jobInfo.getJobGroup());
                    operationLog.setJobDesc(jobInfo.getJobDesc());
                }
            }
        }

        xxlJobOperationLogService.add(operationLog);
    }

    private String getIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
