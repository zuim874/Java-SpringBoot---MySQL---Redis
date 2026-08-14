package com.xuwenye.demo.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 买卖会话实体（对应 sys_conversation 表）
 * 1.每个买家与每个卖家唯一一个会话（uk_user_seller 唯一约束）
 * 2.记录未读数（买卖双方各自计数）与最后一条消息预览
 * <p>
 * @author ZuiM
 */
@Data
@TableName("sys_conversation")
public class Conversation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 买家用户ID，关联 sys_user.id */
    private Long userId;

    /** 卖家ID，关联 sys_seller.id */
    private Long sellerId;

    /** 最后一条消息内容（列表预览） */
    @TableField("last_message")
    private String lastMessage;

    /** 买家未读数 */
    private Integer unreadUser;

    /** 卖家未读数 */
    private Integer unreadSeller;

    /** 最后消息时间 */
    @TableField("last_time")
    private LocalDateTime lastTime;

    @TableField("create_time")
    private LocalDateTime createTime;
}
