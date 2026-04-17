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
     * 企业运营后台的SSO入口URL
     * 当xxl-job-admin的Token过期时，自动重定向到该URL进行重新登录
     * 格式示例：http://enterprise-host:port/enterprise/sso/xxl-job-redirect
     */
    private String enterpriseSsoUrl;

    /**
     * Access Token有效期（秒）
     * 默认值：1800秒（30分钟）
     * Access Token用于日常访问资源，短期有效
     */
    private int accessTokenExpireSeconds = 1800;

    /**
     * Refresh Token有效期（秒）
     * 默认值：604800秒（7天）
     * Refresh Token用于刷新Access Token，长期有效
     */
    private int refreshTokenExpireSeconds = 604800;

    /**
     * Refresh Token加密密钥（Base64编码，32字节）
     * 用于AES-256-GCM加密Refresh Token
     * 如未配置，将使用secret派生的密钥
     */
    private String refreshTokenEncryptKey;

    /**
     * State参数有效期（秒）
     * 默认值：300秒（5分钟）
     * 用于防止CSRF攻击的State参数有效期
     */
    private int stateExpireSeconds = 300;

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

    /**
     * 获取企业运营后台的SSO入口URL
     * @return 企业SSO入口URL
     */
    public String getEnterpriseSsoUrl() {
        return enterpriseSsoUrl;
    }

    /**
     * 设置企业运营后台的SSO入口URL
     * @param enterpriseSsoUrl 企业SSO入口URL
     */
    public void setEnterpriseSsoUrl(String enterpriseSsoUrl) {
        this.enterpriseSsoUrl = enterpriseSsoUrl;
    }

    /**
     * 获取Access Token有效期（秒）
     * @return Access Token有效期
     */
    public int getAccessTokenExpireSeconds() {
        return accessTokenExpireSeconds;
    }

    /**
     * 设置Access Token有效期（秒）
     * @param accessTokenExpireSeconds Access Token有效期
     */
    public void setAccessTokenExpireSeconds(int accessTokenExpireSeconds) {
        this.accessTokenExpireSeconds = accessTokenExpireSeconds;
    }

    /**
     * 获取Refresh Token有效期（秒）
     * @return Refresh Token有效期
     */
    public int getRefreshTokenExpireSeconds() {
        return refreshTokenExpireSeconds;
    }

    /**
     * 设置Refresh Token有效期（秒）
     * @param refreshTokenExpireSeconds Refresh Token有效期
     */
    public void setRefreshTokenExpireSeconds(int refreshTokenExpireSeconds) {
        this.refreshTokenExpireSeconds = refreshTokenExpireSeconds;
    }

    /**
     * 获取Refresh Token加密密钥
     * @return Base64编码的加密密钥
     */
    public String getRefreshTokenEncryptKey() {
        return refreshTokenEncryptKey;
    }

    /**
     * 设置Refresh Token加密密钥
     * @param refreshTokenEncryptKey Base64编码的加密密钥（32字节）
     */
    public void setRefreshTokenEncryptKey(String refreshTokenEncryptKey) {
        this.refreshTokenEncryptKey = refreshTokenEncryptKey;
    }

    /**
     * 获取State参数有效期（秒）
     * @return State参数有效期
     */
    public int getStateExpireSeconds() {
        return stateExpireSeconds;
    }

    /**
     * 设置State参数有效期（秒）
     * @param stateExpireSeconds State参数有效期
     */
    public void setStateExpireSeconds(int stateExpireSeconds) {
        this.stateExpireSeconds = stateExpireSeconds;
    }
}
