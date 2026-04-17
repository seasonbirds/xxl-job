# xxl-job-admin 单点登录（SSO）设计文档

---

## 目录

1. [实现原理](#一实现原理)
2. [系统交互](#二系统交互)
3. [系统对接](#三系统对接)
4. [安全评估](#四安全评估)
5. [其他内容](#五其他内容)

---

## 一、实现原理

### 1.1 现有登录机制分析

xxl-job-admin当前使用 `xxl-sso-core` 框架实现登录功能：

- **用户认证**：通过用户名（手机号）和密码验证用户身份
- **会话管理**：使用 `XxlSsoHelper.loginWithCookie()` 创建登录会话
- **Token存储**：通过 `SimpleLoginStore` 将Token写入数据库 `xxl_job_user.token` 字段

### 1.2 单点登录方案设计

采用 **HMAC-SHA256签名验证 + 双Token机制** 实现企业运营后台与xxl-job-admin的单点登录。

#### 1.2.1 Token格式定义

**来自企业运营后台的SSO Token**：

```
[Base64Url编码的Payload].[HMAC-SHA256签名]
```

**Payload JSON结构**：
```json
{
  "phone": "13800138000",
  "timestamp": 1713340800000,
  "nonce": "optional-uuid"
}
```

#### 1.2.2 双Token机制

xxl-job-admin内部采用双Token机制提升安全性和用户体验：

| Token类型 | 有效期 | 存储位置 | 用途 | 安全措施 |
|-----------|--------|----------|------|----------|
| **Access Token** | 30分钟（默认） | 内存/上下文 | 访问受保护资源 | JWT格式，HMAC-SHA256签名 |
| **Refresh Token** | 7天（默认） | 加密Cookie | 刷新Access Token | AES-256-GCM加密，HttpOnly，Secure，SameSite |

#### 1.2.3 验证流程

```
1. 分割Token为[Payload, Signature]两部分
2. 使用预共享密钥计算期望签名
3. 常量时间比较签名是否一致（防止时序攻击）
4. Base64Url解码Payload，解析JSON
5. 验证时间戳是否在有效期内（默认5分钟）
6. 根据手机号查询用户
7. 创建xxl-sso会话
8. 生成双Token（Access Token + Refresh Token）
9. Refresh Token存入加密Cookie
```

#### 1.2.4 用户关联机制

两个系统通过手机号进行用户关联：

- 企业运营后台的用户标识：手机号
- xxl-job-admin的用户账号：`username` 字段（运营限制必须是手机号）

```
企业运营后台                          xxl-job-admin
      │                                    │
      │  生成Token: {phone, timestamp}     │
      │───────────────────────────────────>│
      │                                    │
      │                                    │  SELECT * FROM xxl_job_user 
      │                                    │  WHERE username = phone
      │                                    │
      │                                    │  自动创建登录会话
      │                                    │
```

---

## 二、系统交互

### 2.1 首次登录流程

```
┌──────────┐         ┌─────────────────┐         ┌─────────────────────┐
│   用户    │         │  企业运营后台      │         │    xxl-job-admin    │
└────┬─────┘         └────────┬────────┘         └──────────┬──────────┘
     │                        │                               │
     │──1. 点击"任务调度"链接─>│                               │
     │                        │                               │
     │                        │──2. 构造Payload              │
     │                        │   {phone, timestamp, nonce}  │
     │                        │                               │
     │                        │──3. Base64Url编码            │
     │                        │                               │
     │                        │──4. 计算HMAC-SHA256签名     │
     │                        │                               │
     │<──5. 302重定向─────────│                               │
     │  Location: /auth/sso/  │                               │
     │  login?token=xxx       │                               │
     │                        │                               │
     │──────────────────────────────────────────────────────>│
     │                        │                               │
     │                        │                               │──6. 验证SSO是否启用
     │                        │                               │
     │                        │                               │──7. 分割Token为[Payload,Signature]
     │                        │                               │
     │                        │                               │──8. 验证HMAC签名（常量时间比较）
     │                        │                               │
     │                        │                               │──9. 解码Payload，验证时间戳
     │                        │                               │
     │                        │                               │──10. 根据手机号查询用户
     │                        │                               │
     │                        │                               │──11. 创建xxl-sso会话
     │                        │                               │
     │                        │                               │──12. 生成Access Token + Refresh Token
     │                        │                               │
     │                        │                               │──13. Refresh Token存入加密Cookie
     │                        │                               │
     │<──────────────────────────────────────────────────────│
     │  302重定向到目标页面    │                               │
     │  Set-Cookie: xxl_job_rt│                               │
     │  (HttpOnly, Secure)    │                               │
     │                        │                               │
```

### 2.2 Token自动刷新流程

**问题**：原方案中Token过期直接重定向到企业运营后台，运营后台无法验证请求合法性。

**解决方案**：只有Refresh Token也失效时才重定向到企业运营后台，并携带State参数防CSRF。

```
用户访问xxl-job-admin
        │
        ▼
xxl-sso拦截器检查会话
        │
        ├───会话有效───> 正常访问
        │
        └───会话过期───> 重定向到 /auth/sso/redirect
                           │
                           ▼
                    检查Refresh Token Cookie
                           │
                           ├───存在且有效───> 自动刷新Access Token
                           │                    │
                           │                    ▼
                           │               生成新的Token对
                           │               (Refresh Token轮换)
                           │                    │
                           │                    ▼
                           │               重定向回原请求URL
                           │               (用户无感知)
                           │
                           └───不存在或失效───> 携带State参数重定向到企业运营后台
                                                   │
                                                   ▼
                                            企业运营后台验证State
                                                   │
                                                   ├───有效且用户已登录───> 重新生成SSO Token
                                                   │                          │
                                                   │                          ▼
                                                   │                    跳转回xxl-job-admin
                                                   │
                                                   └───无效或用户未登录───> 跳转到企业运营后台登录页
```

### 2.3 State参数机制

**State参数用途**：防止CSRF攻击，让企业运营后台可以验证请求合法性。

**State参数格式**：
```
random_string:timestamp:HMAC-SHA256签名
```

**State参数生成**：
```java
public static String generateState(String secret) {
    String random = generateSecureRandom(16);
    long timestamp = System.currentTimeMillis();
    String payload = random + ":" + timestamp;
    String signature = calculateHmacSha256(payload, secret);
    return payload + ":" + signature;
}
```

**企业运营后台验证State**：
```java
public static boolean validateState(String state, String secret, int expireSeconds) {
    String[] parts = state.split(":");
    if (parts.length != 3) return false;
    
    // 1. 验证HMAC签名
    String payload = parts[0] + ":" + parts[1];
    if (!verifyHmacSha256(payload, parts[2], secret)) {
        return false;
    }
    
    // 2. 验证时间戳有效期
    long timestamp = Long.parseLong(parts[1]);
    return System.currentTimeMillis() - timestamp <= expireSeconds * 1000L;
}
```

---

## 三、系统对接

### 3.1 对接前提

| 要求项 | 说明 |
|--------|------|
| 用户同步 | xxl-job-admin中的 `username` 必须是手机号格式 |
| 时间同步 | 两个系统的服务器时间误差不超过60秒 |
| 密钥一致 | 企业运营后台与xxl-job-admin配置相同的预共享密钥 |

### 3.2 xxl-job-admin配置

**配置文件**：`application.properties`

```properties
### 启用SSO
xxl.job.sso.enabled=true

### 预共享密钥（与企业运营后台一致）
### 要求：至少32个字符的强随机字符串
xxl.job.sso.secret=your-32-character-strong-secret-key

### 来自企业运营后台的SSO Token有效期（秒）
xxl.job.sso.token.expire.seconds=300

### 企业运营后台SSO入口URL
### 当Refresh Token失效时，重定向到此URL
### 格式示例：http://enterprise-host:port/enterprise/sso/xxl-job-redirect
xxl.job.sso.enterprise.sso.url=http://enterprise-host:port/enterprise/sso/xxl-job-redirect

### === 双Token配置 ===
### Access Token有效期（秒）- 默认30分钟
xxl.job.sso.access.token.expire.seconds=1800

### Refresh Token有效期（秒）- 默认7天
xxl.job.sso.refresh.token.expire.seconds=604800

### Refresh Token加密密钥（可选，Base64编码32字节）
### 生成方式：openssl rand -base64 32
xxl.job.sso.refresh.token.encrypt.key=

### xxl-sso原有配置（已预设）
xxl-sso.client.excluded.paths=/auth/sso/login,/auth/sso/redirect
xxl-sso.client.login.path=/auth/sso/redirect
```

### 3.3 企业运营后台配置

```properties
### xxl-job-admin地址
xxl.job.sso.admin.url=http://xxl-job-host:port/xxl-job-admin

### 预共享密钥（与xxl-job-admin一致）
xxl.job.sso.secret=your-32-character-strong-secret-key

### Token有效期（秒）
xxl.job.sso.token.expire.seconds=300
```

### 3.4 企业运营后台Token生成

**Token工具类**：

```java
public class XxlJobSsoTokenGenerator {

    private static final String HMAC_SHA256 = "HmacSHA256";

    /**
     * 生成SSO Token
     */
    public static String generateToken(String phone, String secret) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("phone", phone);
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("nonce", UUID.randomUUID().toString());

        String payloadJson = GsonTool.toJson(payload);
        String payloadBase64 = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

        String signature = calculateHmacSha256(payloadBase64, secret);
        return payloadBase64 + "." + signature;
    }

    /**
     * 构建SSO登录URL
     */
    public static String buildSsoLoginUrl(String phone, String secret, 
                                           String adminUrl, String redirectUrl) {
        String token = generateToken(phone, secret);
        
        return adminUrl + "/auth/sso/login" +
            "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8) +
            "&redirect_url=" + URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8);
    }

    /**
     * 计算HMAC-SHA256签名
     */
    private static String calculateHmacSha256(String data, String secret) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(keySpec);
            byte[] result = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(result);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 calculation failed", e);
        }
    }

    /**
     * 验证State参数（xxl-job-admin重定向时需要）
     */
    public static boolean validateState(String state, String secret, int expireSeconds) {
        if (state == null || state.isEmpty()) {
            return false;
        }
        
        String[] parts = state.split(":");
        if (parts.length != 3) {
            return false;
        }
        
        String payload = parts[0] + ":" + parts[1];
        String signature = parts[2];
        
        // 验证签名
        String expectedSignature = calculateHmacSha256(payload, secret);
        if (!constantTimeEquals(signature, expectedSignature)) {
            return false;
        }
        
        // 验证时间戳
        try {
            long timestamp = Long.parseLong(parts[1]);
            long now = System.currentTimeMillis();
            return now - timestamp <= expireSeconds * 1000L;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 常量时间比较（防止时序攻击）
     */
    private static boolean constantTimeEquals(String a, String b) {
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
```

**企业运营后台SSO入口控制器**：

```java
@Controller
@RequestMapping("/enterprise")
public class EnterpriseSsoController {

    @Value("${xxl.job.sso.secret}")
    private String ssoSecret;

    @Value("${xxl.job.sso.admin.url}")
    private String adminUrl;

    /**
     * 用户点击"任务调度"链接的入口
     */
    @GetMapping("/xxl-job-redirect")
    public RedirectView redirectToXxlJob(HttpServletRequest request) {
        String phone = getCurrentUserPhone(request);
        String redirectUrl = adminUrl + "/";
        String ssoUrl = XxlJobSsoTokenGenerator.buildSsoLoginUrl(
                phone, ssoSecret, adminUrl, redirectUrl
        );
        return new RedirectView(ssoUrl);
    }

    /**
     * 接收xxl-job-admin的重定向请求（Token过期时）
     */
    @GetMapping("/sso/xxl-job-redirect")
    public RedirectView xxlJobSsoRedirect(
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "redirect_url", required = false) String redirectUrl,
            HttpServletRequest request) {
        
        // 1. 验证State参数（防CSRF）
        if (state != null && !XxlJobSsoTokenGenerator.validateState(state, ssoSecret, 300)) {
            logger.warn("Invalid state parameter");
            return new RedirectView("/login");
        }
        
        // 2. 检查用户登录状态
        if (!isUserLoggedIn(request)) {
            // 用户未登录，跳转到登录页
            String loginRedirect = "/login?redirect=" + 
                URLEncoder.encode(request.getRequestURL().toString(), "UTF-8");
            return new RedirectView(loginRedirect);
        }
        
        // 3. 用户已登录，生成SSO Token并跳转回xxl-job-admin
        String phone = getCurrentUserPhone(request);
        String targetUrl = (redirectUrl != null && !redirectUrl.isEmpty()) 
            ? redirectUrl 
            : adminUrl + "/";
        
        String ssoUrl = XxlJobSsoTokenGenerator.buildSsoLoginUrl(
                phone, ssoSecret, adminUrl, targetUrl
        );
        
        return new RedirectView(ssoUrl);
    }
}
```

### 3.5 端点清单

| 端点 | 方法 | 所属系统 | 说明 |
|------|------|----------|------|
| `/auth/sso/login` | GET | xxl-job-admin | SSO登录入口，接收企业运营后台的Token |
| `/auth/sso/redirect` | GET | xxl-job-admin | Token过期重定向端点（xxl-sso配置） |
| `/enterprise/sso/xxl-job-redirect` | GET | 企业运营后台 | SSO入口，接收xxl-job-admin的重定向 |

---

## 四、安全评估

### 4.1 安全机制

| 安全项 | 实现方案 | 说明 |
|--------|----------|------|
| 完整性保护 | HMAC-SHA256签名 | 防止Token被篡改 |
| 时效性保护 | 时间戳验证 | Token必须在有效期内使用 |
| 重放攻击防护 | 短有效期 + 时间戳 | 过期Token无法使用 |
| 时序攻击防护 | 常量时间比较 | 防止通过响应时间推断正确签名 |
| Refresh Token加密 | AES-256-GCM | 带认证标签的加密模式 |
| Cookie安全 | HttpOnly + Secure + SameSite | 防止XSS和CSRF |
| 跨系统请求验证 | State参数 | 防止CSRF，企业运营后台可验证请求合法性 |

### 4.2 风险分析

#### 风险1：Token泄露

**场景**：Token在URL中传递可能被日志记录或网络嗅探。

**缓解措施**：

| 措施 | 说明 |
|------|------|
| 短有效期 | 来自企业运营后台的Token默认5分钟过期 |
| HTTPS强制 | 生产环境必须使用HTTPS |
| 日志保护 | 不记录完整的Token |

#### 风险2：重放攻击

**场景**：攻击者截获有效的Token后重复使用。

**缓解措施**：

| 措施 | 说明 |
|------|------|
| 时间戳验证 | 验证timestamp字段 |
| 短有效期 | 5分钟有效期限制攻击窗口 |
| Nonce可选 | 可实现Nonce缓存机制 |

#### 风险3：密钥泄露

**场景**：预共享密钥被泄露，攻击者可以伪造任意用户的Token。

**缓解措施**：

| 措施 | 说明 |
|------|------|
| 强密钥 | 至少32个随机字符 |
| 环境变量 | 密钥不硬编码，通过环境变量注入 |
| 定期轮换 | 建议每3个月更换一次密钥 |

#### 风险4：CSRF攻击（重定向时）

**场景**：攻击者构造恶意链接诱导用户点击：
```
http://xxl-job-admin/auth/sso/redirect?redirect_url=http://evil.com
```

**缓解措施**：

| 措施 | 说明 |
|------|------|
| State参数 | 携带HMAC签名和时间戳的State参数 |
| 企业运营后台验证 | 企业运营后台必须验证State参数有效性 |
| 同源检查 | redirect_url只允许同源URL |

### 4.3 安全对比

| 维度 | 原方案 | 改进方案 |
|------|--------|----------|
| Token过期处理 | 直接重定向到企业运营后台 | 先尝试Refresh Token自动刷新，失败才重定向 |
| 重定向安全性 | 无验证参数，运营后台无法判断请求合法性 | 携带State参数（签名+时间戳），运营后台可验证 |
| 用户体验 | 每次过期都需跳转 | Refresh Token有效期内自动刷新，用户无感知 |
| Token存储 | 单Token，长期有效 | 双Token，Access Token短期，Refresh Token加密存储 |

### 4.4 生产环境强制要求

- [ ] **HTTPS**：所有SSO相关通信必须使用HTTPS
- [ ] **强密钥**：预共享密钥至少32个随机字符
- [ ] **环境变量**：密钥通过环境变量注入，不硬编码
- [ ] **日志保护**：日志中不记录完整的Token
- [ ] **State验证**：企业运营后台必须验证State参数

---

## 五、其他内容

### 5.1 新增文件清单

| 文件路径 | 说明 |
|----------|------|
| `service/token/TokenInfo.java` | Token信息基类 |
| `service/token/AccessTokenInfo.java` | Access Token信息类 |
| `service/token/RefreshTokenInfo.java` | Refresh Token信息类 |
| `service/token/TokenService.java` | Token服务接口 |
| `service/token/TokenServiceImpl.java` | Token服务实现 |
| `util/CryptoUtil.java` | 加密工具类（AES-256-GCM、HMAC-SHA256、常量时间比较） |
| `util/TokenCookieUtil.java` | Token Cookie工具类 |

### 5.2 更新文件清单

| 文件路径 | 更新内容 |
|----------|----------|
| `config/XxlJobSsoProperties.java` | 新增双Token配置项 |
| `controller/base/SsoLoginController.java` | 支持自动刷新、State参数 |
| `resources/application.properties` | 新增双Token配置 |

### 5.3 测试清单

#### 功能测试

- [ ] 企业运营后台点击链接可自动登录
- [ ] Access Token过期时自动刷新
- [ ] Refresh Token过期时携带State参数重定向
- [ ] 企业运营后台验证State参数

#### 安全测试

- [ ] 无效State参数被拒绝
- [ ] 过期Token无法使用
- [ ] 签名篡改后验证失败
- [ ] 外部redirect_url被拦截

### 5.4 回滚方案

如需快速禁用SSO功能：

```properties
xxl.job.sso.enabled=false
```

禁用后：
- 用户通过传统的用户名密码方式登录
- SSO端点将拒绝访问，重定向到登录页面

---

**文档版本**：v4.0  
**最后更新**：2026-04-17
