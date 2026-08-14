package com.xuwenye.demo.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户优惠券实体（对应 sys_user_coupon 表）
 * 1.记录已发放到买家账户的优惠券，含快照信息（模板变更不影响已发放的券）
 * 2.status：0未使用 1已使用 2已过期
 * 3.下单结算时使用，需校验归属、状态、过期时间与使用门槛
 * <p>
 * @author ZuiM
 */
@Data
@TableName("sys_user_coupon")
public class UserCoupon {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 持有者用户ID */
    private Long userId;

    /** 来源优惠券模板ID */
    private Long couponId;

    /** 优惠券名称（快照） */
    private String name;

    /** 优惠类型（快照）：1满减 2折扣 */
    private Integer type;

    /** 优惠值（快照） */
    private BigDecimal discountValue;

    /** 使用门槛（快照） */
    private BigDecimal minAmount;

    /** 状态：0未使用 1已使用 2已过期 */
    private Integer status;

    /** 过期时间 */
    @TableField("expire_time")
    private LocalDateTime expireTime;

    /** 领取时间 */
    @TableField("receive_time")
    private LocalDateTime receiveTime;

    /** 使用时间 */
    @TableField("use_time")
    private LocalDateTime useTime;

    /** 使用的订单ID（status=1时有效） */
    private Long orderId;
}
