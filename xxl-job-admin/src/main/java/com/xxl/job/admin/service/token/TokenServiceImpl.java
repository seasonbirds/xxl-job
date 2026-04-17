package com.xxl.job.admin.service.token;

import com.xxl.job.admin.config.XxlJobSsoProperties;
import com.xxl.job.admin.util.CryptoUtil;
import com.xxl.tool.json.GsonTool;
import com.xxl.tool.response.Response;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token服务实现类
 * 
 * 实现双Token机制：
 * - Access Token：JWT格式，HMAC-SHA256签名，短期有效（默认30分钟）
 * - Refresh Token：AES-256-GCM加密，长期有效（默认7天）
 * 
 * 安全特性：
 * 1. Refresh Token每次刷新后轮换（单次使用机制）
 * 2. 已使用的Refresh Token记录在缓存中
 * 3. Access Token使用JWT格式，无状态
 * 
 * @author xxl-job
 */
@Service
public class TokenServiceImpl implements TokenService {

    private static final Logger logger = LoggerFactory.getLogger(TokenServiceImpl.class);

    private static final String JWT_HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final String ENCRYPT_KEY_CACHE = "encrypt_key_derived";

    @Resource
    private XxlJobSsoProperties ssoProperties;

    /**
     * 已使用的Refresh Token缓存（用于实现单次使用机制）
     * Key: Refresh Token ID
     * Value: 过期时间戳
     */
    private final ConcurrentHashMap<String, Long> usedRefreshTokens = new ConcurrentHashMap<>();

    /**
     * 派生的Refresh Token加密密钥
     */
    private volatile String derivedEncryptKey;

    @PostConstruct
    public void init() {
        logger.info("TokenService initialized, accessTokenExpire={}s, refreshTokenExpire={}s",
                ssoProperties.getAccessTokenExpireSeconds(),
                ssoProperties.getRefreshTokenExpireSeconds());
    }

    /**
     * 生成Token对
     * 
     * @param userId 用户ID
     * @param phone 用户手机号
     * @return Token对
     */
    @Override
    public Response<TokenPair> generateTokenPair(String userId, String phone) {
        if (userId == null || phone == null) {
            return Response.ofFail("userId or phone is null");
        }

        try {
            AccessTokenInfo accessTokenInfo = AccessTokenInfo.build(
                    userId,
                    phone,
                    ssoProperties.getAccessTokenExpireSeconds()
            );

            RefreshTokenInfo refreshTokenInfo = RefreshTokenInfo.build(
                    userId,
                    phone,
                    ssoProperties.getRefreshTokenExpireSeconds()
            );

            String accessToken = generateAccessToken(accessTokenInfo);
            String refreshToken = encryptRefreshToken(refreshTokenInfo);

            accessTokenInfo.setRefreshTokenId(refreshTokenInfo.getTokenId());

            TokenPair tokenPair = new TokenPair(
                    accessToken,
                    refreshToken,
                    accessTokenInfo,
                    refreshTokenInfo
            );

            logger.info("Generated token pair for user: {}, phone: {}", userId, maskPhone(phone));
            return Response.ofSuccess(tokenPair);

        } catch (Exception e) {
            logger.error("Failed to generate token pair", e);
            return Response.ofFail("Failed to generate token: " + e.getMessage());
        }
    }

