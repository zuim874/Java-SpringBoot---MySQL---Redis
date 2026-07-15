package com.xuweney.demo.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
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
    private Integer status;      //0禁用，1启用
    private LocalDateTime createTime;
}