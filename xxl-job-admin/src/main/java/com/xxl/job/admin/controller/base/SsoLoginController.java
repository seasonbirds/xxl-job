package com.xxl.job.admin.controller.base;

import com.xxl.job.admin.config.XxlJobSsoProperties;
import com.xxl.job.admin.service.SsoService;
import com.xxl.job.admin.service.impl.SsoServiceImpl;
import com.xxl.sso.core.annotation.XxlSso;
import com.xxl.tool.core.StringTool;
import com.xxl.tool.response.Response;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

/**
 * SSO单点登录控制器
 * 
 * 提供企业运营后台与xxl-job-admin的单点登录对接端点。
 * 
 * 使用方式：
 * 企业运营后台生成SSO Token后，通过以下URL跳转：
 * GET /auth/sso/login?token=<SSO_TOKEN>&redirect_url=<目标页面>
 * 
 * 安全特性：
 * 1. Token验证：验证HMAC-SHA256签名和时效性
 * 2. 重定向保护：只允许跳转到同源URL，防止开放重定向攻击
 * 3. 失败处理：验证失败时自动跳转到传统登录页面
 * 
 * @author xxl-job
 */
@Controller
@RequestMapping("/auth")
public class SsoLoginController {

    private static final Logger logger = LoggerFactory.getLogger(SsoLoginController.class);

    @Resource
    private XxlJobSsoProperties xxlJobSsoProperties;

    @Resource
    private SsoServiceImpl ssoServiceImpl;

    /**
     * SSO单点登录入口
     * 
     * 处理来自企业运营后台的单点登录请求，完成以下步骤：
     * 1. 检查SSO功能是否启用
     * 2. 验证SSO Token的有效性（签名、时效性）
     * 3. 根据Token中的手机号查询用户
     * 4. 自动创建登录会话（写入Cookie和数据库）
     * 5. 重定向到目标页面
     * 
     * 端点：GET /auth/sso/login
     * 
     * @param request HTTP请求对象
     * @param response HTTP响应对象，用于写入登录Cookie
     * @param token SSO Token（来自企业运营后台），格式：Base64Url(Payload).HMAC-SHA256签名
     * @param redirectUrl 登录成功后跳转的目标URL（可选，默认为首页"/"）
     * @return RedirectView 重定向视图，成功时跳转到目标页面，失败时跳转到登录页面
     */
    @RequestMapping("/sso/login")
    @XxlSso(login = false)
    public RedirectView ssoLogin(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(value = "token", required = false) String token,
            @RequestParam(value = "redirect_url", required = false) String redirectUrl) {

        if (!xxlJobSsoProperties.isEnabled()) {
            logger.warn("SSO login attempted but SSO is not enabled");
            return createRedirectView(getLoginUrl());
        }

        if (StringTool.isBlank(token)) {
            logger.warn("SSO login attempted with empty token");
            return createRedirectView(getLoginUrl());
        }

        Response<SsoService.SsoTokenInfo> tokenValidation = ssoServiceImpl.validateToken(token);
        if (!tokenValidation.isSuccess()) {
            logger.warn("SSO token validation failed: {}", tokenValidation.getMsg());
            return createRedirectView(getLoginUrl());
        }

        SsoService.SsoTokenInfo tokenInfo = tokenValidation.getData();
        String phone = tokenInfo.phone();

        Response<Boolean> loginResult = ssoServiceImpl.doLoginWithResponse(phone, response);
        if (!loginResult.isSuccess()) {
            logger.warn("SSO auto login failed for phone {}: {}", phone, loginResult.getMsg());
            return createRedirectView(getLoginUrl());
        }

        String targetUrl = validateRedirectUrl(redirectUrl, request);
        logger.info("SSO login success for phone: {}, redirect to: {}", phone, targetUrl);
        return createRedirectView(targetUrl);
    }

    /**
     * 获取登录失败时的跳转URL
     * 
     * 优先使用配置的loginFailureUrl，如未配置则使用默认值"/auth/login"。
     * 
     * @return 登录失败跳转URL
     */
    private String getLoginUrl() {
        String failureUrl = xxlJobSsoProperties.getLoginFailureUrl();
        return (failureUrl != null && !failureUrl.isEmpty()) ? failureUrl : "/auth/login";
    }

