package com.xxl.job.admin.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IP工具类
 * 提供获取客户端IP等网络相关工具方法
 *
 * @author xxl-job
 */
public class IpUtil {

    /**
     * 获取客户端IP地址
     * 支持反向代理场景（X-Forwarded-For等header）
     *
     * @param request HTTP请求
     * @return 客户端IP地址
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (isEmptyIp(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (isEmptyIp(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (isEmptyIp(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (isEmptyIp(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (isEmptyIp(ip)) {
            ip = request.getRemoteAddr();
        }

        // 多个代理情况，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.substring(0, ip.indexOf(",")).trim();
        }

        return ip != null ? ip : "";
    }

    /**
     * 判断IP是否为空或未知
     */
    private static boolean isEmptyIp(String ip) {
        return ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip);
    }

}
