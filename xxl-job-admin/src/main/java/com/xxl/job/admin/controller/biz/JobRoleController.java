package com.xxl.job.admin.controller.biz;

import com.xxl.job.admin.constant.Consts;
import com.xxl.job.admin.mapper.XxlJobRoleMapper;
import com.xxl.job.admin.mapper.XxlJobUserMapper;
import com.xxl.job.admin.model.XxlJobRole;
import com.xxl.job.admin.model.XxlJobUser;
import com.xxl.job.admin.util.I18nUtil;
import com.xxl.sso.core.annotation.XxlSso;
import com.xxl.tool.core.CollectionTool;
import com.xxl.tool.core.StringTool;
import com.xxl.tool.response.PageModel;
import com.xxl.tool.response.Response;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 角色管理控制器
 * 提供角色的增删改查、启用/禁用等功能
 * 
 * 关联说明：
 * - 用户表 `xxl_job_user.role` 字段关联角色表 `xxl_job_role.id`
 *
 * @author xxl-job
 */
@Controller
@RequestMapping("/role")
public class JobRoleController {

    @Resource
    private XxlJobRoleMapper xxlJobRoleMapper;

    @Resource
    private XxlJobUserMapper xxlJobUserMapper;

    /**
     * 角色管理页面入口
     *
     * @param model 视图模型
     * @return 角色管理页面视图
     */
    @RequestMapping
    @XxlSso(role = Consts.ADMIN_ROLE)
    public String index(Model model) {
        return "biz/role.list";
    }

    /**
     * 分页查询角色列表
     *
     * @param offset   分页偏移量
     * @param pagesize 每页数量
     * @param name     角色名称（模糊查询）
     * @param code     角色编码（模糊查询）
     * @param status   状态（-1表示全部，0禁用，1启用）
     * @return 分页查询结果
     */
    @RequestMapping("/pageList")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<PageModel<XxlJobRole>> pageList(@RequestParam(required = false, defaultValue = "0") int offset,
                                                      @RequestParam(required = false, defaultValue = "10") int pagesize,
                                                      @RequestParam(required = false) String name,
                                                      @RequestParam(required = false) String code,
                                                      @RequestParam(required = false, defaultValue = "-1") int status) {

        // 分页查询角色列表
        List<XxlJobRole> list = xxlJobRoleMapper.pageList(offset, pagesize, name, code, status);
        int list_count = xxlJobRoleMapper.pageListCount(offset, pagesize, name, code, status);

        // 封装分页结果
        PageModel<XxlJobRole> pageModel = new PageModel<>();
        pageModel.setData(list);
        pageModel.setTotal(list_count);

        return Response.ofSuccess(pageModel);
    }

    /**
     * 查询所有启用状态的角色列表
     * 用于用户管理页面的角色下拉框
     *
     * @return 启用状态的角色列表
     */
    @RequestMapping("/listEnabled")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<List<XxlJobRole>> listEnabled() {
        List<XxlJobRole> list = xxlJobRoleMapper.findAllEnabled();
        return Response.ofSuccess(list);
    }

    /**
     * 根据名称搜索角色（模糊匹配）
     * 用于搜索框的实时查询
     *
     * @param keyword 搜索关键词
     * @return 匹配的角色列表
     */
    @RequestMapping("/searchByName")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<List<XxlJobRole>> searchByName(@RequestParam(required = false) String keyword) {
        if (StringTool.isBlank(keyword)) {
            return Response.ofSuccess(xxlJobRoleMapper.findAllEnabled());
        }
        List<XxlJobRole> list = xxlJobRoleMapper.pageList(0, 20, keyword, null, 1);
        return Response.ofSuccess(list);
    }

    /**
     * 根据编码搜索角色（模糊匹配）
     * 用于搜索框的实时查询
     *
     * @param keyword 搜索关键词
     * @return 匹配的角色列表
     */
    @RequestMapping("/searchByCode")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<List<XxlJobRole>> searchByCode(@RequestParam(required = false) String keyword) {
        if (StringTool.isBlank(keyword)) {
            return Response.ofSuccess(xxlJobRoleMapper.findAllEnabled());
        }
        List<XxlJobRole> list = xxlJobRoleMapper.pageList(0, 20, null, keyword, 1);
        return Response.ofSuccess(list);
    }

