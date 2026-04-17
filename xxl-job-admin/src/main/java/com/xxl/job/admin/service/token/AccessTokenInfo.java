package com.xxl.job.admin.service.token;

/**
 * Access Token信息类
 * 
 * Access Token用于访问xxl-job-admin的受保护资源，
 * 特点是短期有效（默认30分钟）。
 * 
 * 与xxl-sso的关系：
 * - Access Token本质上与xxl-sso的Session Token功能相同
 * - 新增的双Token机制是在xxl-sso之上的增强层
 * - Refresh Token用于在xxl-sso Token过期时自动刷新
 * 
 * @author xxl-job
 */
public class AccessTokenInfo extends TokenInfo {

    /**
     * Token类型标识，固定为"access"
     */
    public static final String TOKEN_TYPE = "access";

    /**
     * 关联的Refresh Token ID（可选）
     * 用于追踪哪个Refresh Token生成了这个Access Token
     */
    private String refreshTokenId;

    public String getRefreshTokenId() {
        return refreshTokenId;
    }

    public void setRefreshTokenId(String refreshTokenId) {
        this.refreshTokenId = refreshTokenId;
    }

    /**
     * 构建AccessTokenInfo
     * 
     * @param userId 用户ID
     * @param phone 用户手机号
     * @param expireSeconds 有效期（秒）
     * @return AccessTokenInfo实例
     */
    public static AccessTokenInfo build(String userId, String phone, int expireSeconds) {
        AccessTokenInfo token = new AccessTokenInfo();
        token.setUserId(userId);
        token.setPhone(phone);
        token.setCreatedAt(System.currentTimeMillis());
        token.setExpiresAt(System.currentTimeMillis() + expireSeconds * 1000L);
        token.setNonce(java.util.UUID.randomUUID().toString().replace("-", ""));
        return token;
    }

    /**
     * 从JWT Payload解析AccessTokenInfo
     * 
     * @param payload JWT Payload（Map格式）
     * @return AccessTokenInfo实例，解析失败返回null
     */
    public static AccessTokenInfo fromPayload(java.util.Map<String, Object> payload) {
        if (payload == null) {
            return null;
        }
        
        String type = (String) payload.get("type");
        if (!TOKEN_TYPE.equals(type)) {
            return null;
        }
        
        AccessTokenInfo token = new AccessTokenInfo();
        token.setUserId((String) payload.get("sub"));
        token.setPhone((String) payload.get("phone"));
        
        Object createdAt = payload.get("iat");
        if (createdAt instanceof Number) {
            token.setCreatedAt(((Number) createdAt).longValue() * 1000);
        }
        
        Object expiresAt = payload.get("exp");
        if (expiresAt instanceof Number) {
            token.setExpiresAt(((Number) expiresAt).longValue() * 1000);
        }
        
        token.setNonce((String) payload.get("nonce"));
        token.setRefreshTokenId((String) payload.get("rt_id"));
        
        return token;
    }

    /**
     * 转换为JWT Payload Map
     * 
     * @return JWT Payload Map
     */
    public java.util.Map<String, Object> toPayload() {
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("type", TOKEN_TYPE);
        if (userId != null) payload.put("sub", userId);
        if (phone != null) payload.put("phone", phone);
        if (createdAt != null) payload.put("iat", createdAt / 1000);
        if (expiresAt != null) payload.put("exp", expiresAt / 1000);
        if (nonce != null) payload.put("nonce", nonce);
        if (refreshTokenId != null) payload.put("rt_id", refreshTokenId);
        return payload;
    }
}
