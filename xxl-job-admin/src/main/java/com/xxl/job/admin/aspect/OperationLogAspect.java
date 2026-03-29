package com.xxl.job.admin.aspect;

import com.xxl.job.admin.annotation.OperationLog;
import com.xxl.job.admin.model.XxlJobOperationLog;
import com.xxl.job.admin.service.OperationLogService;
import com.xxl.sso.core.helper.XxlSsoHelper;
import com.xxl.sso.core.model.LoginInfo;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Date;

/**
 * operation log aspect
 *
 * @author xxl job
 */
@Aspect
@Component
public class OperationLogAspect {
    private static Logger logger = LoggerFactory.getLogger(OperationLogAspect.class);

    @Resource
    private OperationLogService operationLogService;

    private ParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

    @Around("@annotation(com.xxl.job.admin.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long beginTime = System.currentTimeMillis();
        Object result = null;
        Exception exception = null;

        try {
            // 执行目标方法
            result = point.proceed();
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            try {
                // 记录操作日志
                recordOperationLog(point, result, exception);
            } catch (Exception e) {
                logger.error("record operation log error", e);
            }
        }

        return result;
    }

    private void recordOperationLog(ProceedingJoinPoint point, Object result, Exception exception) {
        // 获取方法签名
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();

        // 获取注解信息
        OperationLog operationLog = method.getAnnotation(OperationLog.class);
        if (operationLog == null) {
            return;
        }

        // 创建操作日志对象
        XxlJobOperationLog log = new XxlJobOperationLog();

        // 设置基本信息
        log.setOperationModule(operationLog.module());
        log.setOperationType(operationLog.type());
        log.setOperationTime(new Date());

        // 获取当前登录用户
        LoginInfo loginInfo = XxlSsoHelper.getLoginInfo();
        if (loginInfo != null) {
            log.setOperator(loginInfo.getUsername());
        }

        // 获取客户端IP
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        if (request != null) {
            String ip = getClientIp(request);
            log.setOperationIp(ip);
        }

        // 提取目标对象信息
        extractTargetInfo(point, method, log);

        // 保存日志
        operationLogService.save(log);
    }

    private void extractTargetInfo(ProceedingJoinPoint point, Method method, XxlJobOperationLog log) {
        Object[] args = point.getArgs();
        String[] paramNames = parameterNameDiscoverer.getParameterNames(method);

        if (args == null || paramNames == null || args.length != paramNames.length) {
            return;
        }

        String module = log.getOperationModule();

        // 根据不同模块提取目标信息
        switch (module) {
            case "USER":
                extractUserInfo(args, paramNames, log);
                break;
            case "EXECUTOR":
                extractExecutorInfo(args, paramNames, log);
                break;
            case "JOB":
                extractJobInfo(args, paramNames, log);
                break;
            case "LOGIN":
                // 登录操作不需要额外提取目标信息
                break;
        }
    }

    private void extractUserInfo(Object[] args, String[] paramNames, XxlJobOperationLog log) {
        for (int i = 0; i < args.length; i++) {
            if ("id".equals(paramNames[i]) && args[i] instanceof Integer) {
                log.setTargetId((Integer) args[i]);
            } else if ("username".equals(paramNames[i]) && args[i] instanceof String) {
                log.setTargetName((String) args[i]);
            }
        }
    }

    private void extractExecutorInfo(Object[] args, String[] paramNames, XxlJobOperationLog log) {
        for (int i = 0; i < args.length; i++) {
            if ("id".equals(paramNames[i]) && args[i] instanceof Integer) {
                log.setTargetId((Integer) args[i]);
            } else if ("title".equals(paramNames[i]) && args[i] instanceof String) {
                log.setTargetName((String) args[i]);
            }
        }
    }

    private void extractJobInfo(Object[] args, String[] paramNames, XxlJobOperationLog log) {
        for (int i = 0; i < args.length; i++) {
            if ("id".equals(paramNames[i]) && args[i] instanceof Integer) {
                log.setTargetId((Integer) args[i]);
            } else if ("jobDesc".equals(paramNames[i]) && args[i] instanceof String) {
                log.setTargetName((String) args[i]);
            }
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
