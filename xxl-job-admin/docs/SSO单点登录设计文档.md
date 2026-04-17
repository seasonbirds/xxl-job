# xxl-job-admin 单点登录（SSO）设计文档

---

## 一、背景与目标

### 1.1 业务背景

xxl-job-admin作为企业运营后台的子系统，需要实现单点登录功能。用户在企业运营后台通过点击链接即可自动登录到xxl-job-admin，无需再次输入用户名密码。

### 1.2 安全问题

**原方案存在的安全问题**：

| 问题 | 描述 | 风险等级 |
|------|------|----------|
| Token过期直接重定向 | Token过期时直接重定向到企业运营后台，运营后台无法验证请求合法性 | 高 |
| 无Refresh Token机制 | 每次Token过期都需要用户重新跳转，体验差 | 中 |
| 无CSRF防护 | 重定向到企业运营后台时无验证参数，易被利用 | 高 |

### 1.3 改进目标

1. **引入双Token机制**：Access Token短期有效，Refresh Token长期有效用于刷新
2. **自动刷新机制**：Access Token过期时自动使用Refresh Token刷新，用户无感知
3. **安全重定向**：只有Refresh Token也失效时才重定向到企业运营后台，并携带State参数防CSRF
4. **企业运营后台验证**：State参数包含签名和时间戳，运营后台可以验证请求合法性

---

## 二、双Token机制设计

### 2.1 Token类型

| Token类型 | 有效期 | 存储位置 | 用途 | 安全措施 |
|-----------|--------|----------|------|----------|
| **Access Token** | 30分钟（默认） | 内存/请求上下文 | 访问受保护资源 | JWT格式，HMAC-SHA256签名 |
| **Refresh Token** | 7天（默认） | 加密Cookie | 刷新Access Token | AES-256-GCM加密，HttpOnly，Secure |

### 2.2 Token结构

#### Access Token（JWT格式）

```
[Header].[Payload].[Signature]
```

**Header**：
```json
{"alg":"HS256","typ":"JWT"}
```

**Payload**：
```json
{
  "type": "access",
  "sub": "user_id",
  "phone": "13800138000",
  "iat": 1713340800,
  "exp": 1713342600,
  "nonce": "random_string",
  "rt_id": "refresh_token_id"
}
```

#### Refresh Token（加密存储）

**明文Payload**：
```json
{
  "type": "refresh",
  "tid": "RT_1713340800000_abc123",
  "sub": "user_id",
  "phone": "13800138000",
  "iat": 1713340800,
  "exp": 1713945600,
  "nonce": "random_string",
  "ver": 1
}
```

**加密方式**：AES-256-GCM，带认证标签

**Cookie属性**：
- HttpOnly：防止JavaScript访问
- Secure：仅HTTPS传输（生产环境）
- SameSite=Lax：防止CSRF
- Path：应用上下文路径

### 2.3 交互流程

#### 首次登录流程

```
企业运营后台                           xxl-job-admin
      │                                    │
      │──1. 用户点击"任务调度"链接────────>│
      │                                    │
      │  2. 生成SSO Token (HMAC-SHA256)   │
      │                                    │
      │<──3. 302重定向────────────────────│
      │    Location: /auth/sso/login      │
      │    ?token=xxx&redirect_url=xxx    │
      │                                    │
      │───────────────────────────────────>│
      │                                    │
      │                                    │──4. 验证SSO Token签名
      │                                    │──5. 根据手机号查询用户
      │                                    │──6. 创建xxl-sso会话
      │                                    │──7. 生成Access Token + Refresh Token
      │                                    │──8. Refresh Token存入加密Cookie
      │                                    │
      │<──9. 302重定向────────────────────│
      │    Set-Cookie: xxl_job_rt=xxx     │
      │    (HttpOnly, Secure, SameSite)   │
```

#### Token自动刷新流程

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

---

## 三、安全改进详解

### 3.1 双Token机制的安全性

**为什么需要双Token？**

