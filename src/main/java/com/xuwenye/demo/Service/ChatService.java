package com.xuwenye.demo.Service;

import com.xuwenye.demo.Entity.ChatMessage;
import com.xuwenye.demo.Entity.Conversation;
import com.xuwenye.demo.Entity.Seller;
import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Mapper.ChatMessageMapper;
import com.xuwenye.demo.Mapper.ConversationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 买卖双方聊天业务层
 * 1.会话：按买家+卖家唯一会话（不存在则创建）
 * 2.发送：写入消息并更新会话最后消息/未读数（对方 +1）
 * 3.读取：买家/卖家各自的会话列表、会话消息（支持增量轮询）
 * 4.已读：标记对方消息已读并清零未读数
 * <p>
 * @author ZuiM
 */
@Service
@Slf4j
public class ChatService {

    private final ConversationMapper conversationMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final SellerService sellerService;
    private final UserService userService;

    public ChatService(ConversationMapper conversationMapper,
                       ChatMessageMapper chatMessageMapper,
                       SellerService sellerService,
                       UserService userService) {
        this.conversationMapper = conversationMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.sellerService = sellerService;
        this.userService = userService;
    }

    /**
     * 获取或创建会话
     * <p>
     * @author ZuiM
     * @param userId 买家用户ID
     * @param sellerId 卖家ID
     * @return Conversation 会话
     */
    @Transactional
    public Conversation getOrCreateConversation(Long userId, Long sellerId) {
        Seller seller = sellerService.getSellerById(sellerId);
        if (seller == null) {
            throw new IllegalArgumentException("卖家不存在");
        }
        Conversation conv = conversationMapper.findByUserAndSeller(userId, sellerId);
        if (conv == null) {
            conv = new Conversation();
            conv.setUserId(userId);
            conv.setSellerId(sellerId);
            conv.setUnreadUser(0);
            conv.setUnreadSeller(0);
            conv.setLastMessage("");
            conv.setLastTime(LocalDateTime.now());
            conversationMapper.insert(conv);
        }
        return conv;
    }

    /**
     * 发送消息
     * 1.校验消息长度与发送者身份
     * 2.写入消息，更新会话最后消息与对方未读数
     * <p>
     * @author ZuiM
     * @param userId 买家用户ID（本人）
     * @param sellerId 卖家ID
     * @param senderRole 发送者身份：USER买家 / SELLER卖家
     * @param content 消息内容
     * @return ChatMessage 发送成功的消息
     */
    @Transactional
    public ChatMessage sendMessage(Long userId, Long sellerId, String senderRole, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }
        String realContent = content.trim();
        if (realContent.length() > 500) {
            throw new IllegalArgumentException("消息内容不能超过 500 字");
        }
        if (!"USER".equals(senderRole) && !"SELLER".equals(senderRole)) {
            throw new IllegalArgumentException("非法的发送者身份");
        }
        Conversation conv = getOrCreateConversation(userId, sellerId);

        ChatMessage msg = new ChatMessage();
        msg.setConversationId(conv.getId());
        msg.setSenderId("USER".equals(senderRole) ? userId : sellerId);
        msg.setSenderRole(senderRole);
        msg.setContent(realContent);
        msg.setIsRead(0);
        msg.setCreateTime(LocalDateTime.now());
        chatMessageMapper.insert(msg);

        // 更新会话最后消息与对方未读数
        Conversation update = new Conversation();
        update.setId(conv.getId());
        update.setLastMessage(realContent);
        update.setLastTime(LocalDateTime.now());
        if ("USER".equals(senderRole)) {
            update.setUnreadSeller(conv.getUnreadSeller() + 1);
        } else {
            update.setUnreadUser(conv.getUnreadUser() + 1);
        }
        conversationMapper.updateById(update);
        return msg;
    }

    /**
     * 根据会话ID查询会话（含归属校验用途）
     * <p>
     * @author ZuiM
     * @param conversationId 会话ID
     * @return Conversation 会话（可能为 null）
     */
    public Conversation getConversationById(Long conversationId) {
        return conversationMapper.selectById(conversationId);
    }

    /**
     * 查询买家的会话列表（附带卖家信息与未读数）
     * <p>
     * @author ZuiM
     * @param userId 买家用户ID
     * @return List<Map<String,Object>> 会话列表
     */
    public List<Map<String, Object>> getUserConversations(Long userId) {
        List<Conversation> list = conversationMapper.findUserConversations(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Conversation c : list) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", c.getId());
            item.put("sellerId", c.getSellerId());
            item.put("lastMessage", c.getLastMessage());
            item.put("unread", c.getUnreadUser());
            item.put("lastTime", c.getLastTime());
            Seller seller = sellerService.getSellerById(c.getSellerId());
            if (seller != null) {
                item.put("sellerName", seller.getSellerName());
                item.put("sellerAvatar", seller.getSellerAvatar());
            }
            result.add(item);
        }
        return result;
    }

    /**
     * 查询卖家会话列表（附带买家信息与未读数）
     * <p>
     * @author ZuiM
     * @param sellerId 卖家ID
     * @return List<Map<String,Object>> 会话列表
     */
    public List<Map<String, Object>> getSellerConversations(Long sellerId) {
        List<Conversation> list = conversationMapper.findSellerConversations(sellerId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Conversation c : list) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", c.getId());
            item.put("userId", c.getUserId());
            item.put("lastMessage", c.getLastMessage());
            item.put("unread", c.getUnreadSeller());
            item.put("lastTime", c.getLastTime());
            User user = userService.getUserById(c.getUserId());
            if (user != null) {
                item.put("username", user.getUsername());
                item.put("nickname", user.getNickname());
                item.put("avatar", user.getAvatar());
            }
            result.add(item);
        }
        return result;
    }

    /**
     * 拉取会话消息（最近 limit 条，正序）
     * <p>
     * @author ZuiM
     * @param conversationId 会话ID
     * @param limit 条数
     * @return List<ChatMessage> 消息列表
     */
    public List<ChatMessage> getMessages(Long conversationId, int limit) {
        return chatMessageMapper.findRecent(conversationId, Math.max(1, Math.min(limit, 200)));
    }

    /**
     * 增量拉取消息（轮询，afterId 之后的新消息）
     * <p>
     * @author ZuiM
     * @param conversationId 会话ID
     * @param afterId 上次拉取的最大消息ID
     * @return List<ChatMessage> 新消息列表
     */
    public List<ChatMessage> getMessagesAfter(Long conversationId, Long afterId) {
        if (afterId == null) {
            afterId = 0L;
        }
        return chatMessageMapper.findAfter(conversationId, afterId);
    }

    /**
     * 将会话中对方发来的消息标记为已读，并清零本方未读数
     * <p>
     * @author ZuiM
     * @param conversationId 会话ID
     * @param selfRole 本方身份（USER买家 / SELLER卖家）
     */
    @Transactional
    public void markRead(Long conversationId, String selfRole) {
        if ("USER".equals(selfRole)) {
            // 标记卖家发来的消息已读，并清零买家未读数
            chatMessageMapper.markRead(conversationId, "SELLER");
            conversationMapper.clearUserUnread(conversationId);
        } else if ("SELLER".equals(selfRole)) {
            chatMessageMapper.markRead(conversationId, "USER");
            conversationMapper.clearSellerUnread(conversationId);
        }
    }
}
