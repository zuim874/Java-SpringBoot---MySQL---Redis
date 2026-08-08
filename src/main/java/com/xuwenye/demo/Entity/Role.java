package com.xuwenye.demo.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色实体（对应 sys_role 表）
 * 1.主键自增
 * 2.Java 字段驼峰命名，@TableField 映射数据库列
 * <p>
 * @author ZuiM
 */
@Data
@TableName("sys_role")
public class Role {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色编码 */
    @TableField("role_code")
    private String roleCode;

    /** 角色名称 */
    @TableField("role_name")
    private String roleName;

    /** 角色描述 */
    private String description;

    /** 状态：0禁用，1启用 */
    private Integer status;

    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
