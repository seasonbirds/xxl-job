package com.xxl.job.admin.filter;

import com.xxl.job.admin.util.I18nUtil;
import com.xxl.sso.core.helper.XxlSsoHelper;
import com.xxl.sso.core.model.LoginInfo;
import com.xxl.tool.response.Response;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 速率限制过滤器
 * 用于限制接口调用频率，避免系统压力过大
 * 使用Redis实现分布式速率限制，支持集群部署
 *
 * @author xuxueli
 */
@Component
@WebFilter(filterName = "rateLimitFilter", urlPatterns = "/joblog/exportLog")
public class RateLimitFilter implements Filter {

    @Value("${xxl.job.log.export.rate-limit:5}")
    private int rateLimitCount;

    @Value("${xxl.job.log.export.rate-limit-period:5}")
    private int rateLimitPeriod; // minutes

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 初始化逻辑
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 获取当前用户ID
        String userId = getCurrentUserId(httpRequest);
        if (userId != null) {
            String cacheKey = "exportLog_rate_limit:" + userId;
            
            // 使用Redis进行速率限制
            Long currentCount = stringRedisTemplate.opsForValue().increment(cacheKey);
            
            // 如果是第一次计数，设置过期时间
            if (currentCount != null && currentCount == 1) {
                stringRedisTemplate.expire(cacheKey, rateLimitPeriod, TimeUnit.MINUTES);
            }

            // 检查是否超过频率限制
            if (currentCount != null && currentCount > rateLimitCount) {
                httpResponse.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
                httpResponse.getWriter().write(I18nUtil.getString("system_too_many_requests"));
                return;
            }
        }

        // 继续执行后续逻辑
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // 销毁逻辑
    }

    /**
     * 获取当前用户ID
     */
    private String getCurrentUserId(HttpServletRequest request) {
        Response<LoginInfo> loginInfoResponse = XxlSsoHelper.loginCheckWithAttr(request);
        if (loginInfoResponse.isSuccess()) {
            return loginInfoResponse.getData().getUserId();
        }
        return null;
    }
}