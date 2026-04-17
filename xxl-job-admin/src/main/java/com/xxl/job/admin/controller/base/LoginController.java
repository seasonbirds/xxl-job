package com.xxl.job.admin.controller.base;

import com.xxl.job.admin.config.SsoProperties;
import com.xxl.job.admin.exception.SsoException;
import com.xxl.job.admin.mapper.XxlJobUserMapper;
import com.xxl.job.admin.model.XxlJobUser;
import com.xxl.job.admin.service.SsoAuthService;
import com.xxl.job.admin.util.I18nUtil;
import com.xxl.sso.core.annotation.XxlSso;
import com.xxl.sso.core.helper.XxlSsoHelper;
import com.xxl.sso.core.model.LoginInfo;
import com.xxl.tool.core.StringTool;
import com.xxl.tool.crypto.Sha256Tool;
import com.xxl.tool.id.UUIDTool;
import com.xxl.tool.response.Response;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 登录控制器
 *
 * 处理用户登录、登出、密码修改以及SSO单点登录相关请求
 *
 * SSO会话状态一致性解决方案：
 * 当xxl-job-admin的会话过期时，如果配置了自动重定向，
 * 会自动跳转到企业运营后台的SSO入口，由运营后台判断用户登录状态后再跳转回来。
 *
 * 流程：
 * 1. 用户访问xxl-job-admin页面 -> 会话过期 -> 拦截器跳转到 /auth/login
 * 2. /auth/login 检测到SSO配置 -> 重定向到 {sso.server-url}?redirect={目标页面}
 * 3. 企业运营后台检测用户已登录 -> 生成JWT Token -> 跳转回 /auth/sso?token=xxx
 * 4. xxl-job-admin验证Token -> 设置新会话 -> 跳转回目标页面
 *
 * @author xuxueli 2015-12-19 16:13:16
 */
