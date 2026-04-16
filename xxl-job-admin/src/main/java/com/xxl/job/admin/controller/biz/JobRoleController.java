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

@Controller
@RequestMapping("/role")
public class JobRoleController {

    @Resource
    private XxlJobRoleMapper xxlJobRoleMapper;

    @Resource
    private XxlJobUserMapper xxlJobUserMapper;

    @RequestMapping
    @XxlSso(role = Consts.ADMIN_ROLE)
    public String index(Model model) {
        return "biz/role.list";
    }

    @RequestMapping("/pageList")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<PageModel<XxlJobRole>> pageList(@RequestParam(required = false, defaultValue = "0") int offset,
                                                      @RequestParam(required = false, defaultValue = "10") int pagesize,
                                                      @RequestParam(required = false) String name,
                                                      @RequestParam(required = false) String code,
                                                      @RequestParam(required = false, defaultValue = "-1") int status) {

        List<XxlJobRole> list = xxlJobRoleMapper.pageList(offset, pagesize, name, code, status);
        int list_count = xxlJobRoleMapper.pageListCount(offset, pagesize, name, code, status);

        PageModel<XxlJobRole> pageModel = new PageModel<>();
        pageModel.setData(list);
        pageModel.setTotal(list_count);

        return Response.ofSuccess(pageModel);
    }

    @RequestMapping("/listEnabled")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<List<XxlJobRole>> listEnabled() {
        List<XxlJobRole> list = xxlJobRoleMapper.findAllEnabled();
        return Response.ofSuccess(list);
    }

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

    @RequestMapping("/insert")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<String> insert(XxlJobRole xxlJobRole) {

        if (StringTool.isBlank(xxlJobRole.getName())) {
            return Response.ofFail(I18nUtil.getString("system_please_input") + I18nUtil.getString("role_name"));
        }
        xxlJobRole.setName(xxlJobRole.getName().trim());
        if (!(xxlJobRole.getName().length() >= 2 && xxlJobRole.getName().length() <= 50)) {
            return Response.ofFail(I18nUtil.getString("system_lengh_limit") + "[2-50]");
        }

        if (StringTool.isBlank(xxlJobRole.getCode())) {
            return Response.ofFail(I18nUtil.getString("system_please_input") + I18nUtil.getString("role_code"));
        }
        xxlJobRole.setCode(xxlJobRole.getCode().trim());
        if (!(xxlJobRole.getCode().length() >= 2 && xxlJobRole.getCode().length() <= 50)) {
            return Response.ofFail(I18nUtil.getString("system_lengh_limit") + "[2-50]");
        }

        XxlJobRole existByName = xxlJobRoleMapper.loadByName(xxlJobRole.getName());
        if (existByName != null) {
            return Response.ofFail(I18nUtil.getString("role_name_repeat"));
        }

        XxlJobRole existByCode = xxlJobRoleMapper.loadByCode(xxlJobRole.getCode());
        if (existByCode != null) {
            return Response.ofFail(I18nUtil.getString("role_code_repeat"));
        }

        xxlJobRole.setStatus(1);
        xxlJobRole.setOldRole(0);
        Date now = new Date();
        xxlJobRole.setAddTime(now);
        xxlJobRole.setUpdateTime(now);

        xxlJobRoleMapper.save(xxlJobRole);
        return Response.ofSuccess();
    }

    @RequestMapping("/update")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<String> update(XxlJobRole xxlJobRole) {

        XxlJobRole existRole = xxlJobRoleMapper.loadById(xxlJobRole.getId());
        if (existRole == null) {
            return Response.ofFail(I18nUtil.getString("role_not_exist"));
        }

        if (existRole.getStatus() == 0) {
            return Response.ofFail(I18nUtil.getString("role_disabled_cannot_edit"));
        }

        if (StringTool.isBlank(xxlJobRole.getName())) {
            return Response.ofFail(I18nUtil.getString("system_please_input") + I18nUtil.getString("role_name"));
        }
        xxlJobRole.setName(xxlJobRole.getName().trim());
        if (!(xxlJobRole.getName().length() >= 2 && xxlJobRole.getName().length() <= 50)) {
            return Response.ofFail(I18nUtil.getString("system_lengh_limit") + "[2-50]");
        }

        if (StringTool.isBlank(xxlJobRole.getCode())) {
            return Response.ofFail(I18nUtil.getString("system_please_input") + I18nUtil.getString("role_code"));
        }
        xxlJobRole.setCode(xxlJobRole.getCode().trim());
        if (!(xxlJobRole.getCode().length() >= 2 && xxlJobRole.getCode().length() <= 50)) {
            return Response.ofFail(I18nUtil.getString("system_lengh_limit") + "[2-50]");
        }

        XxlJobRole existByName = xxlJobRoleMapper.loadByName(xxlJobRole.getName());
        if (existByName != null && existByName.getId() != xxlJobRole.getId()) {
            return Response.ofFail(I18nUtil.getString("role_name_repeat"));
        }

        XxlJobRole existByCode = xxlJobRoleMapper.loadByCode(xxlJobRole.getCode());
        if (existByCode != null && existByCode.getId() != xxlJobRole.getId()) {
            return Response.ofFail(I18nUtil.getString("role_code_repeat"));
        }

        existRole.setName(xxlJobRole.getName());
        existRole.setCode(xxlJobRole.getCode());
        existRole.setOldRole(xxlJobRole.getOldRole());
        existRole.setUpdateTime(new Date());

        xxlJobRoleMapper.update(existRole);
        return Response.ofSuccess();
    }

    @RequestMapping("/delete")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<String> delete(@RequestParam("ids[]") List<Integer> ids) {

        if (CollectionTool.isEmpty(ids) || ids.size() != 1) {
            return Response.ofFail(I18nUtil.getString("system_please_choose") + I18nUtil.getString("system_one") + I18nUtil.getString("system_data"));
        }

        XxlJobRole existRole = xxlJobRoleMapper.loadById(ids.get(0));
        if (existRole == null) {
            return Response.ofFail(I18nUtil.getString("role_not_exist"));
        }

        List<XxlJobUser> users = xxlJobUserMapper.pageList(0, 1, null, existRole.getOldRole());
        if (users != null && !users.isEmpty()) {
            return Response.ofFail(I18nUtil.getString("role_has_users_cannot_delete"));
        }

        xxlJobRoleMapper.delete(ids.get(0));
        return Response.ofSuccess();
    }

    @RequestMapping("/enable")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<String> enable(@RequestParam("ids[]") List<Integer> ids) {

        if (CollectionTool.isEmpty(ids)) {
            return Response.ofFail(I18nUtil.getString("system_please_choose") + I18nUtil.getString("system_data"));
        }

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

    @RequestMapping("/disable")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<String> disable(@RequestParam("ids[]") List<Integer> ids) {

        if (CollectionTool.isEmpty(ids)) {
            return Response.ofFail(I18nUtil.getString("system_please_choose") + I18nUtil.getString("system_data"));
        }

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

    @RequestMapping("/getUsersByRole")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<List<String>> getUsersByRole(@RequestParam int oldRole) {

        List<XxlJobUser> users = xxlJobUserMapper.pageList(0, 1000, null, oldRole);
        List<String> usernames = new ArrayList<>();
        if (users != null) {
            for (XxlJobUser user : users) {
                usernames.add(user.getUsername());
            }
        }
        return Response.ofSuccess(usernames);
    }

}
