# xxl-job-admin 单点登录（SSO）设计文档

---

## 目录

1. [背景与目标](#一背景与目标)
2. [实现原理](#二实现原理)
3. [系统交互](#三系统交互)
4. [系统对接](#四系统对接)
5. [安全评估](#五安全评估)
6. [实现方案](#六实现方案)
7. [测试方案](#七测试方案)
8. [部署与运维](#八部署与运维)
9. [附录](#九附录)

---

## 一、背景与目标

### 1.1 业务背景

xxl-job-admin作为企业运营后台的一个子系统，需要实现与企业运营后台的单点登录功能。用户在企业运营后台通过点击链接即可自动登录到xxl-job-admin管理后台，无需再次输入用户名和密码。

### 1.2 关联机制

| 系统 | 用户标识 | 关联方式 |
|------|----------|----------|
| 企业运营后台 | 手机号 | 作为用户唯一标识 |
| xxl-job-admin | username | 运营限制添加的用户账号必须是手机号 |

两个系统通过 **手机号 = username** 进行用户关联。

### 1.3 设计目标

1. **安全性**：防止Token篡改、重放攻击、时序攻击
2. **易用性**：企业运营后台只需生成Token并跳转，无需复杂对接
3. **兼容性**：不影响原有的用户名密码登录方式
4. **可维护性**：配置化管理，支持热切换（启用/禁用）

---

## 二、实现原理

### 2.1 现有登录机制分析

xxl-job-admin当前使用 `xxl-sso-core` 框架实现登录功能，核心机制如下：

#### 2.1.1 登录流程

```
用户输入用户名密码
       ↓
XxlJobUserMapper.loadByUserName() 查询用户
       ↓
Sha256Tool.sha256() 验证密码
       ↓
XxlSsoHelper.loginWithCookie() 创建登录会话
       ↓
SimpleLoginStore.set() 将Token写入数据库
       ↓
返回登录成功Cookie
```

#### 2.1.2 关键组件

| 组件 | 路径 | 职责 |
|------|------|------|
| `LoginController` | `controller/base/` | 处理登录请求 |
| `XxlSsoHelper` | `xxl-sso-core` | SSO核心工具类 |
| `SimpleLoginStore` | `web/xxlsso/` | 登录状态存储（数据库） |
| `XxlJobUser` | `model/` | 用户实体类 |

#### 2.1.3 会话管理

- **存储方式**：Token存储在 `xxl_job_user.token` 字段
- **Cookie名称**：由 `xxl-sso.token.key` 配置，默认为 `xxl_job_login_token`
- **有效期**：由 `xxl-sso.token.timeout` 配置，默认为7天

### 2.2 单点登录方案设计

#### 2.2.1 方案选型

| 方案 | 优点 | 缺点 | 推荐度 |
|------|------|------|--------|
| JWT签名验证 | 无状态、易扩展、安全 | 需要额外实现 | ⭐⭐⭐⭐⭐ |
| 共享Session | 简单直接 | 需要共享存储、耦合度高 | ⭐⭐ |
| OAuth2.0 | 标准协议、功能完善 | 实现复杂、对接成本高 | ⭐⭐⭐ |

**选择方案**：基于HMAC-SHA256签名的Token验证方案

#### 2.2.2 核心原理

```
┌─────────────────────────────────────────────────────────────┐
│                    企业运营后台                                │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ 1. 构造Payload JSON:                                  │    │
│  │    {                                                   │    │
│  │      "phone": "13800138000",                         │    │
│  │      "timestamp": 1713340800000,                     │    │
│  │      "nonce": "optional-uuid"                         │    │
│  │    }                                                   │    │
│  └─────────────────────────────────────────────────────┘    │
│                           ↓                                   │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ 2. Base64Url编码Payload                              │    │
│  └─────────────────────────────────────────────────────┘    │
│                           ↓                                   │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ 3. 使用预共享密钥计算HMAC-SHA256签名                 │    │
│  │    signature = HMAC-SHA256(payloadBase64, secret)  │    │
│  └─────────────────────────────────────────────────────┘    │
│                           ↓                                   │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ 4. 拼接Token:                                         │    │
│  │    token = payloadBase64 + "." + signature          │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                           ↓
              重定向到xxl-job-admin
                           ↓
┌─────────────────────────────────────────────────────────────┐
│                     xxl-job-admin                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ 1. 分割Token为Payload和Signature                     │    │
│  └─────────────────────────────────────────────────────┘    │
│                           ↓                                   │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ 2. 使用相同密钥计算期望签名                           │    │
│  └─────────────────────────────────────────────────────┘    │
│                           ↓                                   │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ 3. 常量时间比较签名是否一致（防止时序攻击）           │    │
│  └─────────────────────────────────────────────────────┘    │
│                           ↓                                   │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ 4. Base64Url解码Payload，验证时间戳                  │    │
│  └─────────────────────────────────────────────────────┘    │
│                           ↓                                   │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ 5. 根据手机号查询用户，自动创建登录会话               │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

#### 2.2.3 Token格式定义

**Token结构**：
```
[Base64Url编码的Payload].[HMAC-SHA256签名]
```

**分隔符**：`.` (点号)

**Payload JSON结构**：
```json
{
  "phone": "13800138000",
  "timestamp": 1713340800000,
  "nonce": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Payload字段说明**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `phone` | String | 是 | 用户手机号，作为与企业运营后台关联的唯一标识 |
| `timestamp` | Long | 是 | Token生成时间戳（毫秒），用于验证时效性 |
| `nonce` | String | 否 | 随机字符串，可用于防止重放攻击 |

**签名算法**：HMAC-SHA256

**签名输入**：Base64Url编码的Payload字符串

**签名输出**：Base64Url编码（无填充）的HMAC-SHA256值

---

## 三、系统交互

### 3.1 交互流程图

```
┌──────────┐         ┌─────────────────┐         ┌─────────────────────┐
│   用户    │         │  企业运营后台      │         │    xxl-job-admin    │
└────┬─────┘         └────────┬────────┘         └──────────┬──────────┘
     │                        │                               │
     │──1. 点击"任务调度"链接─>│                               │
     │                        │                               │
     │                        │──2. 构造Payload               │
     │                        │   {phone, timestamp, nonce}  │
     │                        │                               │
     │                        │──3. Base64Url编码             │
     │                        │                               │
     │                        │──4. 计算HMAC-SHA256签名      │
     │                        │                               │
     │                        │──5. 拼接Token                 │
     │                        │                               │
     │<──6. 302重定向─────────│                               │
     │   Location: /xxl-job-  │                               │
     │   admin/auth/sso/login │                               │
     │   ?token=xxx&redirect_ │                               │
     │   url=/xxl-job-admin/  │                               │
     │                        │                               │
     │──────────────────────────────────7. 携带Token访问────>│
     │                        │                               │
     │                        │                               │──8. 验证SSO是否启用
     │                        │                               │
     │                        │                               │──9. 分割Token
     │                        │                               │
     │                        │                               │──10. 验证HMAC签名
     │                        │                               │
     │                        │                               │──11. 解码Payload
     │                        │                               │
     │                        │                               │──12. 验证时间戳
     │                        │                               │
     │                        │                               │──13. 根据手机号查询用户
     │                        │                               │
     │                        │                               │──14. 创建登录会话
     │                        │                               │    (写入Cookie和数据库)
     │                        │                               │
     │<──────────────────────────────────15. 302重定向──────│
     │   Location: /xxl-job-  │                               │
     │   admin/               │                               │
     │   Set-Cookie: xxl_job_ │                               │
     │   login_token=xxx     │                               │
     │                        │                               │
     │──────────────────────────────────16. 访问首页────────>│
     │                        │                               │
     │<──────────────────────────────────17. 返回首页───────│
     │   (已登录状态)          │                               │
     │                        │                               │
```

### 3.2 时序图

```
用户          企业运营后台              xxl-job-admin
 │                 │                          │
 │──点击链接──────>│                          │
 │                 │                          │
 │                 │──构造Payload             │
 │                 │                          │
 │                 │──Base64Url编码           │
 │                 │                          │
 │                 │──计算HMAC签名            │
 │                 │                          │
 │                 │──拼接Token               │
 │                 │                          │
 │<──302重定向─────│                          │
 │                 │                          │
 │──────────────────────────────────────────>│
 │                 │                          │
 │                 │                          │──检查SSO启用
 │                 │                          │
 │                 │                          │──分割Token
 │                 │                          │
 │                 │                          │──验证签名
 │                 │                          │
 │                 │                          │──解码Payload
 │                 │                          │
 │                 │                          │──验证时间戳
 │                 │                          │
 │                 │                          │──查询用户
 │                 │                          │
 │                 │                          │──创建会话
 │                 │                          │
 │<──────────────────────────────────────────│
 │                 │                          │
 │──────────────────────────────────────────>│
 │                 │                          │
 │<──────────────────────────────────────────│
 │                 │                          │
```

### 3.3 详细交互步骤

#### 步骤1：用户点击链接

用户在企业运营后台点击"任务调度中心"或类似链接，触发SSO登录流程。

#### 步骤2：企业运营后台生成Token

企业运营后台执行以下操作：

```java
// 1. 构造Payload
Map<String, Object> payload = new HashMap<>();
payload.put("phone", currentUserPhone);
payload.put("timestamp", System.currentTimeMillis());
payload.put("nonce", UUID.randomUUID().toString());

// 2. 序列化为JSON
String payloadJson = GsonTool.toJson(payload);

// 3. Base64Url编码
String payloadBase64 = Base64.getUrlEncoder()
    .withoutPadding()
    .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

// 4. 计算HMAC-SHA256签名
String signature = calculateHmacSha256(payloadBase64, SECRET_KEY);

// 5. 拼接Token
String token = payloadBase64 + "." + signature;
```

#### 步骤3：重定向到xxl-job-admin

```
HTTP/1.1 302 Found
Location: http://xxl-job-host:port/xxl-job-admin/auth/sso/login
          ?token=eyJwaG9uZSI6IjEzOD...
          &redirect_url=%2Fxxl-job-admin%2F
```

#### 步骤4：xxl-job-admin验证Token

```java
// 1. 检查SSO是否启用
if (!xxlJobSsoProperties.isEnabled()) {
    return redirectToLoginPage();
}

// 2. 分割Token
String[] parts = token.split("\\.");
if (parts.length != 2) {
    return redirectToLoginPage();
}

// 3. 验证签名
String expectedSignature = calculateHmacSha256(parts[0], secret);
if (!constantTimeEquals(parts[1], expectedSignature)) {
    return redirectToLoginPage();
}

// 4. 解码Payload
String payloadJson = new String(
    Base64.getUrlDecoder().decode(parts[0]),
    StandardCharsets.UTF_8
);

// 5. 验证时间戳
long timestamp = (Long) payload.get("timestamp");
long now = System.currentTimeMillis();
if (now - timestamp > expireMs) {
    return redirectToLoginPage();
}
```

#### 步骤5：自动登录

```java
// 1. 根据手机号查询用户
XxlJobUser user = xxlJobUserMapper.loadByUserName(phone);
if (user == null) {
    return redirectToLoginPage();
}

// 2. 创建登录会话
LoginInfo loginInfo = new LoginInfo(
    String.valueOf(user.getId()),
    UUIDTool.getSimpleUUID()
);
XxlSsoHelper.loginWithCookie(loginInfo, response, false);
```

#### 步骤6：重定向到目标页面

```
HTTP/1.1 302 Found
Location: /xxl-job-admin/
Set-Cookie: xxl_job_login_token=xxx; Path=/xxl-job-admin; HttpOnly
```

### 3.4 错误处理流程

```
Token验证失败
      │
      ├───> SSO未启用 ───────────> 重定向到登录页
      │
      ├───> Token为空 ───────────> 重定向到登录页
      │
      ├───> 格式错误 ────────────> 重定向到登录页
      │
      ├───> 签名验证失败 ─────────> 重定向到登录页
      │
      ├───> Payload解析失败 ─────> 重定向到登录页
      │
      ├───> 缺少必填字段 ────────> 重定向到登录页
      │
      ├───> Token已过期 ─────────> 重定向到登录页
      │
      └───> 时间戳在未来 ────────> 重定向到登录页

用户验证失败
      │
      └───> 用户不存在 ──────────> 重定向到登录页
```

---

## 四、系统对接

### 4.1 对接前提

#### 4.1.1 用户同步

两个系统的用户账号必须保持一致：

| 要求项 | 说明 |
|--------|------|
| 账号格式 | xxl-job-admin中的 `username` 必须是手机号 |
| 同步机制 | 建议通过企业运营后台统一管理用户，同步到xxl-job-admin |
| 新增用户 | 企业运营后台新增用户时，同步在xxl-job-admin创建对应用户 |

#### 4.1.2 时间同步

两个系统的服务器时间必须保持同步：

| 要求项 | 说明 |
|--------|------|
| 允许误差 | 建议不超过60秒 |
| 同步方式 | 使用NTP服务同步时间 |
| 影响 | 时间误差超过Token有效期将导致验证失败 |

### 4.2 配置参数

#### 4.2.1 xxl-job-admin配置

**配置文件**：`application.properties`

```properties
### xxl-job-admin SSO (单点登录配置)

# 是否启用SSO功能
# true: 启用，支持单点登录
# false: 禁用，仅支持传统用户名密码登录
xxl.job.sso.enabled=false

# 预共享密钥
# 企业运营后台与xxl-job-admin必须配置相同的密钥
# 密钥要求：至少32个字符，使用强随机字符串
xxl.job.sso.secret=please-change-this-to-a-strong-secret-key-at-least-32-characters

# Token有效期（秒）
# 默认值：300秒（5分钟）
# 用于防止重放攻击，Token必须在有效期内使用
xxl.job.sso.token.expire.seconds=300

# 登录失败跳转URL
# SSO验证失败时跳转的页面
# 默认值：/auth/login（传统登录页面）
xxl.job.sso.login.failure.url=/auth/login

### xxl-sso 原有配置（需排除SSO路径）

# 排除SSO登录路径，不被xxl-sso拦截器拦截
xxl-sso.client.excluded.paths=/auth/sso/login
```

#### 4.2.2 企业运营后台配置

```properties
### xxl-job-admin SSO对接配置

# xxl-job-admin地址
xxl.job.sso.admin.url=http://localhost:8080/xxl-job-admin

# SSO登录端点
xxl.job.sso.login.path=/auth/sso/login

# 预共享密钥（与xxl-job-admin一致）
xxl.job.sso.secret=your-32-character-strong-secret-key

# Token有效期（秒，与xxl-job-admin一致）
xxl.job.sso.token.expire.seconds=300
```

### 4.3 Token生成示例

#### 4.3.1 Java实现

```java
import com.xxl.tool.json.GsonTool;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * xxl-job-admin SSO Token生成工具类
 * 供企业运营后台使用
 */
public class XxlJobSsoTokenGenerator {

    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    /**
     * 生成SSO Token
     * 
     * @param phone 用户手机号
     * @param secret 预共享密钥
     * @param expireSeconds Token有效期（秒）
     * @return SSO Token字符串
     */
    public static String generateToken(String phone, String secret, int expireSeconds) {
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
     * 
     * @param phone 用户手机号
     * @param secret 预共享密钥
     * @param adminUrl xxl-job-admin地址，如 http://localhost:8080/xxl-job-admin
     * @param redirectUrl 登录成功后跳转的目标URL
     * @return 完整的SSO登录URL
     */
    public static String buildSsoLoginUrl(String phone, String secret, 
                                           String adminUrl, String redirectUrl) {
        String token = generateToken(phone, secret, 300);
        
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(adminUrl);
        urlBuilder.append("/auth/sso/login?token=");
        urlBuilder.append(java.net.URLEncoder.encode(token, StandardCharsets.UTF_8));
        
        if (redirectUrl != null && !redirectUrl.isEmpty()) {
            urlBuilder.append("&redirect_url=");
            urlBuilder.append(java.net.URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8));
        }
        
        return urlBuilder.toString();
    }

    /**
     * 计算HMAC-SHA256签名
     */
    private static String calculateHmacSha256(String data, String secret) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256_ALGORITHM
            );
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate HMAC-SHA256 signature", e);
        }
    }
}
```

#### 4.3.2 使用示例

```java
@Controller
@RequestMapping("/enterprise")
public class EnterpriseController {

    @Value("${xxl.job.sso.secret}")
    private String ssoSecret;

    @Value("${xxl.job.sso.admin.url}")
    private String adminUrl;

    /**
     * 跳转到xxl-job-admin（SSO登录）
     */
    @GetMapping("/xxl-job-redirect")
    public RedirectView redirectToXxlJobAdmin(HttpServletRequest request) {
        String currentUserPhone = getCurrentUserPhone(request);
        
        String redirectUrl = adminUrl + "/";
        String ssoUrl = XxlJobSsoTokenGenerator.buildSsoLoginUrl(
            currentUserPhone,
            ssoSecret,
            adminUrl,
            redirectUrl
        );
        
        return new RedirectView(ssoUrl);
    }
}
```

### 4.4 对接步骤

#### 步骤1：配置xxl-job-admin

1. 修改 `application.properties`，添加SSO配置
2. 设置 `xxl.job.sso.enabled=true`
3. 配置预共享密钥 `xxl.job.sso.secret`
4. 确保 `xxl-sso.client.excluded.paths` 包含 `/auth/sso/login`

#### 步骤2：配置企业运营后台

1. 添加相同的预共享密钥配置
2. 实现Token生成工具类（参考4.3.1）
3. 创建跳转入口接口

#### 步骤3：用户同步

1. 确保xxl-job-admin中已存在对应用户
2. 用户账号（username）必须是手机号格式
3. 建议配置用户同步机制

#### 步骤4：测试验证

1. 企业运营后台生成Token并跳转
2. 验证是否自动登录到xxl-job-admin
3. 验证各种错误场景的处理

### 4.5 完整对接示例

#### 企业运营后台前端

```html
<!-- 企业运营后台页面中的链接 -->
<a href="/enterprise/xxl-job-redirect" target="_blank">
    任务调度中心
</a>
```

#### 企业运营后台后端

```java
@RestController
@RequestMapping("/enterprise")
public class SsoRedirectController {

    @Value("${xxl.job.sso.secret}")
    private String ssoSecret;

    @Value("${xxl.job.sso.admin.url}")
    private String adminUrl;

    /**
     * 生成SSO登录链接
     */
    @GetMapping("/xxl-job-login-url")
    public Response<String> getXxlJobLoginUrl(HttpServletRequest request) {
        String phone = getCurrentUserPhone(request);
        String token = XxlJobSsoTokenGenerator.generateToken(phone, ssoSecret, 300);
        
        String loginUrl = adminUrl + "/auth/sso/login" +
            "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8) +
            "&redirect_url=" + URLEncoder.encode(adminUrl + "/", StandardCharsets.UTF_8);
        
        return Response.ofSuccess(loginUrl);
    }

    /**
     * 直接重定向到xxl-job-admin
     */
    @GetMapping("/xxl-job-redirect")
    public void redirectToXxlJob(HttpServletRequest request, 
                                   HttpServletResponse response) throws IOException {
        String phone = getCurrentUserPhone(request);
        String token = XxlJobSsoTokenGenerator.generateToken(phone, ssoSecret, 300);
        
        String loginUrl = adminUrl + "/auth/sso/login" +
            "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8) +
            "&redirect_url=" + URLEncoder.encode(adminUrl + "/", StandardCharsets.UTF_8);
        
        response.sendRedirect(loginUrl);
    }
}
```

---

## 五、安全评估

### 5.1 安全机制

#### 5.1.1 身份认证

| 安全项 | 实现方案 | 说明 |
|--------|----------|------|
| 完整性保护 | HMAC-SHA256签名 | 防止Token被篡改，任何修改都会导致签名验证失败 |
| 真实性验证 | 预共享密钥 | 只有持有密钥的系统才能生成有效Token |
| 签名算法 | HMAC-SHA256 | NIST推荐的安全哈希算法，密钥长度至少256位 |

#### 5.1.2 时效性保护

| 安全项 | 实现方案 | 说明 |
|--------|----------|------|
| Token有效期 | 可配置（默认5分钟） | 过期Token无法使用 |
| 时间戳验证 | 检查timestamp字段 | 防止过期Token被重用 |
| 未来时间检查 | timestamp不能超过当前时间+60秒 | 防止时钟回拨攻击 |

#### 5.1.3 重放攻击防护

| 防护措施 | 说明 |
|----------|------|
| 短有效期 | Token默认5分钟过期，缩短攻击窗口 |
| 时间戳验证 | 相同时间戳的Token在有效期后失效 |
| Nonce可选 | 可实现Nonce缓存，每个Nonce只能使用一次 |

#### 5.1.4 时序攻击防护

| 防护措施 | 说明 |
|----------|------|
| 常量时间比较 | 使用异或运算比较签名，无论是否匹配都执行相同操作 |
| 避免提前返回 | 不使用String.equals()，防止基于时间的侧信道攻击 |

#### 5.1.5 重定向攻击防护

| 防护措施 | 说明 |
|----------|------|
| URL白名单 | 只允许跳转到同源URL |
| 协议检查 | 验证http://或https://开头的URL必须同源 |
| 默认路径 | 非法URL重定向到首页"/" |

### 5.2 风险分析

#### 风险1：Token泄露

**场景描述**：
Token在URL中传递，可能被以下方式泄露：
- 服务器访问日志记录URL
- 浏览器历史记录
- 网络嗅探（HTTP环境）
- Referer头泄漏

**风险等级**：中等

**缓解措施**：

| 措施 | 说明 |
|------|------|
| 短有效期 | Token默认5分钟过期，即使泄露也很快失效 |
| HTTPS强制 | 生产环境必须使用HTTPS，防止网络嗅探 |
| 日志清理 | 配置服务器不记录URL参数，或定期清理日志 |
| 避免Referer | 使用JavaScript跳转或meta refresh，减少Referer泄漏 |

**代码示例（防止Referer泄漏）**：

```html
<!-- 企业运营后台使用JavaScript跳转 -->
<script>
function redirectToXxlJob() {
    fetch('/enterprise/xxl-job-login-url')
        .then(response => response.json())
        .then(data => {
            window.location.href = data.data;
        });
}
</script>

<button onclick="redirectToXxlJob()">任务调度中心</button>
```

#### 风险2：重放攻击

**场景描述**：
攻击者截获有效的Token后，在有效期内重复使用。

**风险等级**：低（有缓解措施）

**缓解措施**：

| 措施 | 说明 |
|------|------|
| 短有效期 | 5分钟有效期限制攻击窗口 |
| 时间戳验证 | 确保Token在有效期内 |
| Nonce机制（可选） | 实现Nonce缓存，每个Nonce只能使用一次 |

**可选增强：Nonce缓存实现**

```java
@Service
public class NonceCacheService {
    
    private final Cache<String, Boolean> nonceCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();
    
    public boolean isNonceUsed(String nonce) {
        if (nonce == null || nonce.isEmpty()) {
            return false;
        }
        return nonceCache.getIfPresent(nonce) != null;
    }
    
    public void markNonceUsed(String nonce) {
        if (nonce != null && !nonce.isEmpty()) {
            nonceCache.put(nonce, true);
        }
    }
}
```

#### 风险3：密钥泄露

**场景描述**：
预共享密钥被泄露，攻击者可以伪造任意用户的Token。

**风险等级**：高

**缓解措施**：

| 措施 | 说明 |
|------|------|
| 强密钥 | 密钥至少32字符，使用随机字符串 |
| 环境变量 | 密钥不硬编码，通过环境变量注入 |
| 配置中心 | 使用配置中心管理密钥，支持热更新 |
| 定期轮换 | 建议每3个月更换一次密钥 |
| 多环境隔离 | 开发、测试、生产环境使用不同密钥 |

**配置示例（使用环境变量）**：

```properties
# application.properties
xxl.job.sso.secret=${XXL_JOB_SSO_SECRET:default-dev-secret}
```

```bash
# 启动时注入
export XXL_JOB_SSO_SECRET="your-production-secret-key-32chars"
java -jar xxl-job-admin.jar
```

#### 风险4：用户不存在

**场景描述**：
Token中的手机号在xxl-job-admin中不存在对应的用户账号。

**风险等级**：低

**缓解措施**：

| 措施 | 说明 |
|------|------|
| 友好错误 | 验证失败时重定向到登录页面，不暴露具体原因 |
| 日志记录 | 记录警告日志，便于排查问题 |
| 用户同步 | 确保用户同步机制正常工作 |

#### 风险5：开放重定向攻击

**场景描述**：
攻击者构造恶意URL诱导用户跳转到钓鱼网站：
```
/auth/sso/login?token=valid-token&redirect_url=http://evil.com/phishing
```

**风险等级**：中

**缓解措施**：

| 措施 | 说明 |
|------|------|
| 同源检查 | 绝对URL必须与当前服务器同源 |
| 相对路径 | 只允许以"/"开头的相对路径 |
| 默认重定向 | 非法URL重定向到首页 |

**验证逻辑**：

```java
private String validateRedirectUrl(String redirectUrl, HttpServletRequest request) {
    if (StringTool.isBlank(redirectUrl)) {
        return "/";
    }
    
    if (!redirectUrl.startsWith("/") && 
        !redirectUrl.startsWith("http://") && 
        !redirectUrl.startsWith("https://")) {
        return "/";
    }
    
    if (redirectUrl.startsWith("http://") || redirectUrl.startsWith("https://")) {
        String requestServer = request.getScheme() + "://" + request.getServerName();
        if (request.getServerPort() != 80 && request.getServerPort() != 443) {
            requestServer += ":" + request.getServerPort();
        }
        if (!redirectUrl.startsWith(requestServer)) {
            logger.warn("External redirect URL detected, blocking: {}", redirectUrl);
            return "/";
        }
    }
    
    return redirectUrl;
}
```

### 5.3 安全建议

#### 5.3.1 生产环境强制要求

| 要求项 | 说明 | 必须/建议 |
|--------|------|-----------|
| HTTPS | 所有SSO相关通信必须使用HTTPS | 必须 |
| 强密钥 | 密钥至少32个随机字符 | 必须 |
| 环境变量 | 密钥通过环境变量注入 | 必须 |
| 日志保护 | 不记录完整的Token | 必须 |
| 定期轮换 | 每3个月更换密钥 | 建议 |
| 监控告警 | 监控SSO登录异常 | 建议 |

#### 5.3.2 密钥管理最佳实践

1. **密钥生成**：
   ```bash
   # 使用openssl生成强随机密钥
   openssl rand -base64 32
   ```

2. **密钥存储**：
   - 开发环境：可配置在 `application-dev.properties`
   - 测试环境：使用配置中心或环境变量
   - 生产环境：使用密钥管理服务（如AWS KMS、阿里云KMS）

3. **密钥轮换**：
   - 支持双密钥过渡期
   - 旧密钥可继续验证一段时间
   - 逐步切换到新密钥

#### 5.3.3 安全监控建议

**监控指标**：

| 指标 | 告警阈值 | 说明 |
|------|----------|------|
| SSO登录失败率 | > 10% | 可能存在攻击或配置问题 |
| 签名验证失败次数 | 5次/分钟 | 可能存在Token篡改尝试 |
| 外部重定向拦截次数 | 1次 | 可能存在开放重定向攻击尝试 |
| 异常IP访问 | 来自陌生IP的SSO请求 | 可能存在攻击者 |

**日志建议**：

```java
// 安全相关日志示例
logger.info("SSO login attempt: phone={}, ip={}", 
    maskPhone(phone), 
    getClientIp(request));

logger.warn("SSO token validation failed: reason={}, ip={}", 
    errorMsg, 
    getClientIp(request));

// 敏感信息脱敏
private String maskPhone(String phone) {
    if (phone == null || phone.length() < 11) {
        return "****";
    }
    return phone.substring(0, 3) + "****" + phone.substring(7);
}
```

### 5.4 安全测试清单

#### 渗透测试项

- [ ] **签名篡改测试**：修改Payload后是否验证失败
- [ ] **过期Token测试**：过期Token是否无法使用
- [ ] **重放攻击测试**：相同Token是否可以重复使用
- [ ] **时序攻击测试**：是否可以通过响应时间推断签名
- [ ] **开放重定向测试**：是否可以跳转到外部网站
- [ ] **用户枚举测试**：是否可以通过错误信息推断用户存在性
- [ ] **SQL注入测试**：手机号参数是否存在注入漏洞
- [ ] **XSS测试**：redirect_url参数是否存在XSS漏洞

---

## 六、实现方案

### 6.1 文件结构

```
xxl-job-admin/src/main/java/com/xxl/job/admin/
├── config/
│   └── XxlJobSsoProperties.java          # SSO配置属性类
├── controller/base/
│   └── SsoLoginController.java            # SSO登录控制器
└── service/
    ├── SsoService.java                    # SSO服务接口
    └── impl/
        └── SsoServiceImpl.java            # SSO服务实现

xxl-job-admin/src/main/resources/
└── application.properties                  # 配置文件（新增SSO配置）

xxl-job-admin/docs/
└── SSO单点登录设计文档.md                   # 本文档
```

### 6.2 核心类说明

#### 6.2.1 XxlJobSsoProperties

**职责**：读取SSO相关配置属性

**配置前缀**：`xxl.job.sso`

**属性说明**：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | boolean | `false` | 是否启用SSO功能 |
| `secret` | String | `null` | 预共享密钥 |
| `tokenExpireSeconds` | long | `300` | Token有效期（秒） |
| `loginFailureUrl` | String | `/auth/login` | 登录失败跳转URL |

#### 6.2.2 SsoService

**职责**：定义SSO服务接口

**方法说明**：

| 方法 | 说明 |
|------|------|
| `validateToken(String token)` | 验证SSO Token的有效性 |
| `autoLogin(String phone)` | 根据手机号自动登录（仅验证用户存在） |

**内部类**：`SsoTokenInfo`

| 字段 | 类型 | 说明 |
|------|------|------|
| `phone` | String | 用户手机号 |
| `timestamp` | Long | Token生成时间戳 |
| `nonce` | String | 随机字符串（可选） |

#### 6.2.3 SsoServiceImpl

**职责**：实现SSO服务的核心逻辑

**核心方法**：

| 方法 | 说明 |
|------|------|
| `validateToken()` | 验证Token格式、签名、时效性 |
| `doLoginWithResponse()` | 执行登录并写入Cookie |
| `calculateHmacSha256()` | 计算HMAC-SHA256签名 |
| `constantTimeEquals()` | 常量时间字符串比较 |

**验证流程**：

```
validateToken(token)
    │
    ├───> 检查SSO是否启用
    │
    ├───> 检查Token是否为空
    │
    ├───> 分割Token为[Payload, Signature]
    │
    ├───> 验证HMAC-SHA256签名（常量时间比较）
    │
    ├───> Base64Url解码Payload
    │
    ├───> 解析JSON获取phone、timestamp、nonce
    │
    ├───> 验证必填字段存在
    │
    ├───> 验证时间戳在有效期内
    │
    └───> 返回SsoTokenInfo
```

#### 6.2.4 SsoLoginController

**职责**：提供SSO登录HTTP端点

**端点**：`GET /auth/sso/login`

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `token` | String | 是 | SSO Token |
| `redirect_url` | String | 否 | 登录成功后跳转的URL |

**处理流程**：

```
ssoLogin(request, response, token, redirectUrl)
    │
    ├───> SSO未启用 ────────────────> 重定向到登录页
    │
    ├───> Token为空 ─────────────────> 重定向到登录页
    │
    ├───> 验证Token失败 ─────────────> 重定向到登录页
    │
    ├───> 自动登录失败 ──────────────> 重定向到登录页
    │
    └───> 验证重定向URL ─────────────> 重定向到目标页
```

### 6.3 配置变更

#### 新增配置

```properties
### xxl-job-admin SSO (单点登录配置)
xxl.job.sso.enabled=false
xxl.job.sso.secret=please-change-this-to-a-strong-secret-key-at-least-32-characters
xxl.job.sso.token.expire.seconds=300
xxl.job.sso.login.failure.url=/auth/login
```

#### 修改配置

```properties
### 原有配置修改：排除SSO路径
xxl-sso.client.excluded.paths=/auth/sso/login
```

### 6.4 依赖说明

**新增依赖**：无（使用现有依赖）

**使用的现有依赖**：

| 依赖 | 用途 |
|------|------|
| `xxl-sso-core` | 登录会话管理 |
| `xxl-tool` | JSON处理、工具类 |
| `javax.crypto` | HMAC-SHA256签名计算 |
| `Spring Web` | 控制器、重定向 |

### 6.5 错误码定义

| 错误场景 | 日志级别 | 用户提示 |
|----------|----------|----------|
| SSO未启用 | WARN | （重定向到登录页） |
| Token为空 | WARN | （重定向到登录页） |
| Token格式错误 | WARN | （重定向到登录页） |
| 签名验证失败 | WARN | （重定向到登录页） |
| Payload解析失败 | ERROR | （重定向到登录页） |
| 缺少必填字段 | WARN | （重定向到登录页） |
| Token已过期 | WARN | （重定向到登录页） |
| 时间戳在未来 | WARN | （重定向到登录页） |
| 用户不存在 | WARN | （重定向到登录页） |
| 外部重定向拦截 | WARN | （重定向到首页） |

---

## 七、测试方案

### 7.1 单元测试

#### 7.1.1 Token验证测试

```java
@SpringBootTest
public class SsoServiceImplTest {

    @InjectMocks
    private SsoServiceImpl ssoService;

    @Mock
    private XxlJobSsoProperties properties;

    private final String testSecret = "test-secret-key-32-characters-here";

    @BeforeEach
    void setUp() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getSecret()).thenReturn(testSecret);
        when(properties.getTokenExpireSeconds()).thenReturn(300L);
    }

    @Test
    void testValidateToken_ValidToken() {
        String validToken = generateValidToken("13800138000", testSecret);
        Response<SsoService.SsoTokenInfo> result = ssoService.validateToken(validToken);
        assertTrue(result.isSuccess());
        assertEquals("13800138000", result.getData().phone());
    }

    @Test
    void testValidateToken_InvalidSignature() {
        String tamperedToken = generateValidToken("13800138000", testSecret) + "x";
        Response<SsoService.SsoTokenInfo> result = ssoService.validateToken(tamperedToken);
        assertFalse(result.isSuccess());
    }

    @Test
    void testValidateToken_ExpiredToken() {
        String expiredToken = generateTokenWithTimestamp(
            "13800138000", 
            testSecret, 
            System.currentTimeMillis() - 600000
        );
        Response<SsoService.SsoTokenInfo> result = ssoService.validateToken(expiredToken);
        assertFalse(result.isSuccess());
    }

    @Test
    void testValidateToken_EmptyToken() {
        Response<SsoService.SsoTokenInfo> result = ssoService.validateToken("");
        assertFalse(result.isSuccess());
    }

    @Test
    void testValidateToken_NullToken() {
        Response<SsoService.SsoTokenInfo> result = ssoService.validateToken(null);
        assertFalse(result.isSuccess());
    }
}
```

#### 7.1.2 常量时间比较测试

```java
@Test
void testConstantTimeEquals_SameString() {
    // 通过反射调用私有方法
    Method method = SsoServiceImpl.class.getDeclaredMethod(
        "constantTimeEquals", String.class, String.class
    );
    method.setAccessible(true);
    
    boolean result = (Boolean) method.invoke(ssoService, "abc123", "abc123");
    assertTrue(result);
}

@Test
void testConstantTimeEquals_DifferentString() {
    Method method = SsoServiceImpl.class.getDeclaredMethod(
        "constantTimeEquals", String.class, String.class
    );
    method.setAccessible(true);
    
    boolean result = (Boolean) method.invoke(ssoService, "abc123", "abc124");
    assertFalse(result);
}
```

### 7.2 集成测试

#### 7.2.1 完整SSO登录流程测试

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class SsoLoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private XxlJobUserMapper userMapper;

    @Value("${xxl.job.sso.secret}")
    private String ssoSecret;

    @Test
    void testSsoLogin_Success() throws Exception {
        String phone = "13800138000";
        ensureUserExists(phone);
        
        String token = generateValidToken(phone, ssoSecret);
        
        mockMvc.perform(get("/auth/sso/login")
                .param("token", token)
                .param("redirect_url", "/"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"))
            .andExpect(cookie().exists("xxl_job_login_token"));
    }

    @Test
    void testSsoLogin_InvalidToken() throws Exception {
        mockMvc.perform(get("/auth/sso/login")
                .param("token", "invalid-token"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/auth/login"));
    }

    @Test
    void testSsoLogin_UserNotExists() throws Exception {
        String phone = "99999999999";
        String token = generateValidToken(phone, ssoSecret);
        
        mockMvc.perform(get("/auth/sso/login")
                .param("token", token))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/auth/login"));
    }

    @Test
    void testSsoLogin_ExternalRedirectBlocked() throws Exception {
        String phone = "13800138000";
        ensureUserExists(phone);
        
        String token = generateValidToken(phone, ssoSecret);
        
        mockMvc.perform(get("/auth/sso/login")
                .param("token", token)
                .param("redirect_url", "http://evil.com"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));
    }
}
```

### 7.3 安全测试

#### 7.3.1 签名篡改测试

**测试目标**：验证修改Payload后签名验证失败

**测试步骤**：
1. 生成有效的Token
2. 修改Payload中的手机号
3. 重新编码Payload但使用原签名
4. 验证请求被拒绝

**预期结果**：重定向到登录页面

#### 7.3.2 重放攻击测试

**测试目标**：验证Token有效期机制

**测试步骤**：
1. 生成Token
2. 在有效期内使用（应该成功）
3. 等待Token过期
4. 再次使用同一Token

**预期结果**：过期后使用失败

#### 7.3.3 开放重定向测试

**测试目标**：验证外部URL被拦截

**测试用例**：

| redirect_url | 预期行为 |
|--------------|----------|
| `http://evil.com` | 重定向到 `/` |
| `https://evil.com` | 重定向到 `/` |
| `//evil.com` | 重定向到 `/` |
| `/dashboard` | 正常重定向 |
| `http://localhost:8080/` | 正常重定向（同源） |

### 7.4 手动测试步骤

#### 步骤1：准备测试环境

1. 启动xxl-job-admin
2. 配置SSO启用：`xxl.job.sso.enabled=true`
3. 配置密钥：`xxl.job.sso.secret=test-secret-key-32-characters`
4. 在数据库中创建测试用户（username为手机号）

#### 步骤2：生成测试Token

使用以下代码生成Token：

```java
public static void main(String[] args) {
    String phone = "13800138000";
    String secret = "test-secret-key-32-characters";
    
    Map<String, Object> payload = new HashMap<>();
    payload.put("phone", phone);
    payload.put("timestamp", System.currentTimeMillis());
    
    String payloadJson = GsonTool.toJson(payload);
    String payloadBase64 = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
    
    String signature = calculateHmacSha256(payloadBase64, secret);
    String token = payloadBase64 + "." + signature;
    
    System.out.println("Token: " + token);
}
```

#### 步骤3：测试SSO登录

1. 构造URL：`http://localhost:8080/xxl-job-admin/auth/sso/login?token=<生成的Token>`
2. 在浏览器中访问该URL
3. 验证是否自动登录并跳转到首页

#### 步骤4：测试错误场景

1. **Token为空**：访问 `/auth/sso/login`
2. **无效Token**：使用随机字符串作为Token
3. **过期Token**：生成5分钟前的Token
4. **用户不存在**：使用不存在的手机号生成Token

---

## 八、部署与运维

### 8.1 配置清单

#### 生产环境配置

```properties
### 生产环境推荐配置

# 启用SSO
xxl.job.sso.enabled=true

# 强密钥（通过环境变量注入）
xxl.job.sso.secret=${XXL_JOB_SSO_SECRET}

# Token有效期5分钟
xxl.job.sso.token.expire.seconds=300

# 失败跳转登录页
xxl.job.sso.login.failure.url=/auth/login

# 排除SSO路径
xxl-sso.client.excluded.paths=/auth/sso/login
```

#### 环境变量配置

```bash
# Linux/macOS
export XXL_JOB_SSO_SECRET="your-strong-production-secret-key-32chars"

# Windows PowerShell
$env:XXL_JOB_SSO_SECRET="your-strong-production-secret-key-32chars"
```

### 8.2 部署检查清单

- [ ] SSO功能已启用（`enabled=true`）
- [ ] 密钥已配置且足够强壮（至少32字符）
- [ ] 密钥通过环境变量注入（未硬编码）
- [ ] xxl-sso拦截器已排除 `/auth/sso/login` 路径
- [ ] 企业运营后台与xxl-job-admin时间同步
- [ ] 用户账号已同步（手机号作为username）
- [ ] 生产环境使用HTTPS
- [ ] 日志中不记录完整Token

### 8.3 运维操作

#### 8.3.1 启用/禁用SSO

**启用SSO**：
```properties
xxl.job.sso.enabled=true
```

**禁用SSO**：
```properties
xxl.job.sso.enabled=false
```

> 注意：禁用SSO后，用户只能通过传统的用户名密码方式登录。

#### 8.3.2 密钥轮换

**步骤**：
1. 准备新密钥
2. 在企业运营后台和xxl-job-admin同时配置双密钥（过渡期）
3. 逐步切换到新密钥
4. 移除旧密钥

**过渡期配置示例**：
```java
// 支持双密钥验证
public Response<SsoTokenInfo> validateToken(String token) {
    // 先尝试新密钥
    Response<SsoTokenInfo> result = validateWithKey(token, newSecret);
    if (result.isSuccess()) {
        return result;
    }
    // 再尝试旧密钥
    return validateWithKey(token, oldSecret);
}
```

#### 8.3.3 故障排查

**常见问题**：

| 问题 | 可能原因 | 解决方案 |
|------|----------|----------|
| Token验证失败 | 密钥不一致 | 检查两边密钥配置 |
| Token验证失败 | 时间不同步 | 配置NTP时间同步 |
| Token验证失败 | Token已过期 | 检查Token生成时间 |
| 自动登录失败 | 用户不存在 | 同步用户账号 |
| 无法访问SSO端点 | 路径被拦截 | 检查excluded.paths配置 |

**日志排查**：

```bash
# 查看SSO相关日志
grep -i "sso\|SSO" application.log

# 查看登录失败日志
grep -i "login.*fail\|fail.*login" application.log

# 查看Token验证日志
grep -i "token" application.log
```

### 8.4 回滚方案

**快速回滚**：

如果SSO功能出现问题，可通过以下方式快速回滚：

1. **修改配置**：
   ```properties
   xxl.job.sso.enabled=false
   ```

2. **重启应用**（如需要）

3. **用户登录方式**：
   - 用户将通过传统的用户名密码方式登录
   - SSO链接将自动跳转到登录页面

**渐进式回滚**：

1. 先在企业运营后台禁用SSO跳转
2. 等待所有活跃会话结束
3. 再在xxl-job-admin禁用SSO功能

---

## 九、附录

### 附录A：Token生成与验证示例代码

#### A.1 Java完整示例

```java
package com.example.sso;

import com.xxl.tool.json.GsonTool;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * xxl-job-admin SSO Token工具类
 * 包含生成和验证功能
 */
public class XxlJobSsoTokenUtils {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final int DEFAULT_EXPIRE_SECONDS = 300;

    /**
     * 生成SSO Token
     */
    public static String generateToken(String phone, String secret) {
        return generateToken(phone, secret, DEFAULT_EXPIRE_SECONDS);
    }

    /**
     * 生成SSO Token（指定有效期）
     */
    public static String generateToken(String phone, String secret, int expireSeconds) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("phone", phone);
        payload.put("timestamp", System.currentTimeMillis());
        
        String payloadJson = GsonTool.toJson(payload);
        String payloadBase64 = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        
        String signature = calculateHmacSha256(payloadBase64, secret);
        
        return payloadBase64 + "." + signature;
    }

    /**
     * 验证SSO Token
     */
    public static boolean validateToken(String token, String secret, int expireSeconds) {
        if (token == null || !token.contains(".")) {
            return false;
        }
        
        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            return false;
        }
        
        String payloadBase64 = parts[0];
        String signature = parts[1];
        
        String expectedSignature = calculateHmacSha256(payloadBase64, secret);
        if (!constantTimeEquals(signature, expectedSignature)) {
            return false;
        }
        
        try {
            String payloadJson = new String(
                    Base64.getUrlDecoder().decode(payloadBase64),
                    StandardCharsets.UTF_8
            );
            
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = GsonTool.fromJson(
                payloadJson, 
                new com.google.gson.reflect.TypeToken<Map<String, Object>>() {}.getType()
            );
            
            Object timestampObj = payload.get("timestamp");
            if (timestampObj == null) {
                return false;
            }
            
            long timestamp = ((Number) timestampObj).longValue();
            long now = System.currentTimeMillis();
            
            return now - timestamp <= expireSeconds * 1000L;
            
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 计算HMAC-SHA256签名
     */
    private static String calculateHmacSha256(String data, String secret) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256
            );
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(keySpec);
            byte[] result = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(result);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 calculation failed", e);
        }
    }

    /**
     * 常量时间字符串比较
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

    /**
     * 使用示例
     */
    public static void main(String[] args) {
        String secret = "your-strong-secret-key-32-characters-here";
        String phone = "13800138000";
        
        // 生成Token
        String token = generateToken(phone, secret);
        System.out.println("Generated Token: " + token);
        
        // 验证Token
        boolean valid = validateToken(token, secret, 300);
        System.out.println("Token Valid: " + valid);
    }
}
```

### 附录B：Base64Url编码说明

**与标准Base64的区别**：

| 字符 | 标准Base64 | Base64Url |
|------|-----------|-----------|
| `+` | ✅ | 替换为 `-` |
| `/` | ✅ | 替换为 `_` |
| `=` 填充 | ✅ | 移除 |

**Java实现**：
```java
// Base64Url编码
String encoded = Base64.getUrlEncoder()
    .withoutPadding()
    .encodeToString(bytes);

// Base64Url解码
byte[] decoded = Base64.getUrlDecoder().decode(encoded);
```

### 附录C：HMAC-SHA256算法说明

**算法特点**：
- 基于SHA-256哈希算法
- 使用密钥进行消息认证
- 提供完整性和真实性验证
- NIST推荐的安全算法

**密钥要求**：
- 最小长度：16字节（128位）
- 推荐长度：32字节（256位）
- 使用随机生成的密钥

**安全属性**：
- 抗碰撞性
- 抗原像性
- 抗第二原像性

### 附录D：时序攻击说明

**攻击原理**：
普通的字符串比较（如 `String.equals()`）在发现第一个不匹配的字符时就返回结果。攻击者可以通过测量响应时间来推断正确的字符。

**示例**：
```java
// 不安全的比较方式
public boolean insecureCompare(String a, String b) {
    if (a.length() != b.length()) return false;
    for (int i = 0; i < a.length(); i++) {
        if (a.charAt(i) != b.charAt(i)) {
            return false;  // 提前返回，泄露信息
        }
    }
    return true;
}
```

**安全的比较方式**：
```java
// 安全的常量时间比较
public boolean secureCompare(String a, String b) {
    if (a.length() != b.length()) return false;
    int result = 0;
    for (int i = 0; i < a.length(); i++) {
        result |= a.charAt(i) ^ b.charAt(i);
    }
    return result == 0;
}
```

### 附录E：参考资料

1. **RFC 7515 - JSON Web Signature (JWS)**
   - https://tools.ietf.org/html/rfc7515

2. **RFC 2104 - HMAC: Keyed-Hashing for Message Authentication**
   - https://tools.ietf.org/html/rfc2104

3. **OWASP - Unvalidated Redirects and Forwards**
   - https://cheatsheetseries.owasp.org/cheatsheets/Unvalidated_Redirects_and_Forwards_Cheat_Sheet.html

4. **OWASP - Timing Attacks**
   - https://owasp.org/www-community/attacks/Timing_Attack

5. **xxl-sso 官方文档**
   - https://www.xuxueli.com/xxl-sso/

---

**文档版本**：v1.0  
**最后更新**：2026-04-17  
**作者**：xxl-job SSO开发团队
