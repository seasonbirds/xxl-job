package com.xxl.job.admin.core.aspect;

import com.xxl.job.admin.core.annotation.OperateLog;
import com.xxl.job.admin.mapper.XxlJobGroupMapper;
import com.xxl.job.admin.mapper.XxlJobInfoMapper;
import com.xxl.job.admin.mapper.XxlJobUserMapper;
import com.xxl.job.admin.model.XxlJobGroup;
import com.xxl.job.admin.model.XxlJobInfo;
import com.xxl.job.admin.model.XxlJobOperateLog;
import com.xxl.job.admin.model.XxlJobUser;
import com.xxl.job.admin.service.XxlJobOperateLogService;
import com.xxl.job.admin.util.HttpUtil;
import com.xxl.sso.core.helper.XxlSsoHelper;
import com.xxl.sso.core.model.LoginInfo;
import com.xxl.tool.core.StringTool;
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
 * 操作日志切面
 * 用于拦截带有@OperateLog注解的方法，记录操作日志
 * @author xxl-job
 */
@Aspect
@Component
public class OperateLogAspect {
    private static final Logger logger = LoggerFactory.getLogger(OperateLogAspect.class);

    @Resource
    private XxlJobOperateLogService xxlJobOperateLogService;

    @Resource
    private XxlJobUserMapper xxlJobUserMapper;

    @Resource
    private XxlJobGroupMapper xxlJobGroupMapper;

    @Resource
    private XxlJobInfoMapper xxlJobInfoMapper;

