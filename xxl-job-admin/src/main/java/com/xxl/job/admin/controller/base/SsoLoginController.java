package com.xxl.job.admin.controller.base;

import com.xxl.job.admin.config.XxlJobSsoProperties;
import com.xxl.job.admin.mapper.XxlJobUserMapper;
import com.xxl.job.admin.model.XxlJobUser;
import com.xxl.job.admin.service.SsoService;
import com.xxl.job.admin.service.impl.SsoServiceImpl;
import com.xxl.job.admin.service.token.TokenService;
import com.xxl.job.admin.service.token.TokenService.TokenPair;
import com.xxl.job.admin.util.CryptoUtil;
import com.xxl.job.admin.util.TokenCookieUtil;
import com.xxl.sso.core.annotation.XxlSso;
import com.xxl.sso.core.helper.XxlSsoHelper;
import com.xxl.sso.core.model.LoginInfo;
import com.xxl.tool.core.StringTool;
import com.xxl.tool.id.UUIDTool;
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
 * 双Token机制：
 * - Access Token：短期有效（默认30分钟），JWT格式
 * - Refresh Token：长期有效（默认7天），AES加密存储
 * 
 * 端点说明：
 * - /auth/sso/login：SSO登录入口（来自企业运营后台）
 * - /auth/sso/redirect：Token过期重定向端点（xxl-sso配置）
 * - /auth/sso/refresh：Token刷新端点（内部使用）
 * 
 * 安全特性：
 * 1. Refresh Token自动刷新机制
 * 2. State参数防止CSRF攻击
 * 3. 只有Refresh Token失效时才重定向到企业运营后台
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

    @Resource
    private TokenService tokenService;

    @Resource
    private XxlJobUserMapper xxlJobUserMapper;

    /**
     * SSO单点登录入口
     * 
     * 处理来自企业运营后台的单点登录请求。
     * 
     * 处理流程：
     * 1. 验证来自企业运营后台的SSO Token
     * 2. 根据手机号查询用户
     * 3. 创建xxl-sso会话（兼容现有机制）
     * 4. 生成双Token（Access Token + Refresh Token）
     * 5. 将Refresh Token存入加密Cookie
     * 6. 重定向到目标页面
     * 
     * 端点：GET /auth/sso/login
     * 
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @param token SSO Token（来自企业运营后台）
     * @param state State参数（防CSRF，可选）
     * @param redirectUrl 登录成功后跳转的目标URL
     * @return RedirectView 重定向视图
     */
    @RequestMapping("/sso/login")
    @XxlSso(login = false)
    public RedirectView ssoLogin(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(value = "token", required = false) String token,
            @RequestParam(value = "state", required = false) String state,
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

        XxlJobUser user = xxlJobUserMapper.loadByUserName(phone);
        if (user == null) {
            logger.warn("SSO login failed: user not found for phone: {}", maskPhone(phone));
            return createRedirectView(getLoginUrl());
        }

        try {
            createXxlSsoSession(user, response);

            Response<TokenPair> tokenPairResult = tokenService.generateTokenPair(
                    String.valueOf(user.getId()),
                    phone
            );

            if (tokenPairResult.isSuccess()) {
                TokenPair tokenPair = tokenPairResult.getData();
                boolean isSecure = TokenCookieUtil.shouldUseSecure(request);
                String contextPath = request.getContextPath();
                
                TokenCookieUtil.setRefreshTokenCookie(
                        response,
                        tokenPair.refreshToken(),
                        xxlJobSsoProperties.getRefreshTokenExpireSeconds(),
                        isSecure,
                        contextPath
                );
                
                logger.info("Generated token pair for user: {}, phone: {}", 
                        user.getId(), maskPhone(phone));
            }

            String targetUrl = validateRedirectUrl(redirectUrl, request);
            logger.info("SSO login success for phone: {}, redirect to: {}", maskPhone(phone), targetUrl);
            return createRedirectView(targetUrl);

        } catch (Exception e) {
            logger.error("SSO login error", e);
            return createRedirectView(getLoginUrl());
        }
    }

    /**
     * SSO会话过期重定向端点
     * 
     * 这是xxl-sso配置的login.path，当Token过期时会重定向到此端点。
     * 
     * 【安全改进】
     * 之前的问题：直接重定向到企业运营后台，运营后台无法验证请求合法性。
     * 
     * 新的处理流程：
     * 1. 检查是否存在有效的Refresh Token
     * 2. 如果存在，尝试自动刷新Access Token（用户无感知）
     * 3. 刷新成功 → 重定向回原请求URL
     * 4. 刷新失败或无Refresh Token → 携带State参数重定向到企业运营后台
     * 
     * State参数的作用：
     * - 防止CSRF攻击
     * - 包含时间戳和签名，企业运营后台可以验证
     * 
     * 端点：GET /auth/sso/redirect
     * 
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @return RedirectView 重定向视图
     */
    @RequestMapping("/sso/redirect")
    @XxlSso(login = false)
    public RedirectView ssoRedirect(
            HttpServletRequest request,
            HttpServletResponse response) {

        if (xxlJobSsoProperties.isEnabled()) {
            String refreshToken = TokenCookieUtil.getRefreshToken(request);
            
            if (StringTool.isNotBlank(refreshToken)) {
                logger.debug("Found refresh token, attempting to refresh");
                
                Response<TokenPair> refreshResult = tokenService.refreshAccessToken(refreshToken);
                
                if (refreshResult.isSuccess()) {
                    TokenPair tokenPair = refreshResult.getData();
                    boolean isSecure = TokenCookieUtil.shouldUseSecure(request);
                    String contextPath = request.getContextPath();
                    
                    TokenCookieUtil.setRefreshTokenCookie(
                            response,
                            tokenPair.refreshToken(),
                            xxlJobSsoProperties.getRefreshTokenExpireSeconds(),
                            isSecure,
                            contextPath
                    );
                    
                    XxlJobUser user = xxlJobUserMapper.loadByUserName(
                            tokenPair.refreshTokenInfo().getPhone()
                    );
                    if (user != null) {
                        createXxlSsoSession(user, response);
                    }
                    
                    String originalUrl = getOriginalRequestUrl(request);
                    logger.info("Token refreshed successfully, redirect to: {}", originalUrl);
                    return createRedirectView(originalUrl);
                } else {
                    logger.warn("Refresh token failed: {}", refreshResult.getMsg());
                    TokenCookieUtil.deleteRefreshTokenCookie(response, request.getContextPath());
                }
            }

            String enterpriseSsoUrl = xxlJobSsoProperties.getEnterpriseSsoUrl();
            if (StringTool.isNotBlank(enterpriseSsoUrl)) {
                String state = CryptoUtil.generateState(xxlJobSsoProperties.getSecret());
                String redirectBackUrl = getCurrentFullUrl(request);
                
                String redirectUrl = buildEnterpriseSsoRedirectUrl(
                        enterpriseSsoUrl, 
                        redirectBackUrl, 
                        state
                );
                
                logger.info("SSO session expired, redirect to enterprise SSO with state: {}", 
                        state.substring(0, Math.min(20, state.length())) + "...");
                
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
     * 获取原始请求URL
     * 
     * 当xxl-sso拦截器重定向到此端点时，
     * 原始请求URL通过xxl-sso的机制传递。
     * 这里尝试从request中获取原始URL。
     * 
     * @param request HTTP请求对象
     * @return 原始请求URL，获取失败返回首页"/"
     */
    private String getOriginalRequestUrl(HttpServletRequest request) {
        String redirectUri = request.getParameter("redirect_uri");
        if (StringTool.isNotBlank(redirectUri)) {
            return redirectUri;
        }
        
        String referer = request.getHeader("Referer");
        if (StringTool.isNotBlank(referer)) {
            if (isSameOrigin(referer, request)) {
                return referer;
            }
        }
        
        return "/";
    }

    /**
     * 检查URL是否同源
     * 
     * @param url 待检查的URL
     * @param request HTTP请求对象
     * @return true表示同源
     */
    private boolean isSameOrigin(String url, HttpServletRequest request) {
        if (url == null) {
            return false;
        }
        
        String requestServer = request.getScheme() + "://" + request.getServerName();
        int port = request.getServerPort();
        if (port != 80 && port != 443) {
            requestServer += ":" + port;
        }
        
        return url.startsWith(requestServer) || url.startsWith("/");
    }

    /**
     * 创建xxl-sso会话
     * 
     * 兼容现有的xxl-sso机制，确保与现有系统无缝集成。
     * 
     * @param user 用户对象
     * @param response HTTP响应对象
     */
    private void createXxlSsoSession(XxlJobUser user, HttpServletResponse response) {
        LoginInfo loginInfo = new LoginInfo(
                String.valueOf(user.getId()),
                UUIDTool.getSimpleUUID()
        );
        
        XxlSsoHelper.loginWithCookie(loginInfo, response, false);
    }

    /**
     * 获取登录失败时的跳转URL
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
     * 防止开放重定向攻击。
     * 
     * @param redirectUrl 待验证的重定向URL
     * @param request HTTP请求对象
     * @return 验证通过的URL，非法时返回默认首页"/"
     */
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

    /**
     * 获取当前请求的完整URL
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
     * 【安全改进】
     * 添加State参数，防止CSRF攻击。
     * 
     * State参数格式：
     * random:timestamp:HMAC-SHA256签名
     * 
     * 企业运营后台可以通过以下方式验证State：
     * 1. 验证HMAC-SHA256签名
     * 2. 验证时间戳是否在有效期内
     * 
     * @param enterpriseSsoUrl 企业运营后台SSO入口URL
     * @param redirectBackUrl 登录成功后跳转回xxl-job-admin的URL
     * @param state State参数（防CSRF）
     * @return 完整的重定向URL
     */
    private String buildEnterpriseSsoRedirectUrl(
            String enterpriseSsoUrl, 
            String redirectBackUrl, 
            String state) {
        try {
            StringBuilder urlBuilder = new StringBuilder(enterpriseSsoUrl);
            String separator = enterpriseSsoUrl.contains("?") ? "&" : "?";
            
            urlBuilder.append(separator)
                    .append("redirect_url=")
                    .append(java.net.URLEncoder.encode(redirectBackUrl, "UTF-8"));
            
            if (state != null) {
                urlBuilder.append("&state=")
                        .append(java.net.URLEncoder.encode(state, "UTF-8"));
            }
            
            return urlBuilder.toString();
        } catch (Exception e) {
            logger.warn("Failed to build enterprise SSO redirect URL", e);
            return enterpriseSsoUrl;
        }
    }

    /**
     * 创建重定向视图
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

    /**
     * 手机号脱敏
     * 
     * 用于日志记录，保护用户隐私。
     * 
     * @param phone 原始手机号
     * @return 脱敏后的手机号
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 11) {
            return "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
