package com.xuweney.demo.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuweney.demo.Entity.Role;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {
    //方法已经由basemapper自动生成
}