# XXL-JOB-Admin 单点登录(SSO)设计文档

## 1. 概述

### 1.1 背景
企业运营后台作为主系统，希望通过单点登录方式集成 xxl-job-admin 任务调度平台。两个系统通过手机号作为用户唯一标识进行关联。

### 1.2 目标
- 实现从企业运营后台到 xxl-job-admin 的无缝单点登录
- 保持现有 xxl-job-admin 的登录机制不变
- 确保安全性和用户体验

---

## 2. 实现原理

### 2.1 核心机制

#### 2.1.1 JWT Token 认证方案
采用 **JWT (JSON Web Token)** 作为身份认证凭证，实现无状态的单点登录。

```
┌─────────────────────────────────────────────────────────────┐
│                    认证流程                                    │
├─────────────────────────────────────────────────────────────┤
│  企业运营后台                    xxl-job-admin                │
│       │                              │                        │
│       │  1. 用户点击"任务调度"链接    │                        │
│       │─────────────────────────────>│                        │
│       │                              │                        │
│       │  2. 生成JWT Token            │                        │
│       │     (包含手机号、签名、过期时间) │                        │
│       │                              │                        │
│       │  3. 重定向到SSO端点           │                        │
│       │     /auth/sso?token=xxx      │                        │
│       │─────────────────────────────>│                        │
│       │                              │                        │
│       │                              │  4. 验证JWT签名         │
│       │                              │  5. 检查Token有效期     │
│       │                              │  6. 根据手机号查找用户    │
│       │                              │  7. (可选)自动创建用户    │
│       │                              │  8. 设置SSO登录状态      │
│       │                              │  9. 重定向到首页          │
│       │<─────────────────────────────│                        │
└─────────────────────────────────────────────────────────────┘
```

#### 2.1.2 JWT Token 结构

```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "phone": "13800138000",
    "iat": 1713302400,
    "exp": 1713302700,
    "jti": "uuid-xxx-xxx",
    "iss": "enterprise-portal",
    "aud": "xxl-job-admin"
  },
  "signature": "HMACSHA256(base64UrlEncode(header) + '.' + base64UrlEncode(payload), secret)"
}
```

**Payload 字段说明：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| phone | String | 是 | 用户手机号，作为用户唯一标识 |
| iat | Long | 是 | 签发时间戳（秒） |
| exp | Long | 是 | 过期时间戳（秒） |
| jti | String | 否 | Token唯一ID，可用于防止重放 |
| iss | String | 否 | 签发者 |
| aud | String | 否 | 受众 |

#### 2.1.3 用户关联机制

两个系统通过 **手机号** 作为唯一关联键：

```
企业运营后台用户                      xxl-job-admin用户
┌──────────────────┐              ┌──────────────────────┐
│  userId: 1001    │              │  id: 1               │
│  phone: 138...   │◄────────────►│  username: 138...    │
│  name: 张三       │   手机号关联   │  password: (hash)    │
│  ...             │              │  role: 1 (管理员)    │
└──────────────────┘              └──────────────────────┘
```

**关键点：**
1. xxl-job-admin 的 `username` 字段存储手机号
2. 用户在运营后台已存在时，xxl-job-admin 中必须预先创建对应用户
3. （可选）支持自动创建用户功能

---

## 3. 系统交互

### 3.1 完整交互时序图

```
┌─────────┐         ┌──────────────┐         ┌──────────────┐
│  用户   │         │ 企业运营后台   │         │ xxl-job-admin│
└────┬────┘         └──────┬───────┘         └──────┬───────┘
     │                     │                        │
     │  1. 点击"任务调度"   │                        │
     │────────────────────>│                        │
     │                     │                        │
     │                     │ 2. 生成JWT Token       │
     │                     │   - 提取用户手机号      │
     │                     │   - 设置过期时间(5分钟) │
     │                     │   - 使用HS256签名       │
     │                     │                        │
     │  3. 302重定向       │                        │
     │<────────────────────│                        │
     │  Location:          │                        │
     │  /auth/sso?         │                        │
     │    token=xxx&       │                        │
     │    redirect=/       │                        │
     │                     │                        │
     │  4. 访问SSO端点      │                        │
     │─────────────────────────────────────────────>│
     │                     │                        │
     │                     │                        │ 5. 验证Token
     │                     │                        │   - 解析JWT
     │                     │                        │   - 验证签名
     │                     │                        │   - 检查过期时间
     │                     │                        │
     │                     │                        │ 6. 查询用户
     │                     │                        │   SELECT * FROM 
     │                     │                        │   xxl_job_user 
     │                     │                        │   WHERE username=?
     │                     │                        │
     │                     │                        │ 7. 设置登录状态
     │                     │                        │   - 调用xxl-sso
     │                     │                        │   - 写入Cookie
     │                     │                        │
     │  8. 302重定向到首页  │                        │
     │<─────────────────────────────────────────────│
     │  Location: /        │                        │
     │                     │                        │
     │  9. 访问首页         │                        │
     │─────────────────────────────────────────────>│
     │                     │                        │
     │  10. 返回调度平台首页 │                        │
     │<─────────────────────────────────────────────│
```