    @Around("@annotation(com.xxl.job.admin.core.annotation.OperateLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OperateLog operateLog = method.getAnnotation(OperateLog.class);

        if (operateLog == null) {
            return joinPoint.proceed();
        }

        String module = operateLog.module();
        boolean isLoginModule = XxlJobOperateLog.Module.LOGIN.getCode().equals(module);
        String logoutAction = XxlJobOperateLog.Action.LOGOUT.getCode();
        boolean isLogoutAction = logoutAction.equals(operateLog.action());

        String operatorBeforeLogout = null;
        if (isLoginModule && isLogoutAction) {
            operatorBeforeLogout = getCurrentOperator();
        }

        Object result = null;
        Throwable throwable = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            throwable = t;
            throw t;
        } finally {
            try {
                if (shouldSaveLog(operateLog, result, throwable)) {
                    saveOperateLog(joinPoint, result, throwable, operateLog, operatorBeforeLogout);
                }
            } catch (Exception e) {
                logger.error("OperateLogAspect save log error: {}", e.getMessage(), e);
            }
        }
    }

    private boolean shouldSaveLog(OperateLog operateLog, Object result, Throwable throwable) {
        if (throwable != null) {
            return false;
        }

        String module = operateLog.module();
        if (XxlJobOperateLog.Module.LOGIN.getCode().equals(module)) {
            if (result instanceof Response<?>) {
                return ((Response<?>) result).isSuccess();
            }
            return false;
        }

        return true;
    }

    private String getCurrentOperator() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        if (request != null) {
            try {
                LoginInfo loginInfo = XxlSsoHelper.loginCheckWithAttr(request).getData();
                if (loginInfo != null && StringTool.isNotBlank(loginInfo.getUserName())) {
                    return loginInfo.getUserName();
                }
            } catch (Exception e) {
                logger.debug("Get login user info error: {}", e.getMessage());
            }
        }
        return null;
    }

    private void saveOperateLog(ProceedingJoinPoint joinPoint, Object result, Throwable throwable,
                                 OperateLog operateLog, String operatorBeforeLogout) {
        XxlJobOperateLog log = new XxlJobOperateLog();
        log.setModule(operateLog.module());
        log.setAction(operateLog.action());
        log.setOperateTime(new Date());

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        if (request != null) {
            log.setIp(HttpUtil.getClientIp(request));
        }

        if (operatorBeforeLogout != null) {
            log.setOperator(operatorBeforeLogout);
        } else {
            String currentOperator = getCurrentOperator();
            if (currentOperator != null) {
                log.setOperator(currentOperator);
            }
        }

        try {
            fillTargetInfo(log, joinPoint, result);
        } catch (Exception e) {
            logger.error("Fill target info error: {}", e.getMessage(), e);
        }

        xxlJobOperateLogService.save(log);
    }

    private void fillTargetInfo(XxlJobOperateLog log, ProceedingJoinPoint joinPoint, Object result) {
        String module = log.getModule();
        Object[] args = joinPoint.getArgs();

        if (XxlJobOperateLog.Module.LOGIN.getCode().equals(module)) {
            if (args != null && args.length > 0 && log.getOperator() == null) {
                for (Object arg : args) {
                    if (arg instanceof String) {
                        String strArg = (String) arg;
                        if (StringTool.isNotBlank(strArg) && strArg.length() >= 4) {
                            log.setOperator(strArg);
                            break;
                        }
                    }
                }
            }
            return;
        }

        if (XxlJobOperateLog.Module.USER.getCode().equals(module)) {
            if (args != null && args.length > 0) {
                for (Object arg : args) {
                    if (arg instanceof XxlJobUser) {
                        XxlJobUser user = (XxlJobUser) arg;
                        if (user.getId() > 0) {
                            log.setTargetId(user.getId());
                            log.setTargetName(user.getUsername());
                        } else if (StringTool.isNotBlank(user.getUsername())) {
                            log.setTargetName(user.getUsername());
                        }
                        break;
                    }
                    if (arg instanceof List) {
                        List<?> list = (List<?>) arg;
                        if (!list.isEmpty() && list.get(0) instanceof Integer) {
                            Integer id = (Integer) list.get(0);
                            XxlJobUser user = xxlJobUserMapper.loadById(id);
                            if (user != null) {
                                log.setTargetId(id);
                                log.setTargetName(user.getUsername());
                            }
                        }
                        break;
                    }
                }
            }
            return;
        }

        if (XxlJobOperateLog.Module.JOB_GROUP.getCode().equals(module)) {
            if (args != null && args.length > 0) {
                for (Object arg : args) {
                    if (arg instanceof XxlJobGroup) {
                        XxlJobGroup group = (XxlJobGroup) arg;
                        if (group.getId() > 0) {
                            log.setTargetId(group.getId());
                            log.setTargetName(group.getTitle());
                        } else if (StringTool.isNotBlank(group.getTitle())) {
                            log.setTargetName(group.getTitle());
                        }
                        break;
                    }
                    if (arg instanceof List) {
                        List<?> list = (List<?>) arg;
                        if (!list.isEmpty() && list.get(0) instanceof Integer) {
                            Integer id = (Integer) list.get(0);
                            XxlJobGroup group = xxlJobGroupMapper.load(id);
                            if (group != null) {
                                log.setTargetId(id);
                                log.setTargetName(group.getTitle());
                            }
                        }
                        break;
                    }
                }
            }
            return;
        }

        if (XxlJobOperateLog.Module.JOB_INFO.getCode().equals(module)) {
            if (args != null && args.length > 0) {
                for (Object arg : args) {
                    if (arg instanceof XxlJobInfo) {
                        XxlJobInfo jobInfo = (XxlJobInfo) arg;
                        if (jobInfo.getId() > 0) {
                            log.setTargetId(jobInfo.getId());
                            if (StringTool.isNotBlank(jobInfo.getJobDesc())) {
                                log.setTargetName(jobInfo.getJobDesc());
                            }
                        } else if (StringTool.isNotBlank(jobInfo.getJobDesc())) {
                            log.setTargetName(jobInfo.getJobDesc());
                        }
                        break;
                    }
                    if (arg instanceof List) {
                        List<?> list = (List<?>) arg;
                        if (!list.isEmpty() && list.get(0) instanceof Integer) {
                            Integer id = (Integer) list.get(0);
                            XxlJobInfo jobInfo = xxlJobInfoMapper.loadById(id);
                            if (jobInfo != null) {
                                log.setTargetId(id);
                                if (StringTool.isNotBlank(jobInfo.getJobDesc())) {
                                    log.setTargetName(jobInfo.getJobDesc());
                                }
                            }
                        }
                        break;
                    }
                }
            }
            return;
        }
    }
}
