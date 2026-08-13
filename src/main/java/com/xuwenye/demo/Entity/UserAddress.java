package com.xuwenye.demo.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户收货地址实体（对应 sys_user_address 表）
 * 1.每个用户可维护多个收货地址，通过 is_default 标记默认地址
 * 2.下单时可直接选择已有地址，无需重复输入
 * 3.逻辑删除字段 is_deleted
 * <p>
 * @author ZuiM
 */
@Data
@TableName("sys_user_address")
public class UserAddress {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID，关联 sys_user.id */
    private Long userId;

    /** 收货人姓名 */
    private String receiverName;

    /** 收货人电话 */
    private String receiverPhone;

    /** 收货地址 */
    private String receiverAddress;

    /** 是否默认地址：0否 1是 */
    @TableField("is_default")
    private Integer isDefault;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}