### 3.2 接口定义

#### 3.2.1 SSO 登录端点

**URL**: `GET /auth/sso`

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| token | String | 是 | JWT Token |
| redirect | String | 否 | 登录成功后的重定向路径，默认 `/` |

**错误处理：**

| 场景 | HTTP状态码 | 处理方式 |
|------|-----------|---------|
| Token为空 | 400 | 重定向到登录页，提示参数错误 |
| Token解析失败 | 401 | 重定向到登录页，提示Token无效 |
| 签名验证失败 | 401 | 重定向到登录页，提示Token无效 |
| Token已过期 | 401 | 重定向到登录页，提示Token已过期 |
| 用户不存在 | 403 | 重定向到登录页，提示用户未授权 |

---

## 4. 系统对接

### 4.1 企业运营后台对接说明

#### 4.1.1 需要实现的功能

1. **生成 JWT Token**

```java
// 示例代码（运营后台实现）
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

public class JwtTokenGenerator {
    
    // 与xxl-job-admin约定的密钥
    private static final String SECRET = "your-256-bit-secret-key-at-least-256-bits";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes());
    
    // Token有效期：5分钟
    private static final long EXPIRATION = 5 * 60 * 1000;
    
    /**
     * 生成SSO Token
     * @param phone 用户手机号
     * @return JWT Token
     */
    public static String generateToken(String phone) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION);
        
        return Jwts.builder()
                .subject(phone)
                .claim("phone", phone)
                .issuedAt(now)
                .expiration(expiration)
                .id(UUID.randomUUID().toString())
                .issuer("enterprise-portal")
                .audience().add("xxl-job-admin").and()
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }
    
    /**
     * 构建SSO跳转URL
     */
    public static String buildSsoUrl(String phone, String redirectUrl) {
        String token = generateToken(phone);
        return String.format("http://xxl-job-admin-server/xxl-job-admin/auth/sso?token=%s&redirect=%s",
                token, 
                java.net.URLEncoder.encode(redirectUrl, java.nio.charset.StandardCharsets.UTF_8)
        );
    }
}
```

#### 4.1.2 页面集成方式

在运营后台页面添加跳转链接：

```html
<!-- 方式1：普通链接 -->
<a href="/api/xxl-job/sso-redirect" target="_blank">任务调度平台</a>

<!-- 方式2：后端重定向 -->
@GetMapping("/api/xxl-job/sso-redirect")
public void redirectToXxlJob(HttpServletResponse response, 
                              @CurrentUser User currentUser) throws IOException {
    String ssoUrl = JwtTokenGenerator.buildSsoUrl(
        currentUser.getPhone(), 
        "/xxl-job-admin/"
    );
    response.sendRedirect(ssoUrl);
}
```

### 4.2 xxl-job-admin 对接配置

#### 4.2.1 配置项说明

在 `application.properties` 中添加以下配置：

