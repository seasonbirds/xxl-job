package com.xxl.job.admin.util;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * 加密工具类
 * 
 * 提供AES-256-GCM加密/解密和HMAC-SHA256签名功能。
 * 
 * 安全特性：
 * 1. AES-256-GCM：带认证的加密模式，提供完整性和机密性
 * 2. HMAC-SHA256：消息认证码，防篡改
 * 3. 常量时间比较：防止时序攻击
 * 
 * @author xxl-job
 */
public final class CryptoUtil {

    private static final String AES_GCM_ALGORITHM = "AES/GCM/NoPadding";
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
    
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int AES_KEY_LENGTH = 32;

    private CryptoUtil() {
    }

    /**
     * 生成AES-256密钥
     * 
     * @return Base64编码的密钥
     */
    public static String generateAesKey() {
        byte[] key = new byte[AES_KEY_LENGTH];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    /**
     * AES-256-GCM加密
     * 
     * 加密格式：[IV(12字节)][密文][认证标签(16字节)]
     * 
     * @param plaintext 明文
     * @param secretKeyBase64 Base64编码的密钥（必须32字节）
     * @return Base64编码的加密结果
     */
    public static String encryptAesGcm(String plaintext, String secretKeyBase64) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        
        try {
            byte[] key = Base64.getDecoder().decode(secretKeyBase64);
            if (key.length != AES_KEY_LENGTH) {
                throw new IllegalArgumentException("AES key must be 32 bytes");
            }
            
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            
            Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
            
            byte[] ciphertextWithTag = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            
            byte[] result = new byte[iv.length + ciphertextWithTag.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(ciphertextWithTag, 0, result, iv.length, ciphertextWithTag.length);
            
            return Base64.getUrlEncoder().withoutPadding().encodeToString(result);
            
        } catch (Exception e) {
            throw new RuntimeException("AES-GCM encryption failed", e);
        }
    }

    /**
     * AES-256-GCM解密
     * 
     * @param ciphertextBase64 Base64编码的加密结果
     * @param secretKeyBase64 Base64编码的密钥
     * @return 明文
     */
    public static String decryptAesGcm(String ciphertextBase64, String secretKeyBase64) {
        if (ciphertextBase64 == null || ciphertextBase64.isEmpty()) {
            return ciphertextBase64;
        }
        
        try {
            byte[] key = Base64.getDecoder().decode(secretKeyBase64);
            if (key.length != AES_KEY_LENGTH) {
                throw new IllegalArgumentException("AES key must be 32 bytes");
            }
            
            byte[] encrypted = Base64.getUrlDecoder().decode(ciphertextBase64);
            
            byte[] iv = Arrays.copyOfRange(encrypted, 0, GCM_IV_LENGTH);
            byte[] ciphertextWithTag = Arrays.copyOfRange(encrypted, GCM_IV_LENGTH, encrypted.length);
            
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            
            Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            
            byte[] plaintext = cipher.doFinal(ciphertextWithTag);
            return new String(plaintext, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            throw new RuntimeException("AES-GCM decryption failed", e);
        }
    }

    /**
     * 计算HMAC-SHA256签名
     * 
     * @param data 待签名的数据
     * @param secret 密钥
     * @return Base64Url编码的签名
     */
    public static String calculateHmacSha256(String data, String secret) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256_ALGORITHM
            );
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 calculation failed", e);
        }
    }

    /**
     * 验证HMAC-SHA256签名
     * 
     * 使用常量时间比较，防止时序攻击。
     * 
     * @param data 原始数据
     * @param signature 待验证的签名
     * @param secret 密钥
     * @return true表示签名有效
     */
    public static boolean verifyHmacSha256(String data, String signature, String secret) {
        if (data == null || signature == null) {
            return false;
        }
        
        String expectedSignature = calculateHmacSha256(data, secret);
        return constantTimeEquals(signature, expectedSignature);
    }

    /**
     * 常量时间字符串比较
     * 
     * 防止时序攻击（Timing Attack）。
     * 普通的字符串比较会在发现第一个不匹配的字符时立即返回，
     * 攻击者可以通过测量响应时间来推断正确的字符。
     * 
     * 此方法会遍历所有字符进行异或运算，无论是否匹配都执行相同数量的操作。
     * 
     * @param a 第一个字符串
     * @param b 第二个字符串
     * @return true表示两个字符串相等
     */
    public static boolean constantTimeEquals(String a, String b) {
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

    /**
     * 生成安全的随机字符串
     * 
     * @param length 字符串长度
     * @return 随机字符串
     */
    public static String generateSecureRandom(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.substring(0, length);
    }

    /**
     * 生成State参数（用于防止CSRF攻击）
     * 
     * State参数包含：
     * 1. 随机字符串（防猜测）
     * 2. 时间戳（防重放）
     * 3. HMAC签名（防篡改）
     * 
     * @param secret 密钥
     * @return State参数
     */
    public static String generateState(String secret) {
        String random = generateSecureRandom(16);
        long timestamp = System.currentTimeMillis();
        String payload = random + ":" + timestamp;
        String signature = calculateHmacSha256(payload, secret);
        return payload + ":" + signature;
    }

    /**
     * 验证State参数
     * 
     * @param state State参数
     * @param secret 密钥
     * @param expireSeconds 有效期（秒）
     * @return true表示State有效
     */
    public static boolean validateState(String state, String secret, int expireSeconds) {
        if (state == null || state.isEmpty()) {
            return false;
        }
        
        String[] parts = state.split(":");
        if (parts.length != 3) {
            return false;
        }
        
        String random = parts[0];
        String timestampStr = parts[1];
        String signature = parts[2];
        
        String payload = random + ":" + timestampStr;
        if (!verifyHmacSha256(payload, signature, secret)) {
            return false;
        }
        
        try {
            long timestamp = Long.parseLong(timestampStr);
            long now = System.currentTimeMillis();
            return now - timestamp <= expireSeconds * 1000L;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
