package com.xxl.job.admin.service.token;

/**
 * Token信息基类
 * 
 * 包含AccessToken和RefreshToken的公共属性。
 * 
 * @author xxl-job
 */
public abstract class TokenInfo {

    /**
     * 用户ID
     */
    protected String userId;

    /**
     * 用户手机号（与username关联）
     */
    protected String phone;

    /**
     * Token创建时间戳（毫秒）
     */
    protected Long createdAt;

    /**
     * Token过期时间戳（毫秒）
     */
    protected Long expiresAt;

    /**
     * 随机字符串，增加Token的唯一性
     */
    protected String nonce;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    /**
     * 检查Token是否已过期
     * @return true表示已过期
     */
    public boolean isExpired() {
        return expiresAt == null || System.currentTimeMillis() > expiresAt;
    }

    /**
     * 检查Token是否即将过期
     * @param thresholdSeconds 阈值（秒）
     * @return true表示即将过期
     */
    public boolean isAboutToExpire(int thresholdSeconds) {
        if (expiresAt == null) {
            return true;
        }
        long remaining = expiresAt - System.currentTimeMillis();
        return remaining < thresholdSeconds * 1000L;
    }
}
