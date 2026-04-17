package com.xxl.job.admin.service;

import com.xxl.tool.response.Response;

/**
 * SSO单点登录服务接口
 * 
 * 定义了单点登录所需的核心功能：
 * - Token验证：验证来自企业运营后台的SSO Token
 * - 自动登录：根据手机号自动创建登录会话
 * 
 * 与企业运营后台的对接方式：
 * 1. 企业运营后台生成包含手机号、时间戳的Token
 * 2. 使用预共享密钥进行HMAC-SHA256签名
 * 3. 通过URL参数传递Token到xxl-job-admin
 * 4. xxl-job-admin验证Token后自动登录
 * 
 * @author xxl-job
 */
public interface SsoService {

    /**
     * 验证SSO Token的有效性
     * 
     * 验证流程：
     * 1. 检查Token格式是否正确（Base64Url(payload).HMAC-SHA256签名）
     * 2. 验证HMAC-SHA256签名是否匹配
     * 3. 解析Payload获取手机号、时间戳等信息
     * 4. 验证时间戳是否在有效期内（防止重放攻击）
     * 
     * @param token 来自企业运营后台的SSO Token
     * @return 验证结果，成功时返回SsoTokenInfo对象，失败时返回错误信息
     */
    Response<SsoTokenInfo> validateToken(String token);

    /**
     * 根据手机号自动登录
     * 
     * 注意：此方法仅验证用户是否存在，不实际创建登录会话。
     * 如需创建会话，请使用SsoServiceImpl.doLoginWithResponse()方法。
     * 
     * @param phone 用户手机号（与xxl_job_user表的username字段关联）
     * @return 操作结果，成功时返回true，失败时返回错误信息
     */
    Response<Boolean> autoLogin(String phone);

    /**
     * SSO Token解析后的数据结构
     * 
     * 对应Token Payload中的字段：
     * - phone: 用户手机号，作为与企业运营后台关联的标识
     * - timestamp: Token生成时间戳（毫秒），用于验证时效性
     * - nonce: 随机字符串（可选），用于防止重放攻击
     * 
     * @param phone 用户手机号
     * @param timestamp Token生成时间戳（毫秒）
     * @param nonce 随机字符串（可选）
     */
    record SsoTokenInfo(
        String phone,
        Long timestamp,
        String nonce
    ) {}
}
