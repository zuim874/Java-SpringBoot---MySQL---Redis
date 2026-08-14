package com.xuwenye.demo.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息实体（对应 sys_chat_message 表）
 * 1.归属于某个会话（conversation_id）
 * 2.senderRole：USER买家 / SELLER卖家
 * 3.is_read 用于未读计数与已读回执
 * <p>
 * @author ZuiM
 */
@Data
@TableName("sys_chat_message")
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话ID，关联 sys_conversation.id */
    private Long conversationId;

    /** 发送者ID（用户ID或卖家ID） */
    private Long senderId;

    /** 发送者身份：USER买家 / SELLER卖家 */
    private String senderRole;

    /** 消息内容 */
    private String content;

    /** 是否已读：0未读 1已读 */
    private Integer isRead;

    @TableField("create_time")
    private LocalDateTime createTime;
}
