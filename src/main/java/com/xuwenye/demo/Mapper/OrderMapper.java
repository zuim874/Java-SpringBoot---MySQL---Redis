package com.xuwenye.demo.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuwenye.demo.Entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 订单数据访问层
 * 1.基础 CRUD 由 BaseMapper 自动生成
 * 2.自定义查询：按用户ID查询订单列表、按订单号查询订单
 * <p>
 * @author ZuiM
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 按用户ID查询订单列表（按创建时间倒序）
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     * @return List<Order> 订单列表
     */
    @Select("SELECT * FROM sys_order WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY create_time DESC")
    List<Order> findOrdersByUserId(@Param("userId") Long userId);

    /**
     * 按订单号查询订单
     * <p>
     * @author ZuiM
     * @param orderNo 订单号
     * @return Order 订单（可能为null）
     */
    @Select("SELECT * FROM sys_order WHERE order_no = #{orderNo} AND is_deleted = 0")
    Order findOrderByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 按ID查询订单（含已逻辑删除的不查）
     * <p>
     * @author ZuiM
     * @param id 订单ID
     * @return Order 订单（可能为null）
     */
    @Select("SELECT * FROM sys_order WHERE id = #{id} AND is_deleted = 0")
    Order findOrderById(@Param("id") Long id);

    /**
     * 按状态查询所有订单（管理员用）
     * <p>
     * @author ZuiM
     * @param status 订单状态（传入null则查询全部）
     * @return List<Order> 订单列表
     */
    @Select("<script>" +
            "SELECT * FROM sys_order WHERE is_deleted = 0" +
            "<if test='status != null'> AND status = #{status}</if>" +
            " ORDER BY create_time DESC" +
            "</script>")
    List<Order> findOrdersByStatus(@Param("status") Integer status);
}