    /**
     * 新增角色
     *
     * @param xxlJobRole 角色对象
     * @return 操作结果
     */
    @RequestMapping("/insert")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<String> insert(XxlJobRole xxlJobRole) {

        // 校验角色名称
        if (StringTool.isBlank(xxlJobRole.getName())) {
            return Response.ofFail(I18nUtil.getString("system_please_input") + I18nUtil.getString("role_name"));
        }
        xxlJobRole.setName(xxlJobRole.getName().trim());
        if (!(xxlJobRole.getName().length() >= 2 && xxlJobRole.getName().length() <= 50)) {
            return Response.ofFail(I18nUtil.getString("system_lengh_limit") + "[2-50]");
        }

        // 校验角色编码
        if (StringTool.isBlank(xxlJobRole.getCode())) {
            return Response.ofFail(I18nUtil.getString("system_please_input") + I18nUtil.getString("role_code"));
        }
        xxlJobRole.setCode(xxlJobRole.getCode().trim());
        if (!(xxlJobRole.getCode().length() >= 2 && xxlJobRole.getCode().length() <= 50)) {
            return Response.ofFail(I18nUtil.getString("system_lengh_limit") + "[2-50]");
        }

        // 校验名称重复
        XxlJobRole existByName = xxlJobRoleMapper.loadByName(xxlJobRole.getName());
        if (existByName != null) {
            return Response.ofFail(I18nUtil.getString("role_name_repeat"));
        }

        // 校验编码重复
        XxlJobRole existByCode = xxlJobRoleMapper.loadByCode(xxlJobRole.getCode());
        if (existByCode != null) {
            return Response.ofFail(I18nUtil.getString("role_code_repeat"));
        }

        // 设置默认值：默认启用
        xxlJobRole.setStatus(1);
        Date now = new Date();
        xxlJobRole.setAddTime(now);
        xxlJobRole.setUpdateTime(now);

        // 保存角色
        xxlJobRoleMapper.save(xxlJobRole);
        return Response.ofSuccess();
    }

    /**
     * 更新角色
     * 注意：编辑时不能编辑状态，状态只能通过启用/禁用按钮操作
     *
     * @param xxlJobRole 角色对象
     * @return 操作结果
     */
    @RequestMapping("/update")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<String> update(XxlJobRole xxlJobRole) {

        // 校验角色是否存在
        XxlJobRole existRole = xxlJobRoleMapper.loadById(xxlJobRole.getId());
        if (existRole == null) {
            return Response.ofFail(I18nUtil.getString("role_not_exist"));
        }

        // 禁用状态的角色不能编辑
        if (existRole.getStatus() == 0) {
            return Response.ofFail(I18nUtil.getString("role_disabled_cannot_edit"));
        }

        // 校验角色名称
        if (StringTool.isBlank(xxlJobRole.getName())) {
            return Response.ofFail(I18nUtil.getString("system_please_input") + I18nUtil.getString("role_name"));
        }
        xxlJobRole.setName(xxlJobRole.getName().trim());
        if (!(xxlJobRole.getName().length() >= 2 && xxlJobRole.getName().length() <= 50)) {
            return Response.ofFail(I18nUtil.getString("system_lengh_limit") + "[2-50]");
        }

        // 校验角色编码
        if (StringTool.isBlank(xxlJobRole.getCode())) {
            return Response.ofFail(I18nUtil.getString("system_please_input") + I18nUtil.getString("role_code"));
        }
        xxlJobRole.setCode(xxlJobRole.getCode().trim());
        if (!(xxlJobRole.getCode().length() >= 2 && xxlJobRole.getCode().length() <= 50)) {
            return Response.ofFail(I18nUtil.getString("system_lengh_limit") + "[2-50]");
        }

        // 校验名称重复（排除自身）
        XxlJobRole existByName = xxlJobRoleMapper.loadByName(xxlJobRole.getName());
        if (existByName != null && existByName.getId() != xxlJobRole.getId()) {
            return Response.ofFail(I18nUtil.getString("role_name_repeat"));
        }

        // 校验编码重复（排除自身）
        XxlJobRole existByCode = xxlJobRoleMapper.loadByCode(xxlJobRole.getCode());
        if (existByCode != null && existByCode.getId() != xxlJobRole.getId()) {
            return Response.ofFail(I18nUtil.getString("role_code_repeat"));
        }

        // 更新角色信息（状态保持不变，只能通过启用/禁用按钮修改）
        existRole.setName(xxlJobRole.getName());
        existRole.setCode(xxlJobRole.getCode());
        existRole.setUpdateTime(new Date());

        // 执行更新
        xxlJobRoleMapper.update(existRole);
        return Response.ofSuccess();
    }

