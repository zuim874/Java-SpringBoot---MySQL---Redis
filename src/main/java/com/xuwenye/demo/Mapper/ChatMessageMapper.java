package com.xuwenye.demo.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuwenye.demo.Entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 聊天消息数据访问层
 * 1.基础 CRUD 由 BaseMapper 自动生成
 * 2.自定义操作：按会话拉取消息、标记已读
 * <p>
 * @author ZuiM
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 拉取会话消息（分页，时间倒序取最近N条后正序返回）
     * <p>
     * @author ZuiM
     * @param conversationId 会话ID
     * @param limit 消息条数
     * @return List<ChatMessage> 消息列表（正序）
     */
    @Select("SELECT * FROM (SELECT * FROM sys_chat_message WHERE conversation_id = #{conversationId} " +
            "ORDER BY id DESC LIMIT #{limit}) t ORDER BY id ASC")
    List<ChatMessage> findRecent(@Param("conversationId") Long conversationId, @Param("limit") int limit);

    /**
     * 按ID拉取增量消息（轮询用：拉取上次之后的新消息）
     * <p>
     * @author ZuiM
     * @param conversationId 会话ID
     * @param afterId 上次拉取的最大消息ID
     * @return List<ChatMessage> 新消息列表（正序）
     */
    @Select("SELECT * FROM sys_chat_message WHERE conversation_id = #{conversationId} AND id > #{afterId} ORDER BY id ASC")
    List<ChatMessage> findAfter(@Param("conversationId") Long conversationId, @Param("afterId") Long afterId);

    /**
     * 将指定会话中、来自指定身份的未读消息标记为已读
     * <p>
     * @author ZuiM
     * @param conversationId 会话ID
     * @param senderRole 对方身份（USER/SELLER），标记该身份发来的消息为已读
     * @return int 受影响行数
     */
    @Update("UPDATE sys_chat_message SET is_read = 1 " +
            "WHERE conversation_id = #{conversationId} AND sender_role = #{senderRole} AND is_read = 0")
    int markRead(@Param("conversationId") Long conversationId, @Param("senderRole") String senderRole);
}
