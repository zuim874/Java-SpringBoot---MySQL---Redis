package com.xuwenye.demo.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体（对应 sys_product 表）
 * 1.主键自增（IdType.AUTO）
 * 2.Java 字段用驼峰命名，@TableField 映射数据库蛇形列名
 * 3.逻辑删除字段 is_deleted（配合 @TableLogic）
 * <p>
 * @author ZuiM
 */
@Data                           // Lombok自动生成getter/setter/toString
@TableName("sys_product")       // 对应数据库表名
public class Product {
    @TableId(type = IdType.AUTO)  // 主键自增
    private Long id;
    private Long sellerId;        // 卖家ID，关联sys_seller.id
    private String productName;   // 商品名称
    private BigDecimal price;     // 商品价格(元)
    private Integer stock;        // 商品库存数量
    private Integer sold;         // 已售出数量
    private Integer status;       // 商品状态：0下架 1上架
    private String description;   // 商品描述
    private String category;      // 商品分类（如：手机配件、电脑外设等）
    private String mainImageUrl;  // 冗余：商品主图URL
    /** 推荐位：0普通 1推荐（会员卖家权益，商城置顶曝光） */
    private Integer recommend;
    @TableField("create_time")
    private LocalDateTime createTime;  // 创建时间
    @TableField("update_time")
    private LocalDateTime updateTime;  // 更新时间

    // 逻辑删除（MyBatis-Plus 需要这个注解识别逻辑删除字段）
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
