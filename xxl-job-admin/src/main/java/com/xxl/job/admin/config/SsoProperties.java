package com.xxl.job.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "xxl.job.sso")
public class SsoProperties {

    private boolean enabled = true;

    private Jwt jwt = new Jwt();

    private boolean autoCreateUser = false;

    private int defaultRole = 0;

    private String defaultPermission = "";

    public static class Jwt {
        private String secret;
        private long expiration = 300;
        private String issuer;
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
}