```properties
# ==================== SSO 配置 ====================
# SSO功能开关
xxl.job.sso.enabled=true

# JWT签名密钥（必须与运营后台一致，至少256位）
xxl.job.sso.jwt.secret=xxl-job-sso-secret-key-2024-minimum-256-bits-required

# Token最大有效时间（秒），防止重放攻击，默认300秒(5分钟)
xxl.job.sso.jwt.expiration=300

# Token签发者（可选，用于验证iss字段）
xxl.job.sso.jwt.issuer=enterprise-portal

# Token受众（可选，用于验证aud字段）
xxl.job.sso.jwt.audience=xxl-job-admin

# 是否开启自动创建用户（默认false）
# 如果开启，手机号不存在时自动创建用户
xxl.job.sso.auto-create-user=false

# 自动创建用户的默认角色（0-普通用户，1-管理员）
xxl.job.sso.default.role=0

# 自动创建用户的默认权限（执行器ID列表，逗号分隔）
xxl.job.sso.default.permission=

# ==================== 会话状态一致性配置 ====================
# 企业运营后台SSO入口地址（会话过期时自动重定向到此地址）
# 当xxl-job-admin会话过期时，如果配置了此项且开启了auto-redirect，
# 会自动跳转到运营后台进行SSO验证，实现无缝续期
# 示例: http://portal.example.com/api/sso/redirect-to-xxl-job
xxl.job.sso.server-url=

# 是否开启自动重定向到运营后台（默认false）
# true=会话过期时自动跳转回运营后台进行SSO验证
# false=会话过期时显示登录页面
xxl.job.sso.auto-redirect=false
```

#### 4.2.2 用户预创建流程

由于安全考虑，建议采用**预创建用户**方式，而非自动创建：

```
┌──────────────────────────────────────────────────────────────┐
│                    用户管理流程                                 │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  方式1：管理员预创建（推荐）                                    │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    │
│  │运营后台管理员 │───>│ 在xxl-job-  │───>│ 用户输入手机号 │    │
│  │             │    │ admin创建用户 │    │ 设置角色权限  │    │
│  └─────────────┘    └─────────────┘    └─────────────┘    │
│                                                              │
│  方式2：自动创建（需谨慎开启）                                  │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    │
│  │ 用户SSO登录  │───>│ 手机号不存在  │───>│ 自动创建用户  │    │
│  │             │    │             │    │ 使用默认角色  │    │
│  └─────────────┘    └─────────────┘    └─────────────┘    │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

## 5. 安全评估

### 5.1 安全威胁分析

| 威胁类型 | 风险等级 | 描述 |
|---------|---------|------|
| Token 泄露 | 高 | Token被截获后可被冒用 |
| 重放攻击 | 中 | 攻击者使用截获的Token重复登录 |
| 签名伪造 | 高 | 攻击者伪造有效Token |
| 用户越权 | 中 | 用户访问未授权的执行器 |
| 中间人攻击 | 高 | 通信过程中数据被篡改 |

### 5.2 安全防护措施

#### 5.2.1 传输安全

```
✅ 强制要求：
   - 生产环境必须使用 HTTPS
   - Cookie 设置 Secure 属性
   - Cookie 设置 HttpOnly 属性
   - Cookie 设置 SameSite=Strict 或 Lax

配置示例：
server.servlet.session.cookie.secure=true
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.same-site=strict
```

#### 5.2.2 Token 安全

| 防护措施 | 实现方式 | 说明 |
|---------|---------|------|
| 签名验证 | HS256 算法 | 确保Token完整性和真实性 |
| 过期时间 | exp 声明 | 默认为5分钟，最长不超过30分钟 |
| 签发时间 | iat 声明 | 可配置最大时间窗口 |
| 唯一标识 | jti 声明 | 可选，用于记录已使用的Token |
| 密钥强度 | 至少256位 | 使用足够长度的随机字符串 |

#### 5.2.3 代码层面安全实现

```java
public class SsoAuthService {
    
    /**
     * 验证Token并获取手机号
     */
    public String validateToken(String token) {
        try {
            // 1. 解析并验证签名
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            // 2. 检查过期时间（JWT库自动验证exp）
            
            // 3. 额外检查：签发时间不能太久远（防止重放）
            Date issuedAt = claims.getIssuedAt();
            if (System.currentTimeMillis() - issuedAt.getTime() > maxExpirationMs) {
                throw new SsoException("Token 已过期");
            }
            
            // 4. （可选）验证签发者
            if (StringUtils.hasText(allowedIssuer)) {
                if (!allowedIssuer.equals(claims.getIssuer())) {
                    throw new SsoException("Token 签发者无效");
                }
            }
            
            // 5. 获取手机号
            String phone = claims.get("phone", String.class);
            if (phone == null) {
                phone = claims.getSubject();
            }
            
            if (!isValidPhone(phone)) {
                throw new SsoException("手机号格式无效");
            }
            
            return phone;
            
        } catch (ExpiredJwtException e) {
            throw new SsoException("Token 已过期", e);
        } catch (JwtException e) {
            throw new SsoException("Token 验证失败", e);
        }
    }
    
