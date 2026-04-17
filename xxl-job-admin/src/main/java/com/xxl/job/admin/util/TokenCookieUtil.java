package com.xxl.job.admin.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Token Cookie工具类
 * 
 * 用于安全地存储和读取Refresh Token。
 * 
 * 安全特性：
 * 1. HttpOnly：防止JavaScript访问
 * 2. Secure：只在HTTPS下传输（生产环境）
 * 3. SameSite：防止CSRF攻击
 * 4. 加密存储：Token内容已加密
 * 
 * @author xxl-job
 */
public final class TokenCookieUtil {

    private static final Logger logger = LoggerFactory.getLogger(TokenCookieUtil.class);

    public static final String REFRESH_TOKEN_COOKIE_NAME = "xxl_job_rt";
    public static final String ACCESS_TOKEN_COOKIE_NAME = "xxl_job_at";

    private static final int COOKIE_MAX_AGE = 7 * 24 * 60 * 60;
    private static final String COOKIE_PATH = "/";

    private TokenCookieUtil() {
    }

    /**
     * 存储Refresh Token到Cookie
     * 
     * @param response HTTP响应
     * @param refreshToken Refresh Token（加密形式）
     * @param maxAge Cookie有效期（秒）
     * @param isSecure 是否只在HTTPS下传输
     * @param contextPath 应用上下文路径
     */
    public static void setRefreshTokenCookie(
            HttpServletResponse response,
            String refreshToken,
            int maxAge,
            boolean isSecure,
            String contextPath) {
        
        String path = (contextPath != null && !contextPath.isEmpty()) ? contextPath : COOKIE_PATH;
        
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        cookie.setMaxAge(maxAge);
        cookie.setPath(path);
        cookie.setHttpOnly(true);
        cookie.setSecure(isSecure);
        
        try {
            cookie.setAttribute("SameSite", "Lax");
        } catch (Exception e) {
            logger.debug("SameSite attribute not supported by this servlet version");
        }
        
        response.addCookie(cookie);
        logger.debug("Set refresh token cookie, maxAge={}, path={}", maxAge, path);
    }

    /**
     * 存储Access Token到Cookie（可选）
     * 
     * Access Token通常不建议存储在Cookie中，
     * 因为它短期有效且可能被频繁刷新。
     * 
     * @param response HTTP响应
     * @param accessToken Access Token
     * @param maxAge Cookie有效期（秒）
     * @param isSecure 是否只在HTTPS下传输
     * @param contextPath 应用上下文路径
     */
    public static void setAccessTokenCookie(
            HttpServletResponse response,
            String accessToken,
            int maxAge,
            boolean isSecure,
            String contextPath) {
        
        String path = (contextPath != null && !contextPath.isEmpty()) ? contextPath : COOKIE_PATH;
        
        Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE_NAME, accessToken);
        cookie.setMaxAge(maxAge);
        cookie.setPath(path);
        cookie.setHttpOnly(true);
        cookie.setSecure(isSecure);
        
        try {
            cookie.setAttribute("SameSite", "Lax");
        } catch (Exception e) {
            logger.debug("SameSite attribute not supported");
        }
        
        response.addCookie(cookie);
    }

    /**
     * 从Cookie中读取Refresh Token
     * 
     * @param request HTTP请求
     * @return Refresh Token，不存在返回null
     */
    public static String getRefreshToken(HttpServletRequest request) {
        return getCookieValue(request, REFRESH_TOKEN_COOKIE_NAME);
    }

    /**
     * 从Cookie中读取Access Token
     * 
     * @param request HTTP请求
     * @return Access Token，不存在返回null
     */
    public static String getAccessToken(HttpServletRequest request) {
        return getCookieValue(request, ACCESS_TOKEN_COOKIE_NAME);
    }

    /**
     * 从Cookie中读取指定名称的值
     * 
     * @param request HTTP请求
     * @param cookieName Cookie名称
     * @return Cookie值，不存在返回null
     */
    private static String getCookieValue(HttpServletRequest request, String cookieName) {
        if (request == null) {
            return null;
        }
        
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        
        return null;
    }

    /**
     * 删除Refresh Token Cookie
     * 
     * @param response HTTP响应
     * @param contextPath 应用上下文路径
     */
    public static void deleteRefreshTokenCookie(HttpServletResponse response, String contextPath) {
        String path = (contextPath != null && !contextPath.isEmpty()) ? contextPath : COOKIE_PATH;
        
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, "");
        cookie.setMaxAge(0);
        cookie.setPath(path);
        cookie.setHttpOnly(true);
        
        response.addCookie(cookie);
        logger.debug("Deleted refresh token cookie");
    }

    /**
     * 删除Access Token Cookie
     * 
     * @param response HTTP响应
     * @param contextPath 应用上下文路径
     */
    public static void deleteAccessTokenCookie(HttpServletResponse response, String contextPath) {
        String path = (contextPath != null && !contextPath.isEmpty()) ? contextPath : COOKIE_PATH;
        
        Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE_NAME, "");
        cookie.setMaxAge(0);
        cookie.setPath(path);
        cookie.setHttpOnly(true);
        
        response.addCookie(cookie);
    }

    /**
     * 删除所有Token Cookie
     * 
     * @param response HTTP响应
     * @param contextPath 应用上下文路径
     */
    public static void deleteAllTokenCookies(HttpServletResponse response, String contextPath) {
        deleteRefreshTokenCookie(response, contextPath);
        deleteAccessTokenCookie(response, contextPath);
    }

    /**
     * 判断是否应该使用Secure Cookie
     * 
     * 生产环境建议使用HTTPS，此时应该设置Secure标志。
     * 
     * @param request HTTP请求
     * @return true表示应该使用Secure Cookie
     */
    public static boolean shouldUseSecure(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        
        String scheme = request.getScheme();
        String header = request.getHeader("X-Forwarded-Proto");
        
        return "https".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(header);
    }
}
