package com.xuwenye.demo.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

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