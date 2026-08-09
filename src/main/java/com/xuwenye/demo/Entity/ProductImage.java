package com.xuwenye.demo.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 商品图片实体（对应 product_image 表）
 * 1.主键自增（IdType.AUTO）
 * 2.Java 字段用驼峰命名，@TableField 映射数据库蛇形列名
 * 3.逻辑删除字段 is_deleted
 * 4.一对多关联：一个商品有多张图片（由 product_id 关联）
 * 5.is_main 标记主图，sort 控制展示顺序
 * <p>
 * @author ZuiM
 */
@Data
@TableName("product_image")
public class ProductImage {
    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联商品ID */
    @TableField("product_id")
    private Long productId;

    /** 图片访问地址 */
    @TableField("image_url")
    private String imageUrl;

    /** 排序号，数字越小越靠前 */
    private Integer sort;

    /** 0普通图 1主图 */
    @TableField("is_main")
    private Integer isMain;

    /** 逻辑删除：0未删除 1已删除 */
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;

    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;
}