| 场景 | 单Token方案 | 双Token方案 |
|------|------------|-------------|
| Token有效期 | 需要平衡安全性和用户体验 | Access Token短期（安全），Refresh Token长期（体验） |
| Token泄露风险 | 长期Token泄露风险高 | Access Token短期泄露风险低，Refresh Token加密存储 |
| 自动续期 | 无法实现 | Access Token过期自动刷新 |
| 撤销机制 | 困难 | Refresh Token可撤销、可轮换 |

### 3.2 Refresh Token安全措施

**加密存储**：
- 使用AES-256-GCM加密算法
- 带认证标签，防止篡改
- 密钥可配置，也可从secret派生

**Cookie安全属性**：
```java
Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
cookie.setMaxAge(expireSeconds);
cookie.setPath(contextPath);
cookie.setHttpOnly(true);  // 防止XSS
cookie.setSecure(isSecure);  // 仅HTTPS
cookie.setAttribute("SameSite", "Lax");  // 防止CSRF
```

**单次使用机制**：
- 每次刷新Access Token时，生成新的Refresh Token
- 旧的Refresh Token标记为已使用
- 防止Token复用攻击

### 3.3 State参数防CSRF

**问题场景**：

攻击者可能构造以下链接诱导用户点击：
```
http://xxl-job-admin/auth/sso/redirect?redirect_url=http://evil.com
```

**解决方案**：State参数

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

**State参数验证（企业运营后台）**：
```java
public static boolean validateState(String state, String secret, int expireSeconds) {
    String[] parts = state.split(":");
    if (parts.length != 3) return false;
    
    String payload = parts[0] + ":" + parts[1];
    String signature = parts[2];
    
    // 1. 验证签名
    if (!verifyHmacSha256(payload, signature, secret)) {
        return false;
    }
    
    // 2. 验证时间戳
    long timestamp = Long.parseLong(parts[1]);
    return System.currentTimeMillis() - timestamp <= expireSeconds * 1000L;
}
```

### 3.4 企业运营后台对接说明

**企业运营后台需要实现的功能**：

1. **SSO入口端点**：
   - 路径示例：`/enterprise/sso/xxl-job-redirect`
   - 功能：接收来自xxl-job-admin的重定向请求

2. **验证State参数**：
   - 验证HMAC-SHA256签名
   - 验证时间戳有效期
   - 防止CSRF攻击

3. **检查用户登录状态**：
   - 用户已登录 → 生成SSO Token，跳转回xxl-job-admin
   - 用户未登录 → 跳转到登录页面

**企业运营后台示例代码**：

```java
@Controller
@RequestMapping("/enterprise")
public class EnterpriseSsoController {

    @Value("${xxl.job.sso.secret}")
    private String ssoSecret;

    @Value("${xxl.job.sso.admin.url}")
    private String adminUrl;

    /**
     * xxl-job-admin SSO入口
     * 接收来自xxl-job-admin的重定向请求
     */
    @GetMapping("/sso/xxl-job-redirect")
    public RedirectView xxlJobSsoRedirect(
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "redirect_url", required = false) String redirectUrl,
            HttpServletRequest request) {
        
        // 1. 验证State参数（防CSRF）
        if (state != null && !CryptoUtil.validateState(state, ssoSecret, 300)) {
            logger.warn("Invalid state parameter: {}", state);
            return new RedirectView("/login");
        }
        
        // 2. 检查用户登录状态
        if (!isUserLoggedIn(request)) {
            // 用户未登录，跳转到登录页
            String loginRedirect = "/login?redirect=" + 
                URLEncoder.encode(request.getRequestURL().toString(), "UTF-8");
            return new RedirectView(loginRedirect);
        }
        
        // 3. 获取当前用户手机号
        String phone = getCurrentUserPhone(request);
        
        // 4. 生成SSO Token
        String token = XxlJobSsoTokenGenerator.generateToken(phone, ssoSecret);
        
        // 5. 构建跳转回xxl-job-admin的URL
        String targetUrl = (redirectUrl != null && !redirectUrl.isEmpty()) 
            ? redirectUrl 
            : adminUrl + "/";
        
        String ssoLoginUrl = adminUrl + "/auth/sso/login" +
            "?token=" + URLEncoder.encode(token, "UTF-8") +
            "&redirect_url=" + URLEncoder.encode(targetUrl, "UTF-8");
        
        return new RedirectView(ssoLoginUrl);
    }
}
```