    /**
     * 手机号格式验证
     */
    private boolean isValidPhone(String phone) {
        if (phone == null) return false;
        // 中国大陆手机号格式：1开头，11位数字
        return phone.matches("^1[3-9]\\d{9}$");
    }
}
```

#### 5.2.4 用户权限控制

```
访问控制流程：
1. SSO验证通过 → 获取用户信息
2. 检查用户状态（是否被禁用）
3. 检查用户角色和权限
4. 设置登录Session

权限说明：
- role=1：管理员，可管理所有执行器
- role=0：普通用户，只能访问 permission 字段指定的执行器
```

### 5.3 安全配置建议

```properties
# 生产环境安全配置建议

# 1. 使用强密钥（至少32个字符，随机生成）
xxl.job.sso.jwt.secret=${XXL_JOB_SSO_SECRET:change-me-in-production}

# 2. 缩短Token有效期
xxl.job.sso.jwt.expiration=180

# 3. 关闭自动创建用户
xxl.job.sso.auto-create-user=false

# 4. 限制来源IP（可选，通过Nginx或防火墙实现）
# 建议只允许运营后台服务器IP访问SSO端点

# 5. Cookie安全配置
server.servlet.session.cookie.secure=true
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.same-site=strict
```

---

## 6. 错误码定义

| 错误码 | 错误信息 | 说明 |
|--------|---------|------|
| SSO001 | Token 不能为空 | 请求参数缺失 |
| SSO002 | Token 格式无效 | 无法解析为JWT格式 |
| SSO003 | Token 签名验证失败 | 密钥不匹配或被篡改 |
| SSO004 | Token 已过期 | 超过有效期 |
| SSO005 | Token 签发时间无效 | 时间戳异常 |
| SSO006 | 手机号格式无效 | 不符合手机号规范 |
| SSO007 | 用户不存在 | 该手机号未在系统中注册 |
| SSO008 | 用户已被禁用 | 账号状态异常 |
| SSO009 | 系统异常 | 内部错误 |

---

## 6. 会话状态一致性方案

### 6.1 问题分析

**问题场景：**
1. 用户在运营后台点击"任务调度"，通过SSO跳转到xxl-job-admin
2. xxl-job-admin验证JWT成功，设置自己的会话状态（xxl-sso的Cookie）
3. 过了一段时间，xxl-job-admin的会话过期（由`xxl-sso.token.timeout`控制，默认7天）
4. 用户再次访问xxl-job-admin页面，被拦截器跳转到`/auth/login`
5. 但此时用户在运营后台仍然是登录状态

**状态不一致的表现：**
- 运营后台：用户已登录
- xxl-job-admin：会话过期，需要重新登录

### 6.2 解决方案

实现**自动重定向回运营后台**的机制：

```
┌─────────────────────────────────────────────────────────────────────┐
│                    会话状态一致性方案                                   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  用户访问xxl-job-admin页面                                           │
│       │                                                             │
│       ▼                                                             │
│  ┌─────────────────┐                                                │
│  │会话是否已过期？  │                                                │
│  └────────┬────────┘                                                │
│           │否                                                        │
│           ▼                                                          │
│      正常访问页面                                                    │
│           │                                                          │
│           │是                                                        │
│           ▼                                                          │
│  ┌─────────────────┐                                                │
│  │SSO自动重定向     │                                                │
│  │是否已开启？      │                                                │
│  └────────┬────────┘                                                │
│           │否                                                        │
│           ▼                                                          │
│      显示登录页面                                                    │
│           │                                                          │
│           │是                                                        │
│           ▼                                                          │
│  ┌──────────────────────────────────────────────┐                  │
│  │  重定向到运营后台SSO入口                        │                  │
│  │  {sso.server-url}?redirect={目标页面}         │                  │
│  └──────────────────────┬───────────────────────┘                  │
│                         │                                            │
│                         ▼                                            │
│  ┌──────────────────────────────────────────────┐                  │
│  │  运营后台检测用户登录状态                       │                  │
│  │  - 已登录：生成JWT，跳转回/auth/sso            │                  │
│  │  - 未登录：跳转到运营后台登录页                 │                  │
│  └──────────────────────┬───────────────────────┘                  │
│                         │                                            │
│                         ▼ (已登录)                                   │
│  ┌──────────────────────────────────────────────┐                  │
│  │  跳转回xxl-job-admin的/auth/sso端点            │                  │
│  │  验证JWT → 设置新会话 → 跳转回目标页面          │                  │
│  └──────────────────────────────────────────────┘                  │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 6.3 完整时序图

