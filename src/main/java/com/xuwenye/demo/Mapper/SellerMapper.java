package com.xuwenye.demo.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuwenye.demo.Entity.Seller;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 卖家数据访问层
 * 1.基础 CRUD 由 BaseMapper 自动生成
 * 2.自定义查询：按名称查询
 * <p>
 * @author ZuiM
 */
@Mapper
public interface SellerMapper extends BaseMapper<Seller> {
    // 基础 CRUD 方法已由 BaseMapper 自动生成

    /**
     * 查询所有未删除的卖家
     * <p>
     * @author ZuiM
     * @return List<Seller> 卖家列表
     */
    @Select("SELECT * FROM sys_seller WHERE is_deleted = 0")
    List<Seller> findAllSellers();

    /**
     * 按ID查询卖家
     * <p>
     * @author ZuiM
     * @param id 卖家ID
     * @return Seller 卖家（可能为null）
     */
    @Select("SELECT * FROM sys_seller WHERE id = #{id} AND is_deleted = 0")
    Seller findSellerById(@Param("id") Long id);

    /**
     * 按关联用户ID查询卖家（卖家登录后经 user_id 直查店铺，替代「名称=用户名」约定）
     * <p>
     * @author ZuiM
     * @param userId 关联的用户ID（sys_user.id）
     * @return Seller 卖家（可能为null）
     */
    @Select("SELECT * FROM sys_seller WHERE user_id = #{userId} AND is_deleted = 0")
    Seller findSellerByUserId(@Param("userId") Long userId);
}
