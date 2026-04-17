package com.xxl.job.admin.exception;

public class SsoException extends RuntimeException {

    private final String errorCode;
    private final String errorMessage;

    public SsoException(String errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public SsoException(String errorCode, String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public static SsoException tokenEmpty() {
        return new SsoException("SSO001", "Token 不能为空");
    }

    public static SsoException tokenInvalid() {
        return new SsoException("SSO002", "Token 格式无效");
    }

    public static SsoException tokenInvalid(Throwable cause) {
        return new SsoException("SSO003", "Token 签名验证失败", cause);
    }

    public static SsoException tokenExpired() {
        return new SsoException("SSO004", "Token 已过期");
    }

    public static SsoException tokenIssuedAtInvalid() {
        return new SsoException("SSO005", "Token 签发时间无效");
    }

    public static SsoException phoneInvalid() {
        return new SsoException("SSO006", "手机号格式无效");
    }

    public static SsoException userNotFound() {
        return new SsoException("SSO007", "用户不存在");
    }

    public static SsoException userDisabled() {
        return new SsoException("SSO008", "用户已被禁用");
    }

    public static SsoException systemError(Throwable cause) {
        return new SsoException("SSO009", "系统异常", cause);
    }
}
