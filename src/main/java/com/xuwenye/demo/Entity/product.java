package com.xuwenye.demo.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体（对应 sys_product 表）
 * 1.主键自增（IdType.AUTO）
 * 2.Java 字段用驼峰命名，@TableField 映射数据库蛇形列名
 * 3.逻辑删除字段 is_deleted
 * 4.关联卖家 ID（sellerId），查询时需关联 sys_seller 表
 * <p>
 * @author ZuiM
 */
@Data
@TableName("sys_product")
public class Product {
    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 卖家ID，关联 sys_seller.id */
    @TableField("seller_id")
    private Long sellerId;

    /** 商品名称 */
    @TableField("product_name")
    private String productName;

    /** 商品价格（元） */
    private BigDecimal price;

    /** 商品库存数量 */
    private Integer stock;

    /** 已售出数量 */
    private Integer sold;

    /** 商品状态：0下架 1上架 */
    private Integer status;

    /** 商品描述 */
    private String description;

    /** 商品分类（手机配件/电脑外设/音频设备/智能家居/穿戴设备/摄影器材/其他） */
    private String category;

    /** 冗余：商品主图URL，列表页查询优化 */
    @TableField("main_image_url")
    private String mainImageUrl;

    /** 卖家名称（非数据库字段，由左连接查询填充） */
    @TableField(exist = false)
    private String sellerName;

    /** 逻辑删除：0未删除 1已删除 */
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;

    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField("update_time")
    private LocalDateTime updateTime;
}