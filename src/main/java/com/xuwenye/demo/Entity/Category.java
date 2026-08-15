package com.xuwenye.demo.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品分类实体（对应 sys_category 表）
 * 1.分类由管理员预设维护，卖家新增/编辑商品时从本表拉取并多选
 * 2.商品的 category 字段存储本表 id 集合（英文逗号分隔，支持多分类）
 * 3.主键自增（IdType.AUTO），Java 字段用驼峰命名
 * <p>
 * @author ZuiM
 */
@Data
@TableName("sys_category")
public class Category {
    @TableId(type = IdType.AUTO)
    private Long id;                 // 主键ID
    private String name;             // 分类名称（唯一）
    private Integer sort;            // 排序号，数字越小越靠前
    private Integer status;          // 状态：0禁用 1启用
    @TableField("create_time")
    private LocalDateTime createTime; // 创建时间
    @TableField("update_time")
    private LocalDateTime updateTime; // 更新时间
}
