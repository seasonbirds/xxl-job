package com.xxl.job.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SSO 单点登录配置属性类
 *
 * 用于配置与企业运营后台对接的SSO相关参数
 *
 * @author xxl-job
 */
@Component
@ConfigurationProperties(prefix = "xxl.job.sso")
public class SsoProperties {

    /**
     * 是否启用SSO功能
     */
    private boolean enabled = true;

    /**
     * JWT相关配置
     */
    private Jwt jwt = new Jwt();

    /**
     * 是否自动创建用户（当手机号不存在时）
     */
    private boolean autoCreateUser = false;

    /**
     * 自动创建用户时的默认角色（0-普通用户，1-管理员）
     */
    private int defaultRole = 0;

    /**
     * 自动创建用户时的默认权限（执行器ID列表，逗号分隔）
     */
    private String defaultPermission = "";

    /**
     * 企业运营后台SSO入口地址
     * 当xxl-job-admin会话过期时，自动重定向到此地址
     * 示例: http://portal.example.com/api/sso/redirect-to-xxl-job
     */
    private String serverUrl;

    /**
     * 是否开启自动重定向到运营后台
     * true=会话过期时自动跳转回运营后台进行SSO验证
     */
    private boolean autoRedirect = false;

    /**
     * JWT配置内部类
     */
    public static class Jwt {

        /**
         * JWT签名密钥（必须与企业运营后台一致，至少256位）
         */
        private String secret;

        /**
         * JWT Token的最大有效时间（秒），用于防止重放攻击
         * 默认300秒(5分钟)
         */
        private long expiration = 300;

        /**
         * JWT签发者（可选，用于验证iss字段）
         */
        private String issuer;

        /**
         * JWT受众（可选，用于验证aud字段）
         */
        private String audience;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpiration() {
            return expiration;
        }

        public void setExpiration(long expiration) {
            this.expiration = expiration;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getAudience() {
            return audience;
        }

        public void setAudience(String audience) {
            this.audience = audience;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public boolean isAutoCreateUser() {
        return autoCreateUser;
    }

    public void setAutoCreateUser(boolean autoCreateUser) {
        this.autoCreateUser = autoCreateUser;
    }

    public int getDefaultRole() {
        return defaultRole;
    }

    public void setDefaultRole(int defaultRole) {
        this.defaultRole = defaultRole;
    }

    public String getDefaultPermission() {
        return defaultPermission;
    }

    public void setDefaultPermission(String defaultPermission) {
        this.defaultPermission = defaultPermission;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public boolean isAutoRedirect() {
        return autoRedirect;
    }

    public void setAutoRedirect(boolean autoRedirect) {
        this.autoRedirect = autoRedirect;
    }
}
