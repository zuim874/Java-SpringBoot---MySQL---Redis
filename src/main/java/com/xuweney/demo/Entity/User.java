package com.xuweney.demo.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
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
    private Integer status;                 //0禁用，1启用
    private String user_role;
    private LocalDateTime create_time;      //创建账号时间
    private LocalDateTime update_time;                 //上一次更新资料时间
    private LocalDateTime last_login_time;             //上一次登陆时间
    private Integer is_deleted;

    // 逻辑删除（MyBatis-Plus 需要这个注解识别逻辑删除字段）
    @TableLogic
    private Integer isDeleted;
}