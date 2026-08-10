package com.xuwenye.demo.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 商品图片实体（对应 product_image 表）
 * 1.主键自增（IdType.AUTO）
 * 2.商品与图片为一对多关系
 * 3.支持主图标记和排序
 * <p>
 * @author ZuiM
 */
@Data                           // Lombok自动生成getter/setter/toString
@TableName("product_image")     // 对应数据库表名
public class ProductImage {
    @TableId(type = IdType.AUTO)  // 主键自增
    private Long id;
    private Long productId;       // 关联商品id
    private String imageUrl;      // 图片访问地址
    private Integer sort;         // 排序号，数字越小越靠前
    private Integer isMain;       // 0普通图 1主图
    @TableField("create_time")
    private LocalDateTime createTime;  // 创建时间

    // 逻辑删除（MyBatis-Plus 需要这个注解识别逻辑删除字段）
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
