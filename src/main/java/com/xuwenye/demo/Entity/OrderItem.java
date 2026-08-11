package com.xuwenye.demo.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单项实体（对应 sys_order_item 表）
 * 1.主键自增（IdType.AUTO）
 * 2.记录下单时商品快照信息（名称、价格、图片），不受后续商品信息变更影响
 * 3.逻辑删除字段 is_deleted（配合 @TableLogic）
 * <p>
 * @author ZuiM
 */
@Data
@TableName("sys_order_item")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单ID，关联 sys_order.id */
    private Long orderId;

    /** 商品ID，关联 sys_product.id */
    private Long productId;

    /** 下单时商品名称（快照） */
    private String productName;

    /** 下单时商品主图URL（快照） */
    private String productImage;

    /** 下单时商品单价（快照） */
    private BigDecimal price;

    /** 购买数量 */
    private Integer quantity;

    /** 小计金额（price * quantity） */
    private BigDecimal subtotal;

    @TableField("create_time")
    private LocalDateTime createTime;
}