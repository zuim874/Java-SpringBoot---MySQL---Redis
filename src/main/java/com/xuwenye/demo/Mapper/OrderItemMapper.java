package com.xuwenye.demo.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuwenye.demo.Entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 订单项数据访问层
 * 1.基础 CRUD 由 BaseMapper 自动生成
 * 2.自定义查询：按订单ID查询订单项列表
 * <p>
 * @author ZuiM
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    /**
     * 按订单ID查询所有订单项
     * <p>
     * @author ZuiM
     * @param orderId 订单ID
     * @return List<OrderItem> 订单项列表
     */
    @Select("SELECT * FROM sys_order_item WHERE order_id = #{orderId} ORDER BY id ASC")
    List<OrderItem> findItemsByOrderId(@Param("orderId") Long orderId);
}