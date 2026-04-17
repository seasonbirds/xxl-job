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
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * SSO单点登录服务实现类
 * 
 * 实现了基于HMAC-SHA256签名的Token验证机制，
 * 用于与企业运营后台进行单点登录对接。
 * 
 * Token格式说明：
 * Token由两部分组成，用"."分隔：
 * [Base64Url编码的Payload].[HMAC-SHA256签名]
 * 
 * Payload JSON结构：
 * {
 *   "phone": "13800138000",      // 用户手机号
 *   "timestamp": 1713340800000,   // 生成时间戳（毫秒）
 *   "nonce": "optional-uuid"       // 随机字符串（可选）
 * }
 * 
 * 安全机制：
 * 1. HMAC-SHA256签名验证：防止Token被篡改
 * 2. 时间戳验证：防止重放攻击
 * 3. 常量时间比较：防止时序攻击
 * 
 * @author xxl-job
 */
@Service
public class SsoServiceImpl implements SsoService {

    private static final Logger logger = LoggerFactory.getLogger(SsoServiceImpl.class);

    /**
     * HMAC签名算法：HmacSHA256
     */
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    /**
     * Token各部分的分隔符
     */
    private static final String TOKEN_SEPARATOR = "\\.";

    @Resource
    private XxlJobSsoProperties xxlJobSsoProperties;

    @Resource
    private XxlJobUserMapper xxlJobUserMapper;

    /**
     * 验证SSO Token的有效性
     * 
     * 验证流程：
     * 1. 检查SSO功能是否已启用
     * 2. 检查Token是否为空
     * 3. 解析Token为Payload和签名两部分
     * 4. 使用预共享密钥验证HMAC-SHA256签名
     * 5. Base64Url解码Payload并解析为JSON
     * 6. 验证必填字段（phone、timestamp）
     * 7. 验证时间戳是否在有效期内
     * 
     * @param token 来自企业运营后台的SSO Token
     * @return 验证结果，成功时返回SsoTokenInfo对象
     */
    @Override
    public Response<SsoTokenInfo> validateToken(String token) {
        if (!xxlJobSsoProperties.isEnabled()) {
            return Response.ofFail("SSO is not enabled");
        }

        if (token == null || token.isEmpty()) {
            return Response.ofFail("Token is empty");
        }

        try {
            String[] parts = token.split(TOKEN_SEPARATOR);
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
            
            Type mapType = new com.google.gson.reflect.TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> payload = GsonTool.fromJson(payloadJson, mapType);
            if (payload == null) {
                payload = new HashMap<>();
            }

            String phone = (String) payload.get("phone");
            Object timestampObj = payload.get("timestamp");
            String nonce = (String) payload.get("nonce");

            if (phone == null || phone.isEmpty()) {
                return Response.ofFail("Missing phone in token");
            }

            if (timestampObj == null) {
                return Response.ofFail("Missing timestamp in token");
            }

            long timestamp;
            if (timestampObj instanceof Number) {
                timestamp = ((Number) timestampObj).longValue();
            } else if (timestampObj instanceof String) {
                timestamp = Long.parseLong((String) timestampObj);
            } else {
                return Response.ofFail("Invalid timestamp format");
            }

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

    /**
     * 根据手机号自动登录（接口实现，仅验证用户存在）
     * 
     * 注意：此方法仅验证用户是否存在于数据库中，
     * 不实际创建登录会话。如需创建会话，请使用
     * doLoginWithResponse()方法。
     * 
     * @param phone 用户手机号
     * @return 验证结果
     */
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

        return Response.ofSuccess(true);
    }

    /**
     * 执行SSO登录并创建会话
     * 
     * 此方法完成以下操作：
     * 1. 根据手机号查询用户信息
     * 2. 创建LoginInfo对象（包含用户ID和随机Token）
     * 3. 调用XxlSsoHelper.loginWithCookie()创建登录会话
     * 4. 将会话信息写入Cookie和数据库
     * 
     * @param phone 用户手机号（与xxl_job_user.username关联）
     * @param response HTTP响应对象，用于写入Cookie
     * @return 登录结果，成功时返回true
     */
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

    /**
     * 计算HMAC-SHA256签名
     * 
     * 使用预共享密钥对数据进行HMAC-SHA256签名，
     * 用于验证Token的完整性和真实性。
     * 
     * 签名流程：
     * 1. 使用密钥创建HMAC-SHA256 Mac实例
     * 2. 计算数据的HMAC值
     * 3. 使用Base64Url编码（无填充）转换为字符串
     * 
     * @param data 待签名的数据（Base64Url编码的Payload）
     * @param secret 预共享密钥
     * @return Base64Url编码的HMAC-SHA256签名
     * @throws Exception 当签名计算失败时抛出异常
     */
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

    /**
     * 常量时间字符串比较
     * 
     * 使用常量时间比较两个字符串，防止时序攻击（Timing Attack）。
     * 
     * 安全原理：
     * 普通的字符串比较（如String.equals()）通常在发现第一个
     * 不匹配的字符时就返回结果，攻击者可以通过测量响应时间
     * 推断出正确的字符。
     * 
     * 常量时间比较会遍历所有字符进行异或运算，无论是否匹配
     * 都执行相同数量的操作，从而防止时序攻击。
     * 
     * @param a 第一个字符串
     * @param b 第二个字符串
     * @return true表示两个字符串相等，false表示不相等
     */
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
