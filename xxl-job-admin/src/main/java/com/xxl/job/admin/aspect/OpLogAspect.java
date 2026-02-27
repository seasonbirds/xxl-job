package com.xxl.job.admin.aspect;

import com.xxl.job.admin.annotation.OpLog;
import com.xxl.job.admin.service.XxlJobOpLogService;
import com.xxl.job.admin.util.IpUtil;
import com.xxl.sso.core.helper.XxlSsoHelper;
import com.xxl.sso.core.model.LoginInfo;
import com.xxl.tool.response.Response;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 操作日志切面
 * 拦截带有@OpLog注解的方法，记录操作日志
 *
 * @author xxl-job
 */
@Aspect
@Component
public class OpLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(OpLogAspect.class);

    @Autowired
    private XxlJobOpLogService opLogService;

    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * 定义切点：带有@OpLog注解的方法
     */
    @Pointcut("@annotation(com.xxl.job.admin.annotation.OpLog)")
    public void opLogPointCut() {
    }

    /**
     * 方法执行成功后记录日志
     */
    @AfterReturning(value = "opLogPointCut()", returning = "result")
    public void doAfterReturning(JoinPoint joinPoint, Object result) {
        try {
            // 获取注解
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            OpLog opLog = method.getAnnotation(OpLog.class);

            if (opLog == null) {
                return;
            }

            // 判断业务操作是否成功（如果返回结果是Response类型）
            if (result instanceof Response<?> responseResult) {
                if (!responseResult.isSuccess()) {
                    // 业务操作失败，不记录日志
                    return;
                }
            }

            // 获取请求信息
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }
            HttpServletRequest request = attributes.getRequest();

            // 获取当前登录用户
            String operator = getCurrentUser(request);

            // 获取IP地址
            String ip = IpUtil.getClientIp(request);

            // 解析SpEL表达式
            EvaluationContext context = createEvaluationContext(joinPoint);

            // 解析目标ID
            String targetId = "";
            if (!opLog.targetId().isEmpty()) {
                Object targetIdObj = parser.parseExpression(opLog.targetId()).getValue(context);
                targetId = targetIdObj != null ? targetIdObj.toString() : "";
            }

            // 解析目标名称
            String targetName = "";
            if (!opLog.targetName().isEmpty()) {
                Object targetNameObj = parser.parseExpression(opLog.targetName()).getValue(context);
                targetName = targetNameObj != null ? targetNameObj.toString() : "";
            }

            // 解析操作内容
            String content = "";
            if (!opLog.content().isEmpty()) {
                Object contentObj = parser.parseExpression(opLog.content()).getValue(context);
                content = contentObj != null ? contentObj.toString() : "";
            } else {
                // 默认内容
                content = getTypeName(opLog.type()) + "操作";
                if (!targetName.isEmpty()) {
                    content += "：" + targetName;
                }
            }

            // 解析执行器ID（仅任务管理模块使用）
            Integer jobGroup = null;
            if (!opLog.jobGroup().isEmpty()) {
                Object jobGroupObj = parser.parseExpression(opLog.jobGroup()).getValue(context);
                if (jobGroupObj != null) {
                    jobGroup = Integer.valueOf(jobGroupObj.toString());
                }
            }

            // 异步记录日志
            opLogService.asyncLog(opLog.module(), opLog.type(), operator, ip, targetId, targetName, content, jobGroup);

        } catch (Exception e) {
            // 日志记录失败不影响主业务
            logger.error("记录操作日志失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 创建SpEL表达式上下文
     */
    private EvaluationContext createEvaluationContext(JoinPoint joinPoint) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        // 获取方法参数
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = nameDiscoverer.getParameterNames(signature.getMethod());

        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        // 将参数也按索引设置，方便通过 #args[0] 访问
        context.setVariable("args", args);

        return context;
    }

    /**
     * 获取当前登录用户
     */
    private String getCurrentUser(HttpServletRequest request) {
        try {
            Response<LoginInfo> loginInfoResponse = XxlSsoHelper.loginCheckWithAttr(request);
            if (loginInfoResponse.isSuccess() && loginInfoResponse.getData() != null) {
                return loginInfoResponse.getData().getUserName();
            }
        } catch (Exception e) {
            logger.debug("获取当前登录用户失败: {}", e.getMessage());
        }
        return "";
    }

    /**
     * 获取操作类型名称
     */
    private String getTypeName(String type) {
        return switch (type) {
            case "ADD" -> "新增";
            case "UPDATE" -> "编辑";
            case "DELETE" -> "删除";
            case "START" -> "启动";
            case "STOP" -> "停止";
            case "LOGIN" -> "登录";
            case "LOGOUT" -> "登出";
            default -> type;
        };
    }
}
