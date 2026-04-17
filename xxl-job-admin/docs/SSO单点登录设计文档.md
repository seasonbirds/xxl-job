# xxl-job-admin 单点登录（SSO）设计文档

---

## 一、背景与目标

### 1.1 业务背景

xxl-job-admin作为企业运营后台的子系统，需要实现单点登录功能。用户在企业运营后台通过点击链接即可自动登录到xxl-job-admin，无需再次输入用户名密码。

### 1.2 用户关联机制

| 系统 | 用户标识 | 关联方式 |
|------|----------|----------|
| 企业运营后台 | 手机号 | 作为用户唯一标识 |
| xxl-job-admin | username | 运营限制添加的用户账号必须是手机号 |

两个系统通过 **手机号 = username** 进行用户关联。

---

## 二、实现原理

### 2.1 Token格式

**Token结构**：
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

**Payload字段说明**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `phone` | String | 是 | 用户手机号 |
| `timestamp` | Long | 是 | Token生成时间戳（毫秒） |
| `nonce` | String | 否 | 随机字符串 |

### 2.2 签名算法

- **算法**：HMAC-SHA256
- **签名输入**：Base64Url编码的Payload
- **签名输出**：Base64Url编码（无填充）的HMAC-SHA256值

### 2.3 验证流程

```
1. 分割Token为[Payload, Signature]两部分
2. 使用预共享密钥计算期望签名
3. 常量时间比较签名是否一致（防止时序攻击）
4. Base64Url解码Payload，解析JSON
5. 验证时间戳是否在有效期内（默认5分钟）
```

---

## 三、系统交互

### 3.1 完整登录流程

```
┌──────────┐         ┌─────────────────┐         ┌─────────────────────┐
│   用户    │         │  企业运营后台      │         │    xxl-job-admin    │
└────┬─────┘         └────────┬────────┘         └──────────┬──────────┘
     │                        │                               │
     │──点击"任务调度"链接────>│                               │
     │                        │                               │
     │                        │──构造Payload{phone,timestamp} │
     │                        │──Base64Url编码                │
     │                        │──计算HMAC-SHA256签名         │
     │                        │──拼接Token: payload.signature │
     │                        │                               │
     │<──302重定向────────────│                               │
     │  Location: /xxl-job-   │                               │
     │  admin/auth/sso/login  │                               │
     │  ?token=xxx             │                               │
     │                        │                               │
     │──────────────────────────────────────────────────────>│
     │                        │                               │
     │                        │                               │──验证SSO是否启用
     │                        │                               │──验证Token签名
     │                        │                               │──验证时间戳
     │                        │                               │──根据手机号查询用户
     │                        │                               │──创建登录会话
     │                        │                               │
     │<──────────────────────────────────────────────────────│
     │  302重定向到首页        │                               │
     │  Set-Cookie: 登录Token  │                               │
     │                        │                               │
```

### 3.2 Token过期状态同步流程

**问题**：当xxl-job-admin的Token过期时，直接跳转到本地登录页面会造成两边系统登录状态不一致。

**解决方案**：配置企业运营后台的SSO入口URL，Token过期时自动重定向到企业运营后台重新登录。

```
xxl-job-admin Token过期
       │
       ▼
xxl-sso拦截器重定向到 /auth/sso/redirect
       │
       ▼
判断SSO是否启用 && 配置了enterpriseSsoUrl
       │
       ├───是───> 重定向到企业运营后台SSO入口
       │              │
       │              ▼
       │         企业运营后台检测用户登录状态
       │              │
       │              ├───用户已登录───> 重新生成Token，跳转回xxl-job-admin
       │              │
       │              └───用户未登录───> 跳转到企业运营后台登录页面
       │
       └───否───> 重定向到xxl-job-admin本地登录页面
```

---

## 四、系统对接

### 4.1 配置参数

#### xxl-job-admin配置

```properties
### xxl-sso 原有配置（需修改）
xxl-sso.client.excluded.paths=/auth/sso/login,/auth/sso/redirect
xxl-sso.client.login.path=/auth/sso/redirect

### xxl-job-admin SSO 配置
xxl.job.sso.enabled=true
xxl.job.sso.secret=your-32-character-strong-secret-key
xxl.job.sso.token.expire.seconds=300
xxl.job.sso.login.failure.url=/auth/login
xxl.job.sso.enterprise.sso.url=http://enterprise-host:port/enterprise/sso/xxl-job-redirect
```

**配置说明**：

| 配置项 | 说明 |
|--------|------|
| `xxl-sso.client.login.path` | Token过期时重定向的端点，配置为 `/auth/sso/redirect` 实现状态同步 |
| `xxl.job.sso.enterprise.sso.url` | 企业运营后台的SSO入口URL，Token过期时自动重定向到此地址 |

#### 企业运营后台配置

