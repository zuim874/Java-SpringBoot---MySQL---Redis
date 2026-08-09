package com.xuwenye.demo.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 卖家实体（对应 sys_seller 表）
 * 1.主键自增（IdType.AUTO）
 * 2.Java 字段用驼峰命名，@TableField 映射数据库蛇形列名
 * 3.逻辑删除字段 is_deleted
 * 4.商品查询时携带卖家名称，减少前端联查
 * <p>
 * @author ZuiM
 */
@Data
@TableName("sys_seller")
public class Seller {
    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 卖家名称 */
    @TableField("seller_name")
    private String sellerName;

    /** 卖家头像URL（店铺Logo） */
    @TableField("seller_avatar")
    private String sellerAvatar;

    /** 卖家联系方式（客服电话/邮箱） */
    @TableField("seller_contact")
    private String sellerContact;

    /** 卖家地址 */
    private String address;

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