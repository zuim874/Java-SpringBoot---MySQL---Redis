package com.xuwenye.demo.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuwenye.demo.Entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户数据访问层
 * 1.基础 CRUD 由 BaseMapper 自动生成
 * 2.自定义查询：按用户名/邮箱查询、恢复逻辑删除用户
 * <p>
 * @author ZuiM
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 基础 CRUD 方法已由 BaseMapper 自动生成

    /**
     * 查所有（包括已逻辑删除的用户），用于注册时判断用户名是否占用
     * <p>
     * @author ZuiM
     * @param username 用户名
     * @return User 用户（可能为 null）
     */
    // 查所有（包括已删除），用于注册时判断
    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    User findByUsernameAll(@Param("username") String username);

    /**
     * 按邮箱查询用户
     * <p>
     * @author ZuiM
     * @param email 邮箱
     * @return User 用户（可能为 null）
     */
    // 查邮箱
    @Select("SELECT * FROM sys_user WHERE email = #{email}")
    User findByEmail(@Param("email") String email);

    /**
     * 恢复逻辑删除的用户（is_deleted 置 0）
     * <p>
     * @author ZuiM
     * @param id 用户 ID
     * @return int 受影响行数
     */
    // 恢复删除用户
    @Update("UPDATE sys_user SET is_deleted = 0 WHERE id = #{id}")
    int recoverById(@Param("id") Long id);

    /**
     * 查询已逻辑删除的用户（用于账号恢复时校验身份）
     * <p>
     * @author ZuiM
     * @param username 用户名
     * @return User 已删除用户（可能为 null）
     */
    // 查询已逻辑删除的用户（用于账号恢复时校验）
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND is_deleted = 1")
    User findByUsernameDeleted(@Param("username") String username);
}
