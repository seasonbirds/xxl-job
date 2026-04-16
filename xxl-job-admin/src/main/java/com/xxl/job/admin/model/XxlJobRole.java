package com.xxl.job.admin.model;

import java.util.Date;

/**
 * 角色实体类
 * 用于管理系统中的角色信息
 * 
 * 关联说明：
 * - 用户表 `xxl_job_user.role` 字段关联本表的 `id` 字段
 * - 权限判断通过本实体的 `roleType` 字段（0-普通用户，1-管理员）
 *
 * @author xxl-job
 */
public class XxlJobRole {

    /**
     * 主键ID
     */
    private int id;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 角色编码
     */
    private String code;

    /**
     * 状态：0-禁用、1-启用
     */
    private int status;

    /**
     * 权限类型：0-普通用户、1-管理员
     * 用于权限判断，与原系统的 `xxl_job_user.role` 字段含义一致
     */
    private int roleType;

    /**
     * 创建时间
     */
    private Date addTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getRoleType() {
        return roleType;
    }

    public void setRoleType(int roleType) {
        this.roleType = roleType;
    }

    public Date getAddTime() {
        return addTime;
    }

    public void setAddTime(Date addTime) {
        this.addTime = addTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

}
