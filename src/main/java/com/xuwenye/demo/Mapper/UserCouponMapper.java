package com.xuwenye.demo.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuwenye.demo.Entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 用户优惠券数据访问层
 * 1.基础 CRUD 由 BaseMapper 自动生成
 * 2.自定义操作：查询我的优惠券、原子标记已使用（防止并发重复使用）
 * <p>
 * @author ZuiM
 */
@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    /**
     * 查询用户未使用的优惠券（未过期）
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     * @return List<UserCoupon> 可用优惠券列表
     */
    @Select("SELECT * FROM sys_user_coupon WHERE user_id = #{userId} AND status = 0 AND expire_time > NOW() ORDER BY expire_time ASC")
    List<UserCoupon> findAvailableByUserId(@Param("userId") Long userId);

    /**
     * 查询用户全部优惠券（含已用/已过期）
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     * @return List<UserCoupon> 优惠券列表
     */
    @Select("SELECT * FROM sys_user_coupon WHERE user_id = #{userId} ORDER BY receive_time DESC")
    List<UserCoupon> findAllByUserId(@Param("userId") Long userId);

    /**
     * 原子核销优惠券：仅当归属正确且状态为未使用时才能更新为已使用
     * 防止并发下同一张券被重复使用
     * <p>
     * @author ZuiM
     * @param id 用户优惠券ID
     * @param userId 用户ID
     * @param orderId 使用的订单ID
     * @return int 受影响行数（0=券不存在/已使用/非本人）
     */
    @Update("UPDATE sys_user_coupon SET status = 1, use_time = NOW(), order_id = #{orderId} " +
            "WHERE id = #{id} AND user_id = #{userId} AND status = 0 AND expire_time > NOW()")
    int markUsed(@Param("id") Long id, @Param("userId") Long userId, @Param("orderId") Long orderId);

    /**
     * 统计用户已领取指定模板的张数
     * 用于限制同一用户对同一模板的重复自助领取
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     * @param couponId 优惠券模板ID
     * @return int 已领取张数
     */
    @Select("SELECT COUNT(*) FROM sys_user_coupon WHERE user_id = #{userId} AND coupon_id = #{couponId}")
    int countByUserAndCoupon(@Param("userId") Long userId, @Param("couponId") Long couponId);

    /**
     * 查询已领取指定优惠券模板的全部用户ID
     * 用于初始化 Redis「已领取用户集合」（懒加载），配合唯一索引实现「同类券每人限领一次」
     * <p>
     * @author ZuiM
     * @param couponId 优惠券模板ID
     * @return List<Long> 已领取用户ID列表
     */
    @Select("SELECT user_id FROM sys_user_coupon WHERE coupon_id = #{couponId}")
    List<Long> selectUserIdsByCouponId(@Param("couponId") Long couponId);
}