    /**
     * 验证重定向URL的合法性
     * 
     * 防止开放重定向攻击（Open Redirect Attack），
     * 只允许跳转到以下类型的URL：
     * 1. 相对路径（以"/"开头）
     * 2. 同源的绝对路径（协议、域名、端口必须匹配）
     * 
     * 安全原理：
     * 攻击者可能构造恶意URL诱导用户跳转，例如：
     * ?redirect_url=http://evil.com/phishing
     * 通过验证URL的同源性，可以防止此类攻击。
     * 
     * @param redirectUrl 待验证的重定向URL
     * @param request HTTP请求对象，用于获取当前服务器信息
     * @return 验证通过的URL，非法时返回默认首页"/"
     */
    private String validateRedirectUrl(String redirectUrl, HttpServletRequest request) {
        if (StringTool.isBlank(redirectUrl)) {
            return "/";
        }

        if (!redirectUrl.startsWith("/") && !redirectUrl.startsWith("http://") && !redirectUrl.startsWith("https://")) {
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

    /**
     * SSO会话过期重定向端点
     * 
     * 解决Token过期时两边系统登录状态不一致的问题。
     * 
     * 工作原理：
     * 1. 当xxl-job-admin的Token过期时，xxl-sso拦截器会重定向到此端点
     * 2. 此端点判断：
     *    - 如果SSO已启用且配置了企业运营后台SSO入口URL → 重定向到企业运营后台
     *    - 否则 → 重定向到本地登录页面
     * 
     * 状态同步流程：
     * xxl-job-admin Token过期 → 重定向到此端点 → 判断是否配置企业SSO入口
     *     → 已配置 → 重定向到企业运营后台 → 企业运营后台检测用户登录状态
     *         → 用户已登录 → 重新生成Token → 跳转回xxl-job-admin（自动登录）
     *         → 用户未登录 → 跳转到企业运营后台登录页面
     *     → 未配置 → 重定向到xxl-job-admin本地登录页面
     * 
     * 端点：GET /auth/sso/redirect
     * 
     * @param request HTTP请求对象
     * @return RedirectView 重定向视图
     */
    @RequestMapping("/sso/redirect")
    @XxlSso(login = false)
    public RedirectView ssoRedirect(HttpServletRequest request) {
        if (xxlJobSsoProperties.isEnabled()) {
            String enterpriseSsoUrl = xxlJobSsoProperties.getEnterpriseSsoUrl();
            if (StringTool.isNotBlank(enterpriseSsoUrl)) {
                String currentUrl = getCurrentFullUrl(request);
                String redirectUrl = buildEnterpriseSsoRedirectUrl(enterpriseSsoUrl, currentUrl);
                
                logger.info("SSO session expired, redirect to enterprise SSO: {}", redirectUrl);
                
                RedirectView redirectView = new RedirectView();
                redirectView.setUrl(redirectUrl);
                redirectView.setContextRelative(false);
                redirectView.setExposeModelAttributes(false);
                return redirectView;
            }
        }
        
        logger.info("SSO not enabled or enterprise SSO URL not configured, redirect to local login page");
        return createRedirectView("/auth/login");
    }

    /**
     * 获取当前请求的完整URL
     * 
     * 用于构建重定向回xxl-job-admin的URL参数。
     * 
     * @param request HTTP请求对象
     * @return 当前请求的完整URL
     */
    private String getCurrentFullUrl(HttpServletRequest request) {
        StringBuffer url = request.getRequestURL();
        String queryString = request.getQueryString();
        if (queryString != null) {
            url.append("?").append(queryString);
        }
        return url.toString();
    }

    /**
     * 构建企业运营后台SSO重定向URL
     * 
     * 在企业SSO入口URL后添加redirect_url参数，
     * 让企业运营后台在完成登录后跳转回xxl-job-admin。
     * 
     * @param enterpriseSsoUrl 企业运营后台SSO入口URL
     * @param redirectUrl 登录成功后跳转回xxl-job-admin的URL
     * @return 完整的重定向URL
     */
    private String buildEnterpriseSsoRedirectUrl(String enterpriseSsoUrl, String redirectUrl) {
        try {
            String separator = enterpriseSsoUrl.contains("?") ? "&" : "?";
            String encodedRedirectUrl = java.net.URLEncoder.encode(redirectUrl, "UTF-8");
            return enterpriseSsoUrl + separator + "redirect_url=" + encodedRedirectUrl;
        } catch (Exception e) {
            logger.warn("Failed to build enterprise SSO redirect URL", e);
            return enterpriseSsoUrl;
        }
    }

    /**
     * 创建重定向视图
     * 
     * 配置RedirectView的关键属性：
     * - contextRelative: true，表示URL相对于应用上下文
     * - exposeModelAttributes: false，不暴露模型属性到URL
     * 
     * @param url 重定向目标URL
     * @return 配置好的RedirectView对象
     */
    private RedirectView createRedirectView(String url) {
        RedirectView redirectView = new RedirectView();
        redirectView.setUrl(url);
        redirectView.setContextRelative(true);
        redirectView.setExposeModelAttributes(false);
        return redirectView;
    }
}
