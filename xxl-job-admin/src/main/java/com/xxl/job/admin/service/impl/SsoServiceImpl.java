package com.xxl.job.admin.service.impl;

import com.xxl.job.admin.config.XxlJobSsoProperties;
import com.xxl.job.admin.mapper.XxlJobUserMapper;
import com.xxl.job.admin.model.XxlJobUser;
import com.xxl.job.admin.service.SsoService;
import com.xxl.sso.core.helper.XxlSsoHelper;
import com.xxl.sso.core.model.LoginInfo;
import com.xxl.tool.id.UUIDTool;
import com.xxl.tool.json.GsonTool;
import com.xxl.tool.response.Response;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class SsoServiceImpl implements SsoService {

    private static final Logger logger = LoggerFactory.getLogger(SsoServiceImpl.class);

    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    private static final String TOKEN_SEPARATOR = ".";

    @Resource
    private XxlJobSsoProperties xxlJobSsoProperties;

    @Resource
    private XxlJobUserMapper xxlJobUserMapper;

    @Override
    public Response<SsoTokenInfo> validateToken(String token) {
        if (!xxlJobSsoProperties.isEnabled()) {
            return Response.ofFail("SSO is not enabled");
        }

        if (token == null || token.isEmpty()) {
            return Response.ofFail("Token is empty");
        }

        try {
            String[] parts = token.split("\\" + TOKEN_SEPARATOR);
            if (parts.length != 2) {
                return Response.ofFail("Invalid token format");
            }

            String payloadBase64 = parts[0];
            String signature = parts[1];

            String expectedSignature = calculateHmacSha256(payloadBase64, xxlJobSsoProperties.getSecret());
            if (!constantTimeEquals(signature, expectedSignature)) {
                logger.warn("SSO token signature validation failed");
                return Response.ofFail("Invalid token signature");
            }

            String payloadJson = new String(Base64.getUrlDecoder().decode(payloadBase64), StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = GsonTool.parseObject(payloadJson, HashMap.class);

            String phone = (String) payload.get("phone");
            Number timestampNum = (Number) payload.get("timestamp");
            String nonce = (String) payload.get("nonce");

            if (phone == null || phone.isEmpty()) {
                return Response.ofFail("Missing phone in token");
            }

            if (timestampNum == null) {
                return Response.ofFail("Missing timestamp in token");
            }

            long timestamp = timestampNum.longValue();
            long now = System.currentTimeMillis();
            long expireMs = xxlJobSsoProperties.getTokenExpireSeconds() * 1000;

            if (timestamp > now + 60000) {
                return Response.ofFail("Token timestamp is in the future");
            }

            if (now - timestamp > expireMs) {
                return Response.ofFail("Token has expired");
            }

            SsoTokenInfo tokenInfo = new SsoTokenInfo(phone, timestamp, nonce);
            return Response.ofSuccess(tokenInfo);

        } catch (Exception e) {
            logger.error("SSO token validation error", e);
            return Response.ofFail("Token validation error: " + e.getMessage());
        }
    }

    @Override
    public Response<Boolean> autoLogin(String phone) {
        if (phone == null || phone.isEmpty()) {
            return Response.ofFail("Phone is empty");
        }

        XxlJobUser user = xxlJobUserMapper.loadByUserName(phone);
        if (user == null) {
            logger.warn("SSO auto login failed: user not found for phone: {}", phone);
            return Response.ofFail("User not found");
        }

        try {
            LoginInfo loginInfo = new LoginInfo(String.valueOf(user.getId()), UUIDTool.getSimpleUUID());
            return Response.ofSuccess(true);
        } catch (Exception e) {
            logger.error("SSO auto login error", e);
            return Response.ofFail("Auto login error: " + e.getMessage());
        }
    }

    public Response<Boolean> doLoginWithResponse(String phone, HttpServletResponse response) {
        if (phone == null || phone.isEmpty()) {
            return Response.ofFail("Phone is empty");
        }

        XxlJobUser user = xxlJobUserMapper.loadByUserName(phone);
        if (user == null) {
            logger.warn("SSO login failed: user not found for phone: {}", phone);
            return Response.ofFail("User not found");
        }

        try {
            LoginInfo loginInfo = new LoginInfo(String.valueOf(user.getId()), UUIDTool.getSimpleUUID());
            Response<String> result = XxlSsoHelper.loginWithCookie(loginInfo, response, false);
            
            if (result.isSuccess()) {
                logger.info("SSO login success for user: {}", phone);
                return Response.ofSuccess(true);
            } else {
                logger.warn("SSO login failed: {}", result.getMsg());
                return Response.ofFail(result.getMsg());
            }
        } catch (Exception e) {
            logger.error("SSO login error", e);
            return Response.ofFail("Login error: " + e.getMessage());
        }
    }

    private String calculateHmacSha256(String data, String secret) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                HMAC_SHA256_ALGORITHM
        );
        Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
        mac.init(secretKeySpec);
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        
        if (aBytes.length != bBytes.length) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        return result == 0;
    }
}
