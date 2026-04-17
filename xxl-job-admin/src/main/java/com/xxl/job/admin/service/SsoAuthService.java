package com.xxl.job.admin.service;

import com.xxl.job.admin.config.SsoProperties;
import com.xxl.job.admin.exception.SsoException;
import com.xxl.job.admin.mapper.XxlJobUserMapper;
import com.xxl.job.admin.model.XxlJobUser;
import com.xxl.tool.core.StringTool;
import com.xxl.tool.crypto.Sha256Tool;
import com.xxl.tool.id.UUIDTool;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * SSO单点登录认证服务类
 *
 * 负责JWT Token的验证解析、用户查找/创建等核心认证逻辑
 *
 * 认证流程：
 * 1. 解析并验证JWT Token的签名
 * 2. 检查Token的过期时间和签发时间
 * 3. 验证可选的issuer和audience字段
 * 4. 从Token中提取手机号
 * 5. 根据手机号查找或创建用户
 *
 * @author xxl-job
 */
@Service
public class SsoAuthService {

    private static final Logger logger = LoggerFactory.getLogger(SsoAuthService.class);

    @Resource
    private SsoProperties ssoProperties;

    @Resource
    private XxlJobUserMapper xxlJobUserMapper;

    /**
     * 获取JWT签名密钥
     *
     * 从配置中读取密钥并转换为SecretKey对象
     * 密钥长度必须至少为256位以满足HS256算法要求
     *
     * @return SecretKey 签名密钥对象
     * @throws SsoException 密钥未配置时抛出
     */
    private SecretKey getSigningKey() {
        String secret = ssoProperties.getJwt().getSecret();
        if (StringTool.isBlank(secret)) {
            throw SsoException.systemError(new IllegalArgumentException("SSO JWT secret is not configured"));
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 验证JWT Token并提取手机号
     *
     * 执行以下验证步骤：
     * 1. 检查Token是否为空
     * 2. 验证JWT签名是否有效
     * 3. 检查Token是否已过期（exp字段）
     * 4. 检查签发时间是否在有效窗口内（防止重放攻击）
     * 5. 验证issuer签发者（可选）
     * 6. 验证audience受众（可选）
     * 7. 提取并验证手机号格式
     *
     * @param token JWT Token字符串
     * @return String 用户手机号
     * @throws SsoException 验证失败时抛出对应异常
     */
    public String validateToken(String token) {
        // 检查Token是否为空
        if (StringTool.isBlank(token)) {
            throw SsoException.tokenEmpty();
        }

        try {
            SecretKey key = getSigningKey();

            // 解析并验证JWT签名和过期时间
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 检查签发时间是否存在
            Date issuedAt = claims.getIssuedAt();
            if (issuedAt == null) {
                throw SsoException.tokenIssuedAtInvalid();
            }

            // 额外检查：签发时间不能超过配置的最大有效期
            // 这是为了防止令牌被重放攻击
            long maxExpirationMs = ssoProperties.getJwt().getExpiration() * 1000;
            if (System.currentTimeMillis() - issuedAt.getTime() > maxExpirationMs) {
                throw SsoException.tokenExpired();
            }

            // 验证签发者（可选）
            String expectedIssuer = ssoProperties.getJwt().getIssuer();
            if (StringTool.isNotBlank(expectedIssuer)) {
                String actualIssuer = claims.getIssuer();
                if (!expectedIssuer.equals(actualIssuer)) {
                    logger.warn("SSO token issuer mismatch. expected: {}, actual: {}", expectedIssuer, actualIssuer);
                    throw SsoException.tokenInvalid();
                }
            }

            // 验证受众（可选）
            String expectedAudience = ssoProperties.getJwt().getAudience();
            if (StringTool.isNotBlank(expectedAudience)) {
                String actualAudience = claims.getAudience() != null && !claims.getAudience().isEmpty()
                        ? claims.getAudience().iterator().next() : null;
                if (!expectedAudience.equals(actualAudience)) {
                    logger.warn("SSO token audience mismatch. expected: {}, actual: {}", expectedAudience, actualAudience);
                    throw SsoException.tokenInvalid();
                }
            }

            // 提取手机号（优先从phone字段获取，其次从subject获取）
            String phone = claims.get("phone", String.class);
            if (phone == null) {
                phone = claims.getSubject();
            }

            // 验证手机号格式
            if (!isValidPhone(phone)) {
                throw SsoException.phoneInvalid();
            }

            return phone;

        } catch (ExpiredJwtException e) {
            // JWT已过期
            logger.info("SSO token expired", e);
            throw SsoException.tokenExpired();
        } catch (MalformedJwtException | SignatureException | IllegalArgumentException e) {
            // JWT格式错误、签名验证失败、非法参数
            logger.warn("SSO token validation failed", e);
            throw SsoException.tokenInvalid(e);
        } catch (SsoException e) {
            // 自定义SSO异常直接抛出
            throw e;
        } catch (Exception e) {
            // 其他未知异常
            logger.error("SSO token validation error", e);
            throw SsoException.systemError(e);
        }
    }

    /**
     * 根据手机号查找或创建用户
     *
     * 1. 首先尝试根据手机号查找现有用户
     * 2. 如果用户不存在：
     *    - 如果开启了自动创建，则创建新用户
     *    - 如果未开启自动创建，则抛出异常
     *
     * @param phone 用户手机号
     * @return XxlJobUser 用户对象
     * @throws SsoException 用户不存在且未开启自动创建时抛出
     */
    public XxlJobUser findOrCreateUser(String phone) {
        // 尝试查找现有用户
        XxlJobUser user = xxlJobUserMapper.loadByUserName(phone);

        if (user != null) {
            return user;
        }

        // 用户不存在，检查是否开启自动创建
        if (!ssoProperties.isAutoCreateUser()) {
            logger.warn("SSO user not found and auto-create is disabled: phone={}", maskPhone(phone));
            throw SsoException.userNotFound();
        }

        // 自动创建用户
        logger.info("SSO auto-creating user: phone={}", maskPhone(phone));
        XxlJobUser newUser = new XxlJobUser();
        newUser.setUsername(phone);
        // 生成随机密码（SSO用户不使用密码登录）
        String randomPassword = UUIDTool.getSimpleUUID();
        newUser.setPassword(Sha256Tool.sha256(randomPassword));
        newUser.setRole(ssoProperties.getDefaultRole());
        newUser.setPermission(ssoProperties.getDefaultPermission());
        newUser.setToken("");

        xxlJobUserMapper.save(newUser);

        // 重新查询以获取生成的ID
        return xxlJobUserMapper.loadByUserName(phone);
    }

    /**
     * 验证手机号格式
     *
     * 目前仅支持中国大陆手机号格式：
     * - 以1开头
     * - 共11位数字
     * - 第二位为3-9之间的数字
     *
     * @param phone 手机号字符串
     * @return boolean 是否为有效手机号
     */
    private boolean isValidPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }
        // 中国大陆手机号正则：1开头，第二位3-9，共11位
        return phone.matches("^1[3-9]\\d{9}$");
    }

    /**
     * 掩码手机号，用于日志记录
     *
     * 将手机号中间4位替换为****，保护用户隐私
     * 例如：13800138000 -> 138****8000
     *
     * @param phone 原始手机号
     * @return String 掩码后的手机号
     */
    private String maskPhone(String phone) {
        if (StringTool.isBlank(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
