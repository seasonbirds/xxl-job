package com.xxl.job.admin.mapper;

import com.xxl.job.admin.model.XxlJobRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface XxlJobRoleMapper {

    public List<XxlJobRole> pageList(@Param("offset") int offset,
                                      @Param("pagesize") int pagesize,
                                      @Param("name") String name,
                                      @Param("code") String code,
                                      @Param("status") int status);

    public int pageListCount(@Param("offset") int offset,
                              @Param("pagesize") int pagesize,
                              @Param("name") String name,
                              @Param("code") String code,
                              @Param("status") int status);

    public XxlJobRole loadById(@Param("id") int id);

    public XxlJobRole loadByName(@Param("name") String name);

    public XxlJobRole loadByCode(@Param("code") String code);

    public int save(XxlJobRole xxlJobRole);

    public int update(XxlJobRole xxlJobRole);

    public int delete(@Param("id") int id);

    public List<XxlJobRole> findAllEnabled();

    public List<XxlJobRole> findByOldRole(@Param("oldRole") int oldRole);

}
