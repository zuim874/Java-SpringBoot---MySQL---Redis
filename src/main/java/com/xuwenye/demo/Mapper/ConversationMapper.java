package com.xuwenye.demo.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuwenye.demo.Entity.Conversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 买卖会话数据访问层
 * 1.基础 CRUD 由 BaseMapper 自动生成
 * 2.自定义操作：买家/卖家会话列表、按双方ID定位会话、未读清零
 * <p>
 * @author ZuiM
 */
@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {

    /**
     * 查询买家的会话列表（按最后消息时间倒序）
     * <p>
     * @author ZuiM
     * @param userId 买家用户ID
     * @return List<Conversation> 会话列表
     */
    @Select("SELECT * FROM sys_conversation WHERE user_id = #{userId} ORDER BY last_time DESC")
    List<Conversation> findUserConversations(@Param("userId") Long userId);

    /**
     * 查询卖家的会话列表（按最后消息时间倒序）
     * <p>
     * @author ZuiM
     * @param sellerId 卖家ID
     * @return List<Conversation> 会话列表
     */
    @Select("SELECT * FROM sys_conversation WHERE seller_id = #{sellerId} ORDER BY last_time DESC")
    List<Conversation> findSellerConversations(@Param("sellerId") Long sellerId);

    /**
     * 根据买家与卖家ID定位会话（不存在返回 null）
     * <p>
     * @author ZuiM
     * @param userId 买家用户ID
     * @param sellerId 卖家ID
     * @return Conversation 会话（可能为 null）
     */
    @Select("SELECT * FROM sys_conversation WHERE user_id = #{userId} AND seller_id = #{sellerId} LIMIT 1")
    Conversation findByUserAndSeller(@Param("userId") Long userId, @Param("sellerId") Long sellerId);

    /**
     * 买家侧未读清零
     * <p>
     * @author ZuiM
     * @param id 会话ID
     */
    @Update("UPDATE sys_conversation SET unread_user = 0 WHERE id = #{id}")
    void clearUserUnread(@Param("id") Long id);

    /**
     * 卖家侧未读清零
     * <p>
     * @author ZuiM
     * @param id 会话ID
     */
    @Update("UPDATE sys_conversation SET unread_seller = 0 WHERE id = #{id}")
    void clearSellerUnread(@Param("id") Long id);
}
