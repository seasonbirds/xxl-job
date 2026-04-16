package com.xxl.job.admin.mapper;

import com.xxl.job.admin.model.XxlJobRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 角色数据访问层
 * 提供角色相关的数据库操作
 *
 * @author xxl-job
 */
@Mapper
public interface XxlJobRoleMapper {

    /**
     * 分页查询角色列表
     *
     * @param offset   偏移量
     * @param pagesize 每页数量
     * @param name     角色名称（模糊查询）
     * @param code     角色编码（模糊查询）
     * @param status   状态（-1表示全部）
     * @return 角色列表
     */
    public List<XxlJobRole> pageList(@Param("offset") int offset,
                                      @Param("pagesize") int pagesize,
                                      @Param("name") String name,
                                      @Param("code") String code,
                                      @Param("status") int status);

    /**
     * 分页查询角色总数
     *
     * @param offset   偏移量
     * @param pagesize 每页数量
     * @param name     角色名称（模糊查询）
     * @param code     角色编码（模糊查询）
     * @param status   状态（-1表示全部）
     * @return 总数
     */
    public int pageListCount(@Param("offset") int offset,
                              @Param("pagesize") int pagesize,
                              @Param("name") String name,
                              @Param("code") String code,
                              @Param("status") int status);

    /**
     * 根据ID加载角色
     *
     * @param id 角色ID
     * @return 角色对象
     */
    public XxlJobRole loadById(@Param("id") int id);

    /**
     * 根据名称加载角色
     *
     * @param name 角色名称
     * @return 角色对象
     */
    public XxlJobRole loadByName(@Param("name") String name);

    /**
     * 根据编码加载角色
     *
     * @param code 角色编码
     * @return 角色对象
     */
    public XxlJobRole loadByCode(@Param("code") String code);

    /**
     * 保存角色
     *
     * @param xxlJobRole 角色对象
     * @return 影响行数
     */
    public int save(XxlJobRole xxlJobRole);

    /**
     * 更新角色
     *
     * @param xxlJobRole 角色对象
     * @return 影响行数
     */
    public int update(XxlJobRole xxlJobRole);

    /**
     * 删除角色
     *
     * @param id 角色ID
     * @return 影响行数
     */
    public int delete(@Param("id") int id);

    /**
     * 查询所有启用状态的角色
     *
     * @return 启用状态的角色列表
     */
    public List<XxlJobRole> findAllEnabled();

}
