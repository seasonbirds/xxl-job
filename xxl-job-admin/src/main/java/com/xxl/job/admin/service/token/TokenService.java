package com.xxl.job.admin.service.token;

import com.xxl.tool.response.Response;

/**
 * Token服务接口
 * 
 * 提供双Token机制的核心功能：
 * - Access Token：短期有效，用于访问资源
 * - Refresh Token：长期有效，用于刷新Access Token
 * 
 * @author xxl-job
 */
public interface TokenService {

    /**
     * 生成Token对
     * 
     * 为用户生成Access Token和Refresh Token。
     * 
     * @param userId 用户ID
     * @param phone 用户手机号
     * @return Token对响应
     */
    Response<TokenPair> generateTokenPair(String userId, String phone);

    /**
     * 验证Access Token
     * 
     * 验证Access Token的签名和有效期。
     * 
     * @param accessToken Access Token字符串
     * @return 验证结果，成功时返回AccessTokenInfo
     */
    Response<AccessTokenInfo> validateAccessToken(String accessToken);

    /**
     * 验证Refresh Token
     * 
     * 解密并验证Refresh Token的有效性。
     * 
     * @param refreshToken Refresh Token字符串（加密形式）
     * @return 验证结果，成功时返回RefreshTokenInfo
     */
    Response<RefreshTokenInfo> validateRefreshToken(String refreshToken);

    /**
     * 使用Refresh Token刷新Access Token
     * 
     * 安全特性：
     * 1. 每次刷新生成新的Access Token
     * 2. 每次刷新生成新的Refresh Token（单次使用机制）
     * 3. 旧的Refresh Token失效
     * 
     * @param oldRefreshToken 旧的Refresh Token
     * @return 新的Token对
     */
    Response<TokenPair> refreshAccessToken(String oldRefreshToken);

    /**
     * 撤销Token
     * 
     * 使指定的Refresh Token失效。
     * 
     * @param refreshToken Refresh Token
     * @return 操作结果
     */
    Response<Boolean> revokeToken(String refreshToken);

    /**
     * Token对
     * 
     * 包含Access Token和Refresh Token。
     */
    record TokenPair(
        String accessToken,
        String refreshToken,
        AccessTokenInfo accessTokenInfo,
        RefreshTokenInfo refreshTokenInfo
    ) {}
}