---

## 四、系统对接

### 4.1 xxl-job-admin配置

```properties
### 启用SSO
xxl.job.sso.enabled=true

### 预共享密钥（与企业运营后台一致）
xxl.job.sso.secret=your-32-character-strong-secret-key

### 企业运营后台SSO入口URL
### 当Refresh Token失效时，重定向到此URL
xxl.job.sso.enterprise.sso.url=http://enterprise-host:port/enterprise/sso/xxl-job-redirect

### === 双Token配置 ===
### Access Token有效期（秒）- 默认30分钟
xxl.job.sso.access.token.expire.seconds=1800

### Refresh Token有效期（秒）- 默认7天
xxl.job.sso.refresh.token.expire.seconds=604800

### Refresh Token加密密钥（可选，Base64编码32字节）
### 生成方式：openssl rand -base64 32
xxl.job.sso.refresh.token.encrypt.key=

### xxl-sso配置（已预设）
xxl-sso.client.excluded.paths=/auth/sso/login,/auth/sso/redirect
xxl-sso.client.login.path=/auth/sso/redirect
```

### 4.2 企业运营后台配置

```properties
### xxl-job-admin地址
xxl.job.sso.admin.url=http://xxl-job-host:port/xxl-job-admin

### 预共享密钥（与xxl-job-admin一致）
xxl.job.sso.secret=your-32-character-strong-secret-key

### Token有效期（秒）
xxl.job.sso.token.expire.seconds=300
```

### 4.3 端点清单

| 端点 | 方法 | 所属系统 | 说明 |
|------|------|----------|------|
| `/auth/sso/login` | GET | xxl-job-admin | SSO登录入口，接收企业运营后台的Token |
| `/auth/sso/redirect` | GET | xxl-job-admin | Token过期重定向端点（xxl-sso配置） |
| `/enterprise/sso/xxl-job-redirect` | GET | 企业运营后台 | SSO入口，接收xxl-job-admin的重定向 |

---

## 五、新增文件清单

| 文件路径 | 说明 |
|----------|------|
| `service/token/TokenInfo.java` | Token信息基类 |
| `service/token/AccessTokenInfo.java` | Access Token信息类 |
| `service/token/RefreshTokenInfo.java` | Refresh Token信息类 |
| `service/token/TokenService.java` | Token服务接口 |
| `service/token/TokenServiceImpl.java` | Token服务实现 |
| `util/CryptoUtil.java` | 加密工具类（AES、HMAC、常量时间比较） |
| `util/TokenCookieUtil.java` | Token Cookie工具类 |
| `config/XxlJobSsoProperties.java` | SSO配置属性（已更新） |
| `controller/base/SsoLoginController.java` | SSO控制器（已更新） |

---

## 六、安全检查清单

### 开发阶段

- [ ] 预共享密钥使用强随机字符串（至少32字符）
- [ ] 密钥不硬编码，使用环境变量或配置中心
- [ ] Refresh Token使用AES-256-GCM加密
- [ ] Refresh Token Cookie设置HttpOnly、Secure、SameSite属性
- [ ] State参数包含签名和时间戳验证
- [ ] 企业运营后台验证State参数

### 测试阶段

- [ ] Access Token过期时自动刷新
- [ ] Refresh Token过期时携带State参数重定向
- [ ] 企业运营后台验证State参数有效性
- [ ] 无效State参数被拒绝
- [ ] Refresh Token轮换机制正常工作

### 生产环境

- [ ] 强制使用HTTPS
- [ ] 密钥定期轮换（建议每3个月）
- [ ] 监控SSO登录异常
- [ ] 日志不记录完整Token

---

**文档版本**：v3.0  
**最后更新**：2026-04-17  
**安全改进**：双Token机制、自动刷新、State参数防CSRF