```
┌─────────┐         ┌──────────────┐         ┌──────────────┐
│  用户   │         │ 企业运营后台   │         │ xxl-job-admin│
└────┬────┘         └──────┬───────┘         └──────┬───────┘
     │                     │                        │
     │  1. 第一次SSO登录    │                        │
     │────────────────────>│                        │
     │                     │                        │
     │  2. 跳转回/auth/sso  │                        │
     │<─────────────────────────────────────────────│
     │                     │                        │
     │  ... (一段时间后) ...│                        │
     │                     │                        │
     │  3. 访问任务管理页面 │                        │
     │─────────────────────────────────────────────>│
     │                     │                        │
     │                     │                        │ 4. 会话过期
     │                     │                        │    拦截器跳转
     │                     │                        │
     │  5. 302重定向       │                        │
     │<─────────────────────────────────────────────│
     │  Location: /auth/login                        │
     │                     │                        │
     │  6. 访问/login      │                        │
     │─────────────────────────────────────────────>│
     │                     │                        │
     │                     │                        │ 7. 检测SSO配置
     │                     │                        │    autoRedirect=true
     │                     │                        │
     │  8. 302重定向       │                        │
     │<─────────────────────────────────────────────│
     │  Location: {sso.server-url}                  │
     │  ?redirect=/xxl-job-admin/                   │
     │                     │                        │
     │  9. 访问运营后台    │                        │
     │────────────────────>│                        │
     │                     │                        │
     │                     │ 10. 检测用户已登录      │
     │                     │     生成新JWT Token     │
     │                     │                        │
     │  11. 302重定向       │                        │
     │<────────────────────│                        │
     │  Location: /auth/sso?token=xxx               │
     │                     │                        │
     │  12. 访问/auth/sso  │                        │
     │─────────────────────────────────────────────>│
     │                     │                        │
     │                     │                        │ 13. 验证JWT
     │                     │                        │    设置新会话
     │                     │                        │
     │  14. 302重定向       │                        │
     │<─────────────────────────────────────────────│
     │  Location: /xxl-job-admin/                   │
     │                     │                        │
     │  15. 正常访问页面    │                        │
     │─────────────────────────────────────────────>│
```

### 6.4 新增配置项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `xxl.job.sso.server-url` | String | 空 | 企业运营后台的SSO入口地址 |
| `xxl.job.sso.auto-redirect` | boolean | false | 是否开启自动重定向 |

**配置示例：**

```properties
# 企业运营后台SSO入口地址
# 当xxl-job-admin会话过期时，会自动重定向到此地址
xxl.job.sso.server-url=http://portal.example.com/api/sso/redirect-to-xxl-job

# 开启自动重定向
xxl.job.sso.auto-redirect=true
```

### 6.5 企业运营后台需要配合实现

运营后台需要提供一个SSO入口接口，处理来自xxl-job-admin的重定向请求：

```java
/**
 * 运营后台SSO入口
 * 接收xxl-job-admin的重定向请求，验证用户登录状态后生成JWT跳转回去
 */
@GetMapping("/api/sso/redirect-to-xxl-job")
public void redirectToXxlJob(HttpServletRequest request,
                               HttpServletResponse response,
                               @RequestParam(value = "redirect", required = false) String redirect)
        throws IOException {
    
    // 1. 检查当前用户是否已登录
    User currentUser = getCurrentUser(request);
    
    if (currentUser == null) {
        // 2. 用户未登录，跳转到运营后台登录页
        String loginUrl = buildLoginUrlWithRedirect(request.getRequestURL().toString());
        response.sendRedirect(loginUrl);
        return;
    }
    
    // 3. 用户已登录，生成JWT Token
    String token = JwtTokenGenerator.generateToken(currentUser.getPhone());
    
    // 4. 构建跳转回xxl-job-admin的URL
    String xxlJobAdminBaseUrl = "http://xxl-job-server/xxl-job-admin";
    String ssoUrl = xxlJobAdminBaseUrl + "/auth/sso?token=" + token;
    
    if (StringTool.isNotBlank(redirect)) {
        ssoUrl += "&redirect=" + URLEncoder.encode(redirect, StandardCharsets.UTF_8);
    }
    
    // 5. 跳转回xxl-job-admin
    response.sendRedirect(ssoUrl);
}
```

