package com.xuwenye.demo.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuwenye.demo.Entity.Role;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色数据访问层
 * 1.基础 CRUD 由 BaseMapper 自动生成
 * <p>
 * @author ZuiM
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {
    // 基础 CRUD 方法已由 BaseMapper 自动生成
}
