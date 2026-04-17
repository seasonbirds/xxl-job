package com.xxl.job.admin.controller.base;

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

@Controller
@RequestMapping("/auth")
public class LoginController {

	private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

	@Resource
	private XxlJobUserMapper xxlJobUserMapper;

	@Resource
	private SsoAuthService ssoAuthService;

	@RequestMapping("/login")
	@XxlSso(login = false)
	public ModelAndView login(HttpServletRequest request, HttpServletResponse response, ModelAndView modelAndView) {

		// xxl-sso, logincheck
		Response<LoginInfo> loginInfoResponse = XxlSsoHelper.loginCheckWithCookie(request, response);

		if (loginInfoResponse.isSuccess()) {
			modelAndView.setView(new RedirectView("/",true,false));
			return modelAndView;
		}
		return new ModelAndView("base/login");
	}

	@RequestMapping(value="/doLogin", method=RequestMethod.POST)
	@ResponseBody
	@XxlSso(login=false)
	public Response<String> doLogin(HttpServletRequest request, HttpServletResponse response, String userName, String password, String ifRemember){

		// param
		boolean ifRem = StringTool.isNotBlank(ifRemember) && "on".equals(ifRemember);
		if (StringTool.isBlank(userName) || StringTool.isBlank(password)){
			return Response.ofFail( I18nUtil.getString("login_param_empty") );
		}

		// valid user、status
		XxlJobUser xxlJobUser = xxlJobUserMapper.loadByUserName(userName);
		if (xxlJobUser == null) {
			return Response.ofFail( I18nUtil.getString("login_param_unvalid") );
		}

		// valid passowrd
		String passwordHash = Sha256Tool.sha256(password);
		if (!passwordHash.equals(xxlJobUser.getPassword())) {
			return Response.ofFail( I18nUtil.getString("login_param_unvalid") );
		}

		// xxl-sso, do login
		LoginInfo loginInfo = new LoginInfo(String.valueOf(xxlJobUser.getId()), UUIDTool.getSimpleUUID());
		Response<String> result= XxlSsoHelper.loginWithCookie(loginInfo, response, ifRem);

		return Response.of(result.getCode(), result.getMsg());
	}
	
	@RequestMapping(value="/logout", method=RequestMethod.POST)
	@ResponseBody
	@XxlSso(login=false)
	public Response<String> logout(HttpServletRequest request, HttpServletResponse response){

		// xxl-sso, do logout
		Response<String> result = XxlSsoHelper.logoutWithCookie(request, response);

		return Response.of(result.getCode(), result.getMsg());
	}

	@RequestMapping("/updatePwd")
	@ResponseBody
	@XxlSso
	public Response<String> updatePwd(HttpServletRequest request, String oldPassword, String password){

		// valid
		if (oldPassword==null || oldPassword.trim().isEmpty()){
			return Response.ofFail(I18nUtil.getString("system_please_input") + I18nUtil.getString("change_pwd_field_oldpwd"));
		}
		if (password==null || password.trim().isEmpty()){
			return Response.ofFail(I18nUtil.getString("system_please_input") + I18nUtil.getString("change_pwd_field_oldpwd"));
		}
		password = password.trim();
		if (!(password.length()>=4 && password.length()<=20)) {
			return Response.ofFail(I18nUtil.getString("system_lengh_limit")+"[4-20]" );
		}

		// md5 password
		String oldPasswordHash = Sha256Tool.sha256(oldPassword);
		String passwordHash = Sha256Tool.sha256(password);

		// valid old pwd
		Response<LoginInfo> loginInfoResponse = XxlSsoHelper.loginCheckWithAttr(request);
		XxlJobUser existUser = xxlJobUserMapper.loadByUserName(loginInfoResponse.getData().getUserName());
		if (!oldPasswordHash.equals(existUser.getPassword())) {
			return Response.ofFail(I18nUtil.getString("change_pwd_field_oldpwd") + I18nUtil.getString("system_unvalid"));
		}

		// write new
		existUser.setPassword(passwordHash);
		xxlJobUserMapper.update(existUser);

		return Response.ofSuccess();
	}

	@RequestMapping("/sso")
	@XxlSso(login = false)
	public void sso(HttpServletRequest request,
	                 HttpServletResponse response,
	                 @RequestParam(value = "token", required = false) String token,
	                 @RequestParam(value = "redirect", required = false, defaultValue = "/") String redirect) throws IOException {

		logger.info("[SSO] Login attempt. token={}, redirect={}", maskToken(token), redirect);

		try {
			Response<LoginInfo> loginInfoResponse = XxlSsoHelper.loginCheckWithCookie(request, response);
			if (loginInfoResponse.isSuccess()) {
				logger.info("[SSO] User already logged in. userId={}", loginInfoResponse.getData().getUserId());
				response.sendRedirect(sanitizeRedirectUrl(redirect));
				return;
			}

			String phone = ssoAuthService.validateToken(token);
			logger.info("[SSO] Token validated. phone={}", maskPhone(phone));

			XxlJobUser user = ssoAuthService.findOrCreateUser(phone);
			logger.info("[SSO] User found/created. userId={}, username={}", user.getId(), maskPhone(user.getUsername()));

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

	private void redirectWithError(HttpServletResponse response, String errorMsg) throws IOException {
		String encodedError = URLEncoder.encode(errorMsg, StandardCharsets.UTF_8);
		response.sendRedirect("/auth/login?error=" + encodedError);
	}

	private String sanitizeRedirectUrl(String redirect) {
		if (StringTool.isBlank(redirect)) {
			return "/";
		}
		if (redirect.startsWith("http://") || redirect.startsWith("https://")) {
			logger.warn("[SSO] External redirect detected, blocked: {}", redirect);
			return "/";
		}
		if (!redirect.startsWith("/")) {
			return "/" + redirect;
		}
		return redirect;
	}

	private String maskToken(String token) {
		if (StringTool.isBlank(token)) {
			return "null";
		}
		if (token.length() <= 8) {
			return "***";
		}
		return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
	}

	private String maskPhone(String phone) {
		if (StringTool.isBlank(phone) || phone.length() < 7) {
			return phone;
		}
		return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
	}

}