    /**
     * 验证Access Token
     * 
     * @param accessToken Access Token字符串
     * @return 验证结果
     */
    @Override
    public Response<AccessTokenInfo> validateAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            return Response.ofFail("Access token is empty");
        }

        try {
            String[] parts = accessToken.split("\\.");
            if (parts.length != 3) {
                return Response.ofFail("Invalid JWT format");
            }

            String headerBase64 = parts[0];
            String payloadBase64 = parts[1];
            String signature = parts[2];

            String signingInput = headerBase64 + "." + payloadBase64;
            String expectedSignature = CryptoUtil.calculateHmacSha256(signingInput, ssoProperties.getSecret());

            if (!CryptoUtil.constantTimeEquals(signature, expectedSignature)) {
                logger.warn("Access token signature validation failed");
                return Response.ofFail("Invalid token signature");
            }

            String payloadJson = new String(
                    Base64.getUrlDecoder().decode(payloadBase64),
                    StandardCharsets.UTF_8
            );

            com.google.gson.reflect.TypeToken<Map<String, Object>> typeToken =
                    new com.google.gson.reflect.TypeToken<Map<String, Object>>() {};
            Map<String, Object> payload = GsonTool.fromJson(payloadJson, typeToken.getType());

            AccessTokenInfo tokenInfo = AccessTokenInfo.fromPayload(payload);
            if (tokenInfo == null) {
                return Response.ofFail("Invalid token type");
            }

            if (tokenInfo.isExpired()) {
                return Response.ofFail("Access token has expired");
            }

            return Response.ofSuccess(tokenInfo);

        } catch (Exception e) {
            logger.error("Access token validation error", e);
            return Response.ofFail("Token validation error: " + e.getMessage());
        }
    }

    /**
     * 验证Refresh Token
     * 
     * @param refreshToken Refresh Token字符串（加密形式）
     * @return 验证结果
     */
    @Override
    public Response<RefreshTokenInfo> validateRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            return Response.ofFail("Refresh token is empty");
        }

        try {
            RefreshTokenInfo tokenInfo = decryptRefreshToken(refreshToken);
            if (tokenInfo == null) {
                return Response.ofFail("Invalid refresh token");
            }

            if (isRefreshTokenUsed(tokenInfo.getTokenId())) {
                logger.warn("Refresh token has been used before: {}", tokenInfo.getTokenId());
                return Response.ofFail("Refresh token has been revoked");
            }

            if (tokenInfo.isExpired()) {
                return Response.ofFail("Refresh token has expired");
            }

            return Response.ofSuccess(tokenInfo);

        } catch (Exception e) {
            logger.error("Refresh token validation error", e);
            return Response.ofFail("Token validation error: " + e.getMessage());
        }
    }

    /**
     * 使用Refresh Token刷新Access Token
     * 
     * 安全特性：
     * 1. 每次刷新生成新的Access Token
     * 2. 每次刷新生成新的Refresh Token（轮换机制）
     * 3. 旧的Refresh Token标记为已使用
     * 
     * @param oldRefreshToken 旧的Refresh Token
     * @return 新的Token对
     */
    @Override
    public Response<TokenPair> refreshAccessToken(String oldRefreshToken) {
        Response<RefreshTokenInfo> validation = validateRefreshToken(oldRefreshToken);
        if (!validation.isSuccess()) {
            return Response.ofFail(validation.getMsg());
        }

        RefreshTokenInfo oldTokenInfo = validation.getData();

        markRefreshTokenAsUsed(oldTokenInfo.getTokenId(), oldTokenInfo.getExpiresAt());

        return generateTokenPair(oldTokenInfo.getUserId(), oldTokenInfo.getPhone());
    }

    /**
     * 撤销Token
     * 
     * @param refreshToken Refresh Token
     * @return 操作结果
     */
    @Override
    public Response<Boolean> revokeToken(String refreshToken) {
        try {
            RefreshTokenInfo tokenInfo = decryptRefreshToken(refreshToken);
            if (tokenInfo != null) {
                markRefreshTokenAsUsed(tokenInfo.getTokenId(), tokenInfo.getExpiresAt());
                logger.info("Revoked refresh token: {}", tokenInfo.getTokenId());
            }
            return Response.ofSuccess(true);
        } catch (Exception e) {
            logger.error("Failed to revoke token", e);
            return Response.ofFail("Failed to revoke token: " + e.getMessage());
        }
    }

    /**
     * 生成Access Token（JWT格式）
     * 
     * JWT格式：Base64Url(Header).Base64Url(Payload).HMAC-SHA256(SigningInput)
     * 
     * @param tokenInfo Access Token信息
     * @return JWT字符串
     */
    private String generateAccessToken(AccessTokenInfo tokenInfo) {
        String headerBase64 = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(JWT_HEADER.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> payload = tokenInfo.toPayload();
        String payloadJson = GsonTool.toJson(payload);
        String payloadBase64 = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

        String signingInput = headerBase64 + "." + payloadBase64;
        String signature = CryptoUtil.calculateHmacSha256(signingInput, ssoProperties.getSecret());

        return headerBase64 + "." + payloadBase64 + "." + signature;
    }

    /**
     * 加密Refresh Token
     * 
     * 使用AES-256-GCM加密，格式：
     * [IV(12字节)][密文][认证标签(16字节)]
     * 
     * @param tokenInfo Refresh Token信息
     * @return Base64Url编码的加密结果
     */
    private String encryptRefreshToken(RefreshTokenInfo tokenInfo) {
        String encryptKey = getEncryptKey();
        Map<String, Object> payload = tokenInfo.toPayload();
        String payloadJson = GsonTool.toJson(payload);
        return CryptoUtil.encryptAesGcm(payloadJson, encryptKey);
    }

    /**
     * 解密Refresh Token
     * 
     * @param encryptedToken 加密的Token
     * @return RefreshTokenInfo，解密失败返回null
     */
    private RefreshTokenInfo decryptRefreshToken(String encryptedToken) {
        try {
            String encryptKey = getEncryptKey();
            String payloadJson = CryptoUtil.decryptAesGcm(encryptedToken, encryptKey);
            
            com.google.gson.reflect.TypeToken<Map<String, Object>> typeToken =
                    new com.google.gson.reflect.TypeToken<Map<String, Object>>() {};
            Map<String, Object> payload = GsonTool.fromJson(payloadJson, typeToken.getType());
            
            return RefreshTokenInfo.fromPayload(payload);
        } catch (Exception e) {
            logger.debug("Failed to decrypt refresh token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取Refresh Token加密密钥
     * 
     * 优先使用配置的refreshTokenEncryptKey，
     * 如未配置则从secret派生密钥。
     * 
     * @return Base64编码的32字节密钥
     */
    private String getEncryptKey() {
        String configuredKey = ssoProperties.getRefreshTokenEncryptKey();
        if (configuredKey != null && !configuredKey.isEmpty()) {
            return configuredKey;
        }

        if (derivedEncryptKey == null) {
            synchronized (this) {
                if (derivedEncryptKey == null) {
                    String secret = ssoProperties.getSecret();
                    if (secret == null || secret.isEmpty()) {
                        throw new IllegalStateException("SSO secret is not configured");
                    }
                    
                    String keyMaterial = CryptoUtil.calculateHmacSha256(secret, "refresh_token_encrypt_key");
                    byte[] keyBytes = new byte[32];
                    System.arraycopy(
                            keyMaterial.getBytes(StandardCharsets.UTF_8),
                            0,
                            keyBytes,
                            0,
                            Math.min(keyMaterial.length(), 32)
                    );
                    
                    derivedEncryptKey = Base64.getEncoder().encodeToString(keyBytes);
                    logger.info("Derived refresh token encrypt key from secret");
                }
            }
        }
        
        return derivedEncryptKey;
    }

    /**
     * 检查Refresh Token是否已被使用
     * 
     * 用于实现单次使用机制，防止Token复用攻击。
     * 
     * @param tokenId Token ID
     * @return true表示已使用
     */
    private boolean isRefreshTokenUsed(String tokenId) {
        if (tokenId == null) {
            return false;
        }
        
        Long expireTime = usedRefreshTokens.get(tokenId);
        if (expireTime == null) {
            return false;
        }
        
        if (System.currentTimeMillis() > expireTime) {
            usedRefreshTokens.remove(tokenId);
            return false;
        }
        
        return true;
    }

    /**
     * 标记Refresh Token为已使用
     * 
     * @param tokenId Token ID
     * @param expireTime Token过期时间
     */
    private void markRefreshTokenAsUsed(String tokenId, Long expireTime) {
        if (tokenId == null) {
            return;
        }
        
        long cacheExpireTime = expireTime != null ? expireTime : 
                System.currentTimeMillis() + ssoProperties.getRefreshTokenExpireSeconds() * 1000L;
        
        usedRefreshTokens.put(tokenId, cacheExpireTime);
        
        if (usedRefreshTokens.size() > 10000) {
            cleanupExpiredTokens();
        }
    }

    /**
     * 清理已过期的Token缓存
     */
    private void cleanupExpiredTokens() {
        long now = System.currentTimeMillis();
        usedRefreshTokens.entrySet().removeIf(entry -> entry.getValue() < now);
        logger.info("Cleaned up expired refresh token cache, remaining: {}", usedRefreshTokens.size());
    }

    /**
     * 手机号脱敏
     * 
     * 用于日志记录，保护用户隐私。
     * 
     * @param phone 原始手机号
     * @return 脱敏后的手机号（如 138****8000）
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 11) {
            return "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