```properties
xxl.job.sso.admin.url=http://localhost:8080/xxl-job-admin
xxl.job.sso.secret=your-32-character-strong-secret-key
xxl.job.sso.token.expire.seconds=300
```

### 4.2 Token生成示例（企业运营后台）

```java
import com.xxl.tool.json.GsonTool;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class XxlJobSsoTokenGenerator {

    private static final String HMAC_SHA256 = "HmacSHA256";

    public static String generateToken(String phone, String secret) {
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
}
```

### 4.3 构建SSO登录URL

```java
public String buildSsoLoginUrl(String phone, String secret, 
                                 String adminUrl, String redirectUrl) {
    String token = XxlJobSsoTokenGenerator.generateToken(phone, secret);
    
    return adminUrl + "/auth/sso/login" +
           "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8) +
           "&redirect_url=" + URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8);
}
```

---

## 五、安全评估

### 5.1 安全机制

| 安全项 | 实现方案 |
|--------|----------|
| 完整性保护 | HMAC-SHA256签名，防止Token篡改 |
| 时效性保护 | Token默认5分钟过期，时间戳验证 |
| 重放攻击防护 | 短有效期 + 时间戳验证 |
| 时序攻击防护 | 常量时间比较签名 |
| 开放重定向防护 | 只允许跳转到同源URL |
| 状态同步 | Token过期时自动重定向到企业运营后台 |

### 5.2 风险与缓解

| 风险 | 缓解措施 |
|------|----------|
| Token泄露 | 短有效期（5分钟）+ HTTPS强制 |
| 密钥泄露 | 环境变量注入 + 定期轮换 + 强密钥（至少32字符） |
| 重放攻击 | 短有效期 + 时间戳验证 |

### 5.3 生产环境强制要求

- [ ] **HTTPS**：所有SSO相关通信必须使用HTTPS
- [ ] **强密钥**：密钥至少32个随机字符
- [ ] **环境变量**：密钥通过环境变量注入，不硬编码
- [ ] **日志保护**：不记录完整的Token

---

## 六、实现方案

### 6.1 文件结构

```
xxl-job-admin/src/main/java/com/xxl/job/admin/
├── config/
│   └── XxlJobSsoProperties.java          # SSO配置属性
├── controller/base/
│   └── SsoLoginController.java            # SSO控制器
│       ├── /auth/sso/login                # SSO登录入口
│       └── /auth/sso/redirect             # Token过期重定向端点
└── service/
    ├── SsoService.java                    # SSO服务接口
    └── impl/
        └── SsoServiceImpl.java             # SSO服务实现
```

### 6.2 核心端点说明

| 端点 | 方法 | 说明 |
|------|------|------|
| `/auth/sso/login` | GET | SSO登录入口，接收Token并自动登录 |
| `/auth/sso/redirect` | GET | Token过期重定向端点，实现状态同步 |

### 6.3 配置变更

**新增配置**：
```properties
xxl.job.sso.enabled=true
xxl.job.sso.secret=your-32-character-strong-secret-key
xxl.job.sso.token.expire.seconds=300
xxl.job.sso.login.failure.url=/auth/login
xxl.job.sso.enterprise.sso.url=http://enterprise-host:port/enterprise/sso/xxl-job-redirect
```

**修改配置**：
```properties
xxl-sso.client.excluded.paths=/auth/sso/login,/auth/sso/redirect
xxl-sso.client.login.path=/auth/sso/redirect
```

---

## 七、部署与运维

### 7.1 启用SSO

1. 修改 `application.properties`：
   ```properties
   xxl.job.sso.enabled=true
   xxl.job.sso.secret=your-32-character-strong-secret-key
   xxl.job.sso.enterprise.sso.url=http://enterprise-host:port/enterprise/sso/xxl-job-redirect
   ```

2. 确保企业运营后台配置相同的密钥

3. 确保xxl-job-admin中已存在对应用户（username为手机号）

### 7.2 回滚方案

如需快速禁用SSO功能：
```properties
xxl.job.sso.enabled=false
```

用户将通过传统的用户名密码方式登录。

### 7.3 状态同步配置说明

**配置 `xxl.job.sso.enterprise.sso.url` 的作用**：

1. **Token过期时**：xxl-job-admin不会直接跳转到本地登录页面，而是重定向到企业运营后台的SSO入口
2. **企业运营后台处理**：检测用户登录状态，如果用户仍在登录状态，重新生成Token并跳转回xxl-job-admin
3. **用户体验**：用户无需感知Token过期过程，实现无缝状态同步

**不配置的情况**：

如果不配置 `enterprise.sso.url`，Token过期时将跳转到xxl-job-admin本地登录页面，可能造成两边系统登录状态不一致。

---

**文档版本**：v2.0  
**最后更新**：2026-04-17