    /**
     * 删除角色
     * 注意：关联了用户的角色不能删除
     *
     * @param ids 角色ID列表（目前只支持单个删除）
     * @return 操作结果
     */
    @RequestMapping("/delete")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<String> delete(@RequestParam("ids[]") List<Integer> ids) {

        // 校验参数（目前只支持单个删除）
        if (CollectionTool.isEmpty(ids) || ids.size() != 1) {
            return Response.ofFail(I18nUtil.getString("system_please_choose") + I18nUtil.getString("system_one") + I18nUtil.getString("system_data"));
        }

        // 校验角色是否存在
        XxlJobRole existRole = xxlJobRoleMapper.loadById(ids.get(0));
        if (existRole == null) {
            return Response.ofFail(I18nUtil.getString("role_not_exist"));
        }

        // 检查是否有关联用户：xxl_job_user.role 关联 xxl_job_role.id
        // 注意：xxlJobUserMapper.pageList 第二个参数是 role（用户表的role字段）
        List<XxlJobUser> users = xxlJobUserMapper.pageList(0, 1, null, ids.get(0));
        if (users != null && !users.isEmpty()) {
            return Response.ofFail(I18nUtil.getString("role_has_users_cannot_delete"));
        }

        // 执行删除
        xxlJobRoleMapper.delete(ids.get(0));
        return Response.ofSuccess();
    }

    /**
     * 批量启用角色
     *
     * @param ids 角色ID列表
     * @return 操作结果
     */
    @RequestMapping("/enable")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<String> enable(@RequestParam("ids[]") List<Integer> ids) {

        // 校验参数
        if (CollectionTool.isEmpty(ids)) {
            return Response.ofFail(I18nUtil.getString("system_please_choose") + I18nUtil.getString("system_data"));
        }

        // 批量启用
        for (Integer id : ids) {
            XxlJobRole existRole = xxlJobRoleMapper.loadById(id);
            if (existRole != null && existRole.getStatus() == 0) {
                existRole.setStatus(1);
                existRole.setUpdateTime(new Date());
                xxlJobRoleMapper.update(existRole);
            }
        }

        return Response.ofSuccess();
    }

    /**
     * 批量禁用角色
     *
     * @param ids 角色ID列表
     * @return 操作结果
     */
    @RequestMapping("/disable")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<String> disable(@RequestParam("ids[]") List<Integer> ids) {

        // 校验参数
        if (CollectionTool.isEmpty(ids)) {
            return Response.ofFail(I18nUtil.getString("system_please_choose") + I18nUtil.getString("system_data"));
        }

        // 批量禁用
        for (Integer id : ids) {
            XxlJobRole existRole = xxlJobRoleMapper.loadById(id);
            if (existRole != null && existRole.getStatus() == 1) {
                existRole.setStatus(0);
                existRole.setUpdateTime(new Date());
                xxlJobRoleMapper.update(existRole);
            }
        }

        return Response.ofSuccess();
    }

    /**
     * 根据角色ID查询该角色下的用户列表
     * 用于角色管理页面的"查看用户"功能
     *
     * 关联逻辑：xxl_job_user.role 关联 xxl_job_role.id
     *
     * @param roleId 角色ID（即角色表的主键ID）
     * @return 用户名称列表
     */
    @RequestMapping("/getUsersByRole")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<List<String>> getUsersByRole(@RequestParam int roleId) {

        // 查询该角色下的所有用户：xxl_job_user.role = 角色ID
        List<XxlJobUser> users = xxlJobUserMapper.pageList(0, 1000, null, roleId);
        List<String> usernames = new ArrayList<>();
        if (users != null) {
            for (XxlJobUser user : users) {
                usernames.add(user.getUsername());
            }
        }
        return Response.ofSuccess(usernames);
    }

}
