package com.xxl.job.admin.service;

import com.xxl.tool.response.Response;

public interface SsoService {

    Response<SsoTokenInfo> validateToken(String token);

    Response<Boolean> autoLogin(String phone);

    record SsoTokenInfo(
        String phone,
        Long timestamp,
        String nonce
    ) {}
}
