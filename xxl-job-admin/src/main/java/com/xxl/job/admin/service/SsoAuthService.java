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

@Service
public class SsoAuthService {

    private static final Logger logger = LoggerFactory.getLogger(SsoAuthService.class);

    @Resource
    private SsoProperties ssoProperties;

    @Resource
    private XxlJobUserMapper xxlJobUserMapper;

    private SecretKey getSigningKey() {
        String secret = ssoProperties.getJwt().getSecret();
        if (StringTool.isBlank(secret)) {
            throw SsoException.systemError(new IllegalArgumentException("SSO JWT secret is not configured"));
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String validateToken(String token) {
        if (StringTool.isBlank(token)) {
            throw SsoException.tokenEmpty();
        }

        try {
            SecretKey key = getSigningKey();

            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Date issuedAt = claims.getIssuedAt();
            if (issuedAt == null) {
                throw SsoException.tokenIssuedAtInvalid();
            }

            long maxExpirationMs = ssoProperties.getJwt().getExpiration() * 1000;
            if (System.currentTimeMillis() - issuedAt.getTime() > maxExpirationMs) {
                throw SsoException.tokenExpired();
            }

            String expectedIssuer = ssoProperties.getJwt().getIssuer();
            if (StringTool.isNotBlank(expectedIssuer)) {
                String actualIssuer = claims.getIssuer();
                if (!expectedIssuer.equals(actualIssuer)) {
                    logger.warn("SSO token issuer mismatch. expected: {}, actual: {}", expectedIssuer, actualIssuer);
                    throw SsoException.tokenInvalid();
                }
            }

            String expectedAudience = ssoProperties.getJwt().getAudience();
            if (StringTool.isNotBlank(expectedAudience)) {
                String actualAudience = claims.getAudience() != null && !claims.getAudience().isEmpty()
                        ? claims.getAudience().iterator().next() : null;
                if (!expectedAudience.equals(actualAudience)) {
                    logger.warn("SSO token audience mismatch. expected: {}, actual: {}", expectedAudience, actualAudience);
                    throw SsoException.tokenInvalid();
                }
            }

            String phone = claims.get("phone", String.class);
            if (phone == null) {
                phone = claims.getSubject();
            }

            if (!isValidPhone(phone)) {
                throw SsoException.phoneInvalid();
            }

            return phone;

        } catch (ExpiredJwtException e) {
            logger.info("SSO token expired", e);
            throw SsoException.tokenExpired();
        } catch (MalformedJwtException | SignatureException | IllegalArgumentException e) {
            logger.warn("SSO token validation failed", e);
            throw SsoException.tokenInvalid(e);
        } catch (SsoException e) {
            throw e;
        } catch (Exception e) {
            logger.error("SSO token validation error", e);
            throw SsoException.systemError(e);
        }
    }

    public XxlJobUser findOrCreateUser(String phone) {
        XxlJobUser user = xxlJobUserMapper.loadByUserName(phone);

        if (user != null) {
            return user;
        }

        if (!ssoProperties.isAutoCreateUser()) {
            logger.warn("SSO user not found and auto-create is disabled: phone={}", phone);
            throw SsoException.userNotFound();
        }

        logger.info("SSO auto-creating user: phone={}", phone);
        XxlJobUser newUser = new XxlJobUser();
        newUser.setUsername(phone);
        String randomPassword = UUIDTool.getSimpleUUID();
        newUser.setPassword(Sha256Tool.sha256(randomPassword));
        newUser.setRole(ssoProperties.getDefaultRole());
        newUser.setPermission(ssoProperties.getDefaultPermission());
        newUser.setToken("");

        xxlJobUserMapper.save(newUser);

        return xxlJobUserMapper.loadByUserName(phone);
    }

    private boolean isValidPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }
        return phone.matches("^1[3-9]\\d{9}$");
    }
}
