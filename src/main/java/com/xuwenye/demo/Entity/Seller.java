package com.xuwenye.demo.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 卖家实体（对应 sys_seller 表）
 * 1.主键自增（IdType.AUTO）
 * 2.Java 字段用驼峰命名，@TableField 映射数据库蛇形列名
 * 3.逻辑删除字段 is_deleted（配合 @TableLogic）
 * <p>
 * @author ZuiM
 */
@Data                           // Lombok自动生成getter/setter/toString
@TableName("sys_seller")        // 对应数据库表名
public class Seller {
    @TableId(type = IdType.AUTO)  // 主键自增
    private Long id;
    @TableField("user_id")
    private Long userId;          // 关联登录账号ID（sys_user.id，一对一绑定）
    private String sellerName;    // 卖家名称
    private String address;       // 卖家地址
    private String sellerAvatar;  // 卖家头像URL
    private String sellerContact; // 卖家联系方式（电话）
    @TableField("create_time")
    private LocalDateTime createTime;  // 创建时间
    @TableField("update_time")
    private LocalDateTime updateTime;  // 更新时间

    // ===== 表单临时字段（@TableField(exist=false) 不映射数据库列） =====
    // 仅用于「新增卖家」表单传参：管理员填入登录账号的用户名/邮箱，服务端据此创建 sys_user 账号并绑定
    @TableField(exist = false)
    private String username;          // 登录账号用户名（为空时默认取卖家名称）
    @TableField(exist = false)
    private String email;             // 登录账号邮箱（选填）

    // 逻辑删除（MyBatis-Plus 需要这个注解识别逻辑删除字段）
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