### 6.6 用户体验说明

**开启自动重定向后的用户体验：**

1. **用户感知**：整个过程对用户透明，用户感觉不到多次跳转
2. **浏览器行为**：多次302重定向在网络层完成，用户只看到最终页面
3. **登录状态**：两个系统的登录状态保持一致
   - 运营后台登录 → xxl-job-admin自动登录
   - 运营后台登出 → xxl-job-admin会话过期后需要重新登录

**未开启自动重定向的情况：**

1. xxl-job-admin会话过期后，用户会看到登录页面
2. 用户需要手动返回运营后台，重新点击"任务调度"链接

---

## 7. 测试方案

### 7.1 测试场景

| 场景编号 | 测试场景 | 预期结果 |
|---------|---------|---------|
| TC01 | 正常SSO登录 | 成功跳转到首页，已登录状态 |
| TC02 | Token为空 | 重定向到登录页，提示参数错误 |
| TC03 | Token格式错误 | 重定向到登录页，提示Token无效 |
| TC04 | Token签名错误 | 重定向到登录页，提示Token无效 |
| TC05 | Token已过期 | 重定向到登录页，提示Token已过期 |
| TC06 | 用户不存在 | 重定向到登录页，提示用户未授权 |
| TC07 | 使用已使用过的Token | （如开启jti验证）拒绝访问 |
| TC08 | redirect参数校验 | 只能跳转站内路径 |

### 7.2 测试Token生成示例

```java
// 测试用：生成一个有效的Token
public class TestTokenGenerator {
    public static void main(String[] args) {
        String secret = "xxl-job-sso-secret-key-2024-minimum-256-bits-required";
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        
        String token = Jwts.builder()
                .claim("phone", "13800138000")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 300_000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
        
        System.out.println("Test Token: " + token);
        System.out.println("Test URL: http://localhost:8080/xxl-job-admin/auth/sso?token=" + token);
    }
}
```

---

## 8. 运维说明

### 8.1 日志配置

系统会记录SSO相关日志，便于排查问题：

```
日志格式：
[SSO] [操作] [结果] - 详情

示例：
[SSO] [LOGIN_ATTEMPT] [SUCCESS] - phone=13800138000, ip=192.168.1.100
[SSO] [LOGIN_ATTEMPT] [FAIL] - error=Token已过期, ip=192.168.1.101
```

### 8.2 监控指标

建议监控以下指标：

| 指标名 | 说明 | 告警阈值 |
|--------|------|---------|
| sso_login_total | SSO登录总数 | - |
| sso_login_success | SSO登录成功数 | - |
| sso_login_fail | SSO登录失败数 | 持续增长时告警 |
| sso_token_invalid | 无效Token数 | 突增时告警 |

---

## 9. 附录

### 9.1 依赖库

xxl-job-admin 需要添加 JWT 依赖：

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.5</version>
    <scope>runtime</scope>
</dependency>
```

### 9.2 数据库表结构

现有 `xxl_job_user` 表结构无需修改：

```sql
CREATE TABLE `xxl_job_user` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL COMMENT '账号',
  `password` varchar(50) NOT NULL COMMENT '密码',
  `role` tinyint(4) NOT NULL COMMENT '角色：0-普通用户、1-管理员',
  `permission` varchar(255) DEFAULT NULL COMMENT '权限：执行器ID列表，多个逗号分割',
  `token` varchar(50) DEFAULT NULL COMMENT '登录token',
  PRIMARY KEY (`id`),
  UNIQUE KEY `i_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 9.3 参考链接

- JWT 官方文档: https://jwt.io/
- jjwt GitHub: https://github.com/jwtk/jjwt
- XXL-JOB 官方文档: https://www.xuxueli.com/xxl-job/
