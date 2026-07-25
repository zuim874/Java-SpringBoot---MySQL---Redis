package com.xuweney.demo.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuweney.demo.Entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    //方法已经由basemapper自动生成

    // 查所有（包括已删除），用于注册时判断
    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    User findByUsernameAll(@Param("username") String username);

    // 查邮箱
    @Select("SELECT * FROM sys_user WHERE email = #{email}")
    User findByEmail(@Param("email") String email);

    // 恢复删除用户
    @Update("UPDATE sys_user SET is_deleted = 0 WHERE id = #{id}")
    int recoverById(@Param("id") Long id);

    // 检查用户权限
    @Select("SELECT role from sys_user WHERE username = #{username}")
    boolean checkRole(@Param("username") String username);
}
