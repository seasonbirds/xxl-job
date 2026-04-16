package com.xxl.job.admin.web.xxlsso;

import com.xxl.job.admin.constant.Consts;
import com.xxl.job.admin.mapper.XxlJobRoleMapper;
import com.xxl.job.admin.mapper.XxlJobUserMapper;
import com.xxl.job.admin.model.XxlJobRole;
import com.xxl.job.admin.model.XxlJobUser;
import com.xxl.sso.core.model.LoginInfo;
import com.xxl.sso.core.store.LoginStore;
import com.xxl.tool.core.MapTool;
import com.xxl.tool.response.Response;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Simple LoginStore
 *
 * 1、store by database；
 * 2、If you have higher performance requirements, it is recommended to use RedisLoginStore；
 *
 * 权限判断逻辑说明：
 * - 用户表 `xxl_job_user.role` 字段关联角色表 `xxl_job_role.id`
 * - 通过角色ID查询角色表，获取 `role_type` 字段判断权限类型（0-普通用户，1-管理员）
 *
 * @author xuxueli 2025-08-03
 */
@Component
public class SimpleLoginStore implements LoginStore {


    @Resource
    private XxlJobUserMapper xxlJobUserMapper;

    @Resource
    private XxlJobRoleMapper xxlJobRoleMapper;


    @Override
    public Response<String> set(LoginInfo loginInfo) {

        // parse token-signature
        String token_sign = loginInfo.getSignature();

        // write token by UserId
        int ret = xxlJobUserMapper.updateToken(Integer.parseInt(loginInfo.getUserId()), token_sign);
        return ret > 0 ? Response.ofSuccess() : Response.ofFail("token set fail");
    }

    @Override
    public Response<String> update(LoginInfo loginInfo) {
        return Response.ofFail("not support");
    }

    @Override
    public Response<String> remove(String userId) {
        // delete token-signature
        int ret = xxlJobUserMapper.updateToken(Integer.parseInt(userId), "");
        return ret > 0 ? Response.ofSuccess() : Response.ofFail("token remove fail");
    }

    /**
     * check through DB query
     * 
     * 权限判断逻辑：
     * 1. 通过用户ID查询用户信息
     * 2. 通过用户的 `role` 字段（角色ID）查询角色表
     * 3. 根据角色的 `role_type` 字段判断是否是管理员（1=管理员，0=普通用户）
     */
    @Override
    public Response<LoginInfo> get(String userId) {

        // load login-user
        XxlJobUser user = xxlJobUserMapper.loadById(Integer.parseInt(userId));
        if (user == null) {
            return Response.ofFail("userId invalid.");
        }

        // parse role：通过角色ID查询角色表获取权限类型
        // user.getRole() 存储的是角色表的主键ID
        List<String> roleList = null;
        XxlJobRole role = xxlJobRoleMapper.loadById(user.getRole());
        if (role != null && role.getRoleType() == 1) {
            roleList = List.of(Consts.ADMIN_ROLE);
        }

        // parse jobGroup permission
        Map<String, String> extraInfo = MapTool.newMap(
                "jobGroups", user.getPermission()
        );

        // build LoginInfo
        LoginInfo loginInfo = new LoginInfo(userId, user.getToken());
        loginInfo.setUserName(user.getUsername());
        loginInfo.setRoleList(roleList);
        loginInfo.setExtraInfo(extraInfo);

        return Response.ofSuccess(loginInfo);
    }

}
