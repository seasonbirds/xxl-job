package com.xxl.job.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SSO单点登录配置属性类
 * 
 * 用于读取application.properties中以"xxl.job.sso"为前缀的配置项
 * 
 * 配置示例：
 * xxl.job.sso.enabled=true
 * xxl.job.sso.secret=your-32-character-strong-secret-key
 * xxl.job.sso.token.expire.seconds=300
 * xxl.job.sso.login.failure.url=/auth/login
 * 
 * @author xxl-job
 */
@Component
@ConfigurationProperties(prefix = "xxl.job.sso")
public class XxlJobSsoProperties {

    /**
     * 是否启用SSO单点登录功能
     * 默认值：false（禁用）
     */
    private boolean enabled = false;

    /**
     * 预共享密钥，用于HMAC-SHA256签名验证
     * 企业运营后台与xxl-job-admin必须配置相同的密钥
     * 密钥长度建议至少32个字符
     */
    private String secret;

    /**
     * Token有效期（秒）
     * 默认值：300秒（5分钟）
     * 用于防止重放攻击，Token必须在有效期内使用
     */
    private long tokenExpireSeconds = 300;

    /**
     * SSO登录失败时的跳转URL
     * 默认值：/auth/login（跳转到传统登录页面）
     */
    private String loginFailureUrl = "/auth/login";

    /**
     * 获取SSO功能是否启用
     * @return true表示启用，false表示禁用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置SSO功能是否启用
     * @param enabled true表示启用，false表示禁用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取预共享密钥
     * @return 预共享密钥字符串
     */
    public String getSecret() {
        return secret;
    }

    /**
     * 设置预共享密钥
     * @param secret 预共享密钥字符串（建议至少32个字符）
     */
    public void setSecret(String secret) {
        this.secret = secret;
    }

    /**
     * 获取Token有效期（秒）
     * @return Token有效期，单位秒
     */
    public long getTokenExpireSeconds() {
        return tokenExpireSeconds;
    }

    /**
     * 设置Token有效期（秒）
     * @param tokenExpireSeconds Token有效期，单位秒
     */
    public void setTokenExpireSeconds(long tokenExpireSeconds) {
        this.tokenExpireSeconds = tokenExpireSeconds;
    }

    /**
     * 获取登录失败跳转URL
     * @return 登录失败时跳转的URL
     */
    public String getLoginFailureUrl() {
        return loginFailureUrl;
    }

    /**
     * 设置登录失败跳转URL
     * @param loginFailureUrl 登录失败时跳转的URL
     */
    public void setLoginFailureUrl(String loginFailureUrl) {
        this.loginFailureUrl = loginFailureUrl;
    }
}
