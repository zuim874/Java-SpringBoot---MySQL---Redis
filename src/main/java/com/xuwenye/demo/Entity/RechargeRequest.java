package com.xuwenye.demo.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户充值申请实体（对应 sys_recharge_request 表）
 * 1.用户提交申请，管理员审核后决定是否增加余额
 * 2.status: 0=待审核 1=已通过 2=已拒绝
 * <p>
 * @author ZuiM
 */
@Data
@TableName("sys_recharge_request")
public class RechargeRequest {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 用户名（冗余，方便管理员查看） */
    private String username;

    /** 申请充值金额 */
    private BigDecimal amount;

    /** 状态：0待审核 1已通过 2已拒绝 */
    private Integer status;

    /** 备注/拒绝原因 */
    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}