@Controller
@RequestMapping("/auth")
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Resource
    private XxlJobUserMapper xxlJobUserMapper;

    @Resource
    private SsoAuthService ssoAuthService;

    @Resource
    private SsoProperties ssoProperties;

    /**
     * 登录页面
     *
     * 如果用户已登录，直接重定向到首页
     * 如果SSO已启用且配置了自动重定向，会自动跳转到企业运营后台的SSO入口
     *
     * @param request HTTP请求
     * @param response HTTP响应
     * @param modelAndView 模型视图
     * @return 登录页面或重定向
     */
    @RequestMapping("/login")
    @XxlSso(login = false)
    public ModelAndView login(HttpServletRequest request,
                              HttpServletResponse response,
                              ModelAndView modelAndView) {

        // 检查用户是否已登录
        Response<LoginInfo> loginInfoResponse = XxlSsoHelper.loginCheckWithCookie(request, response);
        if (loginInfoResponse.isSuccess()) {
            modelAndView.setView(new RedirectView("/", true, false));
            return modelAndView;
        }

        // 检查是否需要自动重定向到企业运营后台SSO入口
        if (shouldAutoRedirectToSsoServer()) {
            String redirectUrl = buildSsoServerRedirectUrl(request);
            logger.info("[SSO] Session expired, auto redirect to SSO server: {}", redirectUrl);
            modelAndView.setView(new RedirectView(redirectUrl, true, false));
            return modelAndView;
        }

        // 显示登录页面
        return new ModelAndView("base/login");
    }

    /**
     * 执行登录
     *
     * 验证用户名和密码，成功后设置登录状态
     *
     * @param request HTTP请求
     * @param response HTTP响应
     * @param userName 用户名
     * @param password 密码
     * @param ifRemember 是否记住登录状态
     * @return 登录结果
     */
    @RequestMapping(value = "/doLogin", method = RequestMethod.POST)
    @ResponseBody
    @XxlSso(login = false)
    public Response<String> doLogin(HttpServletRequest request,
                                     HttpServletResponse response,
                                     String userName,
                                     String password,
                                     String ifRemember) {

        // 参数校验
        boolean ifRem = StringTool.isNotBlank(ifRemember) && "on".equals(ifRemember);
        if (StringTool.isBlank(userName) || StringTool.isBlank(password)) {
            return Response.ofFail(I18nUtil.getString("login_param_empty"));
        }

        // 验证用户是否存在
        XxlJobUser xxlJobUser = xxlJobUserMapper.loadByUserName(userName);
        if (xxlJobUser == null) {
            return Response.ofFail(I18nUtil.getString("login_param_unvalid"));
        }

        // 验证密码
        String passwordHash = Sha256Tool.sha256(password);
        if (!passwordHash.equals(xxlJobUser.getPassword())) {
            return Response.ofFail(I18nUtil.getString("login_param_unvalid"));
        }

        // 设置登录状态
        LoginInfo loginInfo = new LoginInfo(String.valueOf(xxlJobUser.getId()), UUIDTool.getSimpleUUID());
        Response<String> result = XxlSsoHelper.loginWithCookie(loginInfo, response, ifRem);

        return Response.of(result.getCode(), result.getMsg());
    }

    /**
     * 登出
     *
     * 清除用户的登录状态
     *
     * @param request HTTP请求
     * @param response HTTP响应
     * @return 登出结果
     */
    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    @ResponseBody
    @XxlSso(login = false)
    public Response<String> logout(HttpServletRequest request, HttpServletResponse response) {

        Response<String> result = XxlSsoHelper.logoutWithCookie(request, response);

        return Response.of(result.getCode(), result.getMsg());
    }

    /**
     * 修改密码
     *
     * 当前登录用户修改自己的密码
     *
     * @param request HTTP请求
     * @param oldPassword 旧密码
     * @param password 新密码
     * @return 修改结果
     */
    @RequestMapping("/updatePwd")
    @ResponseBody
    @XxlSso
    public Response<String> updatePwd(HttpServletRequest request,
                                       String oldPassword,
                                       String password) {

        // 参数校验
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            return Response.ofFail(I18nUtil.getString("system_please_input")
                    + I18nUtil.getString("change_pwd_field_oldpwd"));
        }
        if (password == null || password.trim().isEmpty()) {
            return Response.ofFail(I18nUtil.getString("system_please_input")
                    + I18nUtil.getString("change_pwd_field_oldpwd"));
        }
        password = password.trim();
        if (!(password.length() >= 4 && password.length() <= 20)) {
            return Response.ofFail(I18nUtil.getString("system_lengh_limit") + "[4-20]");
        }

        // 验证旧密码
        String oldPasswordHash = Sha256Tool.sha256(oldPassword);
        String passwordHash = Sha256Tool.sha256(password);

        Response<LoginInfo> loginInfoResponse = XxlSsoHelper.loginCheckWithAttr(request);
        XxlJobUser existUser = xxlJobUserMapper.loadByUserName(loginInfoResponse.getData().getUserName());
        if (!oldPasswordHash.equals(existUser.getPassword())) {
            return Response.ofFail(I18nUtil.getString("change_pwd_field_oldpwd")
                    + I18nUtil.getString("system_unvalid"));
        }

        // 更新密码
        existUser.setPassword(passwordHash);
        xxlJobUserMapper.update(existUser);

        return Response.ofSuccess();
    }

    /**
     * SSO单点登录入口
     *
     * 接收企业运营后台传来的JWT Token，验证通过后设置登录状态
     *
     * 流程：
     * 1. 检查用户是否已登录，已登录则直接跳转
     * 2. 验证JWT Token的签名和有效性
     * 3. 从Token中提取手机号
     * 4. 根据手机号查找或创建用户
     * 5. 设置xxl-sso登录状态
     * 6. 重定向到目标页面
     *
     * @param request HTTP请求
     * @param response HTTP响应
     * @param token JWT Token
     * @param redirect 登录成功后的重定向路径
     * @throws IOException IO异常
     */
    @RequestMapping("/sso")
    @XxlSso(login = false)
    public void sso(HttpServletRequest request,
                    HttpServletResponse response,
                    @RequestParam(value = "token", required = false) String token,
                    @RequestParam(value = "redirect", required = false, defaultValue = "/") String redirect)
            throws IOException {

        logger.info("[SSO] Login attempt. token={}, redirect={}", maskToken(token), redirect);

        try {
            // 检查用户是否已登录
            Response<LoginInfo> loginInfoResponse = XxlSsoHelper.loginCheckWithCookie(request, response);
            if (loginInfoResponse.isSuccess()) {
                logger.info("[SSO] User already logged in. userId={}", loginInfoResponse.getData().getUserId());
                response.sendRedirect(sanitizeRedirectUrl(redirect));
                return;
            }

            // 验证JWT Token并获取手机号
            String phone = ssoAuthService.validateToken(token);
            logger.info("[SSO] Token validated. phone={}", maskPhone(phone));

            // 查找或创建用户
            XxlJobUser user = ssoAuthService.findOrCreateUser(phone);
            logger.info("[SSO] User found/created. userId={}, username={}",
                    user.getId(), maskPhone(user.getUsername()));

            // 设置登录状态
            LoginInfo loginInfo = new LoginInfo(String.valueOf(user.getId()), UUIDTool.getSimpleUUID());
            Response<String> loginResult = XxlSsoHelper.loginWithCookie(loginInfo, response, false);

            if (!loginResult.isSuccess()) {
                logger.error("[SSO] Login failed. msg={}", loginResult.getMsg());
                redirectWithError(response, "登录失败，请稍后重试");
                return;
            }

            logger.info("[SSO] Login successful. userId={}, redirect={}", user.getId(), redirect);
            response.sendRedirect(sanitizeRedirectUrl(redirect));

        } catch (SsoException e) {
            logger.warn("[SSO] SSO exception. code={}, msg={}", e.getErrorCode(), e.getErrorMessage());
            redirectWithError(response, e.getErrorMessage());
        } catch (Exception e) {
            logger.error("[SSO] Unexpected error", e);
            redirectWithError(response, "系统异常，请稍后重试");
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 判断是否需要自动重定向到企业运营后台SSO入口
     *
     * 需要满足以下条件：
     * 1. SSO功能已启用
     * 2. 自动重定向已开启
     * 3. 已配置运营后台SSO入口地址
     *
     * @return boolean 是否需要重定向
     */
    private boolean shouldAutoRedirectToSsoServer() {
        return ssoProperties.isEnabled()
                && ssoProperties.isAutoRedirect()
                && StringTool.isNotBlank(ssoProperties.getServerUrl());
    }

    /**
     * 构建跳转到企业运营后台SSO入口的URL
     *
     * URL格式：{sso.server-url}?redirect={xxl-job-admin的当前请求路径}
     *
     * 运营后台需要：
     * 1. 检测redirect参数
     * 2. 验证用户是否已登录
     * 3. 生成JWT Token
     * 4. 跳转回 /auth/sso?token=xxx&redirect=xxx
     *
     * @param request HTTP请求
     * @return String 重定向URL
     */
    private String buildSsoServerRedirectUrl(HttpServletRequest request) {
        String serverUrl = ssoProperties.getServerUrl();

        // 构建目标跳转地址（用户原本想访问的页面）
        // 注意：这里使用request.getContextPath() + "/"作为默认跳转地址
        // 因为拦截器已经跳转到/login，原始目标URL可能丢失
        // 如果需要精确的原始URL，可以在拦截器中保存到session
        String currentPath = request.getContextPath() + "/";

        // URL编码
        String encodedRedirect = URLEncoder.encode(currentPath, StandardCharsets.UTF_8);

        // 拼接URL（判断serverUrl是否已有参数）
        if (serverUrl.contains("?")) {
            return serverUrl + "&redirect=" + encodedRedirect;
        } else {
            return serverUrl + "?redirect=" + encodedRedirect;
        }
    }

    /**
     * 重定向到登录页面并携带错误信息
     *
     * @param response HTTP响应
     * @param errorMsg 错误信息
     * @throws IOException IO异常
     */
    private void redirectWithError(HttpServletResponse response, String errorMsg) throws IOException {
        String encodedError = URLEncoder.encode(errorMsg, StandardCharsets.UTF_8);
        response.sendRedirect("/auth/login?error=" + encodedError);
    }

    /**
     * 清理重定向URL，防止开放重定向攻击
     *
     * 安全措施：
     * 1. 空值返回首页
     * 2. 禁止跳转到外部URL（http://或https://开头）
     * 3. 确保URL以/开头
     *
     * @param redirect 原始重定向路径
     * @return String 安全的重定向路径
     */
    private String sanitizeRedirectUrl(String redirect) {
        if (StringTool.isBlank(redirect)) {
            return "/";
        }
        // 禁止跳转到外部URL，防止开放重定向攻击
        if (redirect.startsWith("http://") || redirect.startsWith("https://")) {
            logger.warn("[SSO] External redirect detected, blocked: {}", redirect);
            return "/";
        }
        // 确保以/开头
        if (!redirect.startsWith("/")) {
            return "/" + redirect;
        }
        return redirect;
    }

    /**
     * 掩码Token，用于日志记录
     *
     * 只显示前后各4位，中间用...代替
     *
     * @param token 原始Token
     * @return String 掩码后的Token
     */
    private String maskToken(String token) {
        if (StringTool.isBlank(token)) {
            return "null";
        }
        if (token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }

    /**
     * 掩码手机号，用于日志记录
     *
     * 中间4位用****代替
     * 例如：13800138000 -> 138****8000
     *
     * @param phone 原始手机号
     * @return String 掩码后的手机号
     */
    private String maskPhone(String phone) {
        if (StringTool.isBlank(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
