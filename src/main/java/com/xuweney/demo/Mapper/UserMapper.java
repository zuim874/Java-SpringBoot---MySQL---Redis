package com.xuweney.demo.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuweney.demo.Entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    //方法已经由basemapper自动生成

    // 查所有（包括已删除），用于注册时判断
    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    User findByUsernameAll(@Param("username") String username);
}
