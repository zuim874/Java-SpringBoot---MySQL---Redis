package com.xuwenye.demo.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuwenye.demo.Entity.UserAddress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户收货地址数据访问层
 * 1.基础 CRUD 由 BaseMapper 自动生成
 * 2.自定义查询：按用户ID查询地址列表、查询默认地址
 * <p>
 * @author ZuiM
 */
@Mapper
public interface UserAddressMapper extends BaseMapper<UserAddress> {

    /**
     * 按用户ID查询所有地址（按更新时间倒序）
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     * @return List<UserAddress> 地址列表
     */
    @Select("SELECT * FROM sys_user_address WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY is_default DESC, update_time DESC")
    List<UserAddress> findAddressesByUserId(@Param("userId") Long userId);

    /**
     * 查询用户的默认地址
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     * @return UserAddress 默认地址（可能为null）
     */
    @Select("SELECT * FROM sys_user_address WHERE user_id = #{userId} AND is_default = 1 AND is_deleted = 0 LIMIT 1")
    UserAddress findDefaultAddress(@Param("userId") Long userId);
}