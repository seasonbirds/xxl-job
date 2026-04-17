package com.xxl.job.admin.service.token;

/**
 * Refresh Token信息类
 * 
 * Refresh Token用于在Access Token过期时自动刷新，
 * 特点是长期有效（默认7天）。
 * 
 * 安全特性：
 * 1. 加密存储：使用AES-256-GCM加密
 * 2. 单次使用：每次刷新后生成新的Refresh Token
 * 3. 用户绑定：与用户ID强绑定
 * 
 * @author xxl-job
 */
public class RefreshTokenInfo extends TokenInfo {

    /**
     * Token类型标识，固定为"refresh"
     */
    public static final String TOKEN_TYPE = "refresh";

    /**
     * Refresh Token唯一标识
     */
    private String tokenId;

    /**
     * 版本号，用于实现单次使用机制
     * 每次刷新后版本号递增
     */
    private int version;

    /**
     * 关联的xxl-sso Session ID（可选）
     */
    private String sessionId;

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * 构建RefreshTokenInfo
     * 
     * @param userId 用户ID
     * @param phone 用户手机号
     * @param expireSeconds 有效期（秒）
     * @return RefreshTokenInfo实例
     */
    public static RefreshTokenInfo build(String userId, String phone, int expireSeconds) {
        RefreshTokenInfo token = new RefreshTokenInfo();
        token.setTokenId(generateTokenId());
        token.setUserId(userId);
        token.setPhone(phone);
        token.setCreatedAt(System.currentTimeMillis());
        token.setExpiresAt(System.currentTimeMillis() + expireSeconds * 1000L);
        token.setNonce(java.util.UUID.randomUUID().toString().replace("-", ""));
        token.setVersion(1);
        return token;
    }

    /**
     * 生成Token ID
     * 格式：RT_ + 时间戳 + 随机字符串
     * 
     * @return Token ID
     */
    private static String generateTokenId() {
        return "RT_" + System.currentTimeMillis() + "_" + 
               java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    /**
     * 从加密的Payload解析RefreshTokenInfo
     * 
     * @param payload 解密后的Payload（Map格式）
     * @return RefreshTokenInfo实例，解析失败返回null
     */
    public static RefreshTokenInfo fromPayload(java.util.Map<String, Object> payload) {
        if (payload == null) {
            return null;
        }
        
        String type = (String) payload.get("type");
        if (!TOKEN_TYPE.equals(type)) {
            return null;
        }
        
        RefreshTokenInfo token = new RefreshTokenInfo();
        token.setTokenId((String) payload.get("tid"));
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
        
        Object version = payload.get("ver");
        if (version instanceof Number) {
            token.setVersion(((Number) version).intValue());
        }
        
        token.setSessionId((String) payload.get("sid"));
        
        return token;
    }

    /**
     * 转换为加密Payload Map
     * 
     * @return Payload Map
     */
    public java.util.Map<String, Object> toPayload() {
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("type", TOKEN_TYPE);
        if (tokenId != null) payload.put("tid", tokenId);
        if (userId != null) payload.put("sub", userId);
        if (phone != null) payload.put("phone", phone);
        if (createdAt != null) payload.put("iat", createdAt / 1000);
        if (expiresAt != null) payload.put("exp", expiresAt / 1000);
        if (nonce != null) payload.put("nonce", nonce);
        payload.put("ver", version);
        if (sessionId != null) payload.put("sid", sessionId);
        return payload;
    }

    /**
     * 生成新版本的Refresh Token（用于单次使用机制）
     * 
     * 每次刷新Access Token时，生成新的Refresh Token，
     * 旧的Refresh Token失效，防止Token复用攻击。
     * 
     * @return 新版本的RefreshTokenInfo
     */
    public RefreshTokenInfo rotate() {
        RefreshTokenInfo newToken = new RefreshTokenInfo();
        newToken.setTokenId(generateTokenId());
        newToken.setUserId(this.userId);
        newToken.setPhone(this.phone);
        newToken.setCreatedAt(System.currentTimeMillis());
        newToken.setExpiresAt(this.expiresAt);
        newToken.setNonce(java.util.UUID.randomUUID().toString().replace("-", ""));
        newToken.setVersion(this.version + 1);
        newToken.setSessionId(this.sessionId);
        return newToken;
    }
}
