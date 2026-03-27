package com.xxl.job.admin.web.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxl.job.admin.core.model.XxlJobUser;
import com.xxl.job.admin.util.I18nUtil;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.constant.ConfConstant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

/**
 * 速率限制拦截器
 * 用于限制用户对特定接口的访问频率
 * 采用Redis实现，支持分布式环境
 *
 * @author xuxueli
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 速率限制计数，默认5次
     */
    @Value("${xxl.job.rate.limit.count:5}")
    private int rateLimitCount;

    /**
     * 速率限制过期时间（分钟），默认60分钟
     */
    @Value("${xxl.job.rate.limit.expire.minutes:60}")
    private int expireMinutes;

    public RateLimitInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 预处理，在请求处理之前进行调用
     * 进行速率限制检查，超过限制则拒绝请求
     *
     * @param request  HTTP请求
     * @param response HTTP响应
     * @param handler  处理器
     * @return true表示继续执行，false表示中断请求
     * @throws Exception 异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        XxlJobUser loginUser = (XxlJobUser) request.getAttribute(ConfConstant.REQUEST_ATTR_LOGIN_IDENTITY);

        if (loginUser == null) {
            return true;
        }

        String username = loginUser.getUsername();
        String uri = request.getRequestURI();

        try {
            String redisKey = buildRateLimitKey(username, uri);
            
            Long currentCount = stringRedisTemplate.opsForValue().increment(redisKey);
            
            if (currentCount != null && currentCount == 1) {
                stringRedisTemplate.expire(redisKey, expireMinutes, TimeUnit.MINUTES);
            }

            if (currentCount != null && currentCount > rateLimitCount) {
                writeResponse(response, new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("system_rate_limit_exceeded")));
                return false;
            }

        } catch (Exception e) {
            logger.error("Rate limit check error for user: {}, uri: {}", username, uri, e);
        }

        return true;
    }

    /**
     * 构建Redis中存储速率限制的key
     *
     * @param username 用户名
     * @param uri      请求URI
     * @return Redis key
     */
    private String buildRateLimitKey(String username, String uri) {
        return "xxl:job:rate:limit:" + username + ":" + uri;
    }

    /**
     * 写入响应信息
     *
     * @param response HTTP响应
     * @param returnT  返回对象
     * @throws IOException IO异常
     */
    private void writeResponse(HttpServletResponse response, ReturnT<?> returnT) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter writer = response.getWriter();
        ObjectMapper objectMapper = new ObjectMapper();
        writer.write(objectMapper.writeValueAsString(returnT));
        writer.flush();
        writer.close();
    }
}
