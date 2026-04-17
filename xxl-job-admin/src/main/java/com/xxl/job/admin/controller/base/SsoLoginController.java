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

@Controller
@RequestMapping("/auth")
public class SsoLoginController {

    private static final Logger logger = LoggerFactory.getLogger(SsoLoginController.class);

    @Resource
    private XxlJobSsoProperties xxlJobSsoProperties;

    @Resource
    private SsoServiceImpl ssoServiceImpl;

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

    private String getLoginUrl() {
        String failureUrl = xxlJobSsoProperties.getLoginFailureUrl();
        return (failureUrl != null && !failureUrl.isEmpty()) ? failureUrl : "/auth/login";
    }

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

    private RedirectView createRedirectView(String url) {
        RedirectView redirectView = new RedirectView();
        redirectView.setUrl(url);
        redirectView.setContextRelative(true);
        redirectView.setExposeModelAttributes(false);
        return redirectView;
    }
}
