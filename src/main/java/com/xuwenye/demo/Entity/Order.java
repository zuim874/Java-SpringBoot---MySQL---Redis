package com.xuwenye.demo.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体（对应 sys_order 表）
 * 1.主键自增（IdType.AUTO）
 * 2.订单号 orderNo 唯一标识一笔订单
 * 3.status 枚举：0待支付 1已支付 2已发货 3已完成 4已取消 5已退款
 * 4.逻辑删除字段 is_deleted（配合 @TableLogic）
 * <p>
 * @author ZuiM
 */
@Data
@TableName("sys_order")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID，关联 sys_user.id */
    private Long userId;

    /** 订单号（唯一，由 OrderNoGenerator 生成） */
    private String orderNo;

    /** 订单总金额（元） */
    private BigDecimal totalAmount;

    /**
     * 订单状态：
     * 0-待支付
     * 1-已支付
     * 2-已发货
     * 3-已完成
     * 4-已取消
     * 5-已退款
     */
    private Integer status;

    /** 支付方式（如：微信支付、支付宝等） */
    private String paymentMethod;

    /** 收货人姓名 */
    private String receiverName;

    /** 收货人电话 */
    private String receiverPhone;

    /** 收货地址 */
    private String receiverAddress;

    /** 订单备注 */
    private String remark;

    /** 支付时间 */
    @TableField("pay_time")
    private LocalDateTime payTime;

    /** 发货时间 */
    @TableField("ship_time")
    private LocalDateTime shipTime;

    /** 完成时间 */
    @TableField("complete_time")
    private LocalDateTime completeTime;

    /** 取消时间 */
    @TableField("cancel_time")
    private LocalDateTime cancelTime;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    /** 逻辑删除 */
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}