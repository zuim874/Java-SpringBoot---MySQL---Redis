package com.xuwenye.demo.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户实体（对应 sys_user 表）
 * 1.主键自增（IdType.AUTO）
 * 2.Java 字段用驼峰命名，@TableField 映射数据库蛇形列名
 * 3.逻辑删除字段 is_deleted（配合 @TableLogic）
 * 4.balance 账户余额（充值/支付用，默认 0）
 * <p>
 * @author ZuiM
 */
@Data                           // Lombok自动生成getter/setter/toString
@TableName("sys_user")         // 对应数据库表名（如果表名和类名一致可以不写）
public class User {
    @TableId(type = IdType.AUTO)  // 主键自增
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String email;
    private Integer status;                 // 0禁用，1启用
    @TableField("user_role")
    private String userRole;
    /** 账户余额（元，默认 0；充值增加、支付扣减） */
    private BigDecimal balance;
    @TableField("create_time")
    private LocalDateTime createTime;      // 创建账号时间
    @TableField("update_time")
    private LocalDateTime updateTime;      // 上一次更新资料时间
    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;   // 上一次登录时间

    // 逻辑删除（MyBatis-Plus 需要这个注解识别逻辑删除字段）
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
