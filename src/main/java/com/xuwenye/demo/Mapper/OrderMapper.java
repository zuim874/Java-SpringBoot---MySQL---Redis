package com.xuwenye.demo.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    /**
     * 分页查询某卖家的订单（通过订单项关联商品归属，DISTINCT 去重）
     * <p>
     * @author ZuiM
     * @param page 分页对象
     * @param sellerId 卖家ID
     * @param status 订单状态（为null则查询全部）
     * @return IPage<Order> 分页订单
     */
    @Select("<script>" +
            "SELECT DISTINCT o.* FROM sys_order o " +
            "JOIN sys_order_item oi ON oi.order_id = o.id " +
            "JOIN sys_product p ON p.id = oi.product_id " +
            "WHERE p.seller_id = #{sellerId} AND o.is_deleted = 0" +
            "<if test='status != null'> AND o.status = #{status}</if>" +
            " ORDER BY o.create_time DESC" +
            "</script>")
    IPage<Order> findSellerOrdersPage(Page<Order> page,
                                      @Param("sellerId") Long sellerId,
                                      @Param("status") Integer status);

    /**
     * 判断某订单是否包含该卖家的商品
     * <p>
     * @author ZuiM
     * @param orderId 订单ID
     * @param sellerId 卖家ID
     * @return int 命中数量（>0 表示包含）
     */
    @Select("SELECT COUNT(*) FROM sys_order_item oi " +
            "JOIN sys_product p ON p.id = oi.product_id " +
            "WHERE oi.order_id = #{orderId} AND p.seller_id = #{sellerId}")
    int countOrderBySeller(@Param("orderId") Long orderId, @Param("sellerId") Long sellerId);
}