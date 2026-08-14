package com.xuwenye.demo.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板实体（对应 sys_coupon 表）
 * 1.管理员创建优惠券模板，可发放给买家
 * 2.type：1满减（discount_value=减免金额） / 2折扣（discount_value=折数，如8.00表示8折）
 * 3.remain_count 控制可发放总量（发完即停）
 * <p>
 * @author ZuiM
 */
@Data
@TableName("sys_coupon")
public class Coupon {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 优惠券名称 */
    private String name;

    /** 优惠类型：1满减 2折扣 */
    private Integer type;

    /** 优惠值：满减为减免金额(元)，折扣为折数 */
    private BigDecimal discountValue;

    /** 使用门槛：订单满X元可用 */
    private BigDecimal minAmount;

    /** 发行总量 */
    private Integer totalCount;

    /** 剩余可发放数量 */
    private Integer remainCount;

    /** 状态：0停用 1启用 */
    private Integer status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
