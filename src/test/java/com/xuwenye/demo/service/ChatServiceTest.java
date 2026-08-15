package com.xuwenye.demo.service;

import com.xuwenye.demo.Entity.ChatMessage;
import com.xuwenye.demo.Entity.Conversation;
import com.xuwenye.demo.Entity.Seller;
import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Mapper.ChatMessageMapper;
import com.xuwenye.demo.Mapper.ConversationMapper;
import com.xuwenye.demo.Mapper.SellerMapper;
import com.xuwenye.demo.Service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 聊天业务层集成测试（需 MySQL + Redis + ActiveMQ）
 * 1.会话管理：创建/获取/查询
 * 2.消息发送：买家/卖家发送消息，未读数更新
 * 3.消息读取：拉取最近消息、增量轮询
 * 4.已读回执：标记已读并清零未读数
 * 5.Redis 缓存：会话/消息的 Cache-Aside 模式
 * <p>
 * @author ZuiM
 */
class ChatServiceTest extends AbstractServiceTest {

    @Autowired
    private ChatService chatService;

    @Autowired
    private SellerMapper sellerMapper;

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    // ========== 测试数据构造 ==========

    private User buildUser() {
        User user = new User();
        user.setUsername(uniqueName("chat_user"));
        user.setPassword("$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa");
        user.setNickname("聊天测试用户" + SUFFIX);
        user.setEmail(uniqueName("chat") + "@test.com");
        user.setStatus(1);
        user.setUserRole("ROLE_USER");
        user.setBalance(BigDecimal.ZERO);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    private Seller buildSeller() {
        Seller seller = new Seller();
        seller.setSellerName("聊天测试卖家" + SUFFIX);
        seller.setAddress("聊天测试地址" + SUFFIX);
        seller.setSellerContact("13800000000");
        seller.setCreateTime(LocalDateTime.now());
        sellerMapper.insert(seller);
        return seller;
    }

    private void cleanData(User user, Seller seller) {
        if (user != null) {
            trackCacheKey("demo:chat:userConversations:" + user.getId());
            trackCacheKey("demo:chat:userConversations:" + user.getId() + ":*");
            userMapper.deleteById(user.getId());
        }
        if (seller != null) {
            trackCacheKey("demo:chat:sellerConversations:" + seller.getId());
            trackCacheKey("demo:chat:sellerConversations:" + seller.getId() + ":*");
            trackCacheKey("demo:seller:detail:" + seller.getId());
            sellerMapper.deleteById(seller.getId());
        }
    }

    // ========== 1. 会话管理 ==========

    /**
     * 买家与卖家创建会话，重复调用返回同一会话
     * <p>
     * @author ZuiM
     */
    @Test
    void 创建会话重复调用返回同一会话() {
        User user = buildUser();
        Seller seller = buildSeller();

        Conversation conv1 = chatService.getOrCreateConversation(user.getId(), seller.getId());
        assertNotNull(conv1.getId());
        Conversation conv2 = chatService.getOrCreateConversation(user.getId(), seller.getId());
        assertEquals(conv1.getId(), conv2.getId());

        cleanData(user, seller);
    }

    /**
     * 按会话ID查询会话详情（含缓存）
     * <p>
     * @author ZuiM
     */
    @Test
    void 按id查询会话详情() {
        User user = buildUser();
        Seller seller = buildSeller();
        Conversation conv = chatService.getOrCreateConversation(user.getId(), seller.getId());

        assertNotNull(chatService.getConversationById(conv.getId()));

        trackCacheKey("demo:chat:conversation:" + conv.getId());
        cleanData(user, seller);
    }

    // ========== 2. 消息发送 ==========

    /**
     * 买家发送消息后卖家未读数 +1
     * <p>
     * @author ZuiM
     */
    @Test
    void 买家发消息后卖家未读数增加() {
        User user = buildUser();
        Seller seller = buildSeller();
        Conversation conv = chatService.getOrCreateConversation(user.getId(), seller.getId());

        ChatMessage msg = chatService.sendMessage(user.getId(), seller.getId(), "USER", "你好，我想咨询一下商品");
        assertNotNull(msg.getId());
        assertEquals("USER", msg.getSenderRole());

        // 卖家未读数应为 1
        Conversation updated = conversationMapper.selectById(conv.getId());
        assertEquals(1, updated.getUnreadSeller());

        trackCacheKey("demo:chat:conversation:" + conv.getId());
        trackCacheKey("demo:chat:messages:" + conv.getId() + ":*");
        cleanData(user, seller);
    }

    /**
     * 卖家回复消息后买家未读数 +1
     * <p>
     * @author ZuiM
     */
    @Test
    void 卖家发消息后买家未读数增加() {
        User user = buildUser();
        Seller seller = buildSeller();
        Conversation conv = chatService.getOrCreateConversation(user.getId(), seller.getId());

        chatService.sendMessage(user.getId(), seller.getId(), "SELLER", "您好，有什么可以帮您的？");
        Conversation updated = conversationMapper.selectById(conv.getId());
        assertEquals(1, updated.getUnreadUser());

        trackCacheKey("demo:chat:conversation:" + conv.getId());
        trackCacheKey("demo:chat:messages:" + conv.getId() + ":*");
        cleanData(user, seller);
    }

    /**
     * 空消息发送被拒绝
     * <p>
     * @author ZuiM
     */
    @Test
    void 空消息发送被拒绝() {
        User user = buildUser();
        Seller seller = buildSeller();
        chatService.getOrCreateConversation(user.getId(), seller.getId());

        assertThrows(IllegalArgumentException.class,
                () -> chatService.sendMessage(user.getId(), seller.getId(), "USER", ""));
        assertThrows(IllegalArgumentException.class,
                () -> chatService.sendMessage(user.getId(), seller.getId(), "USER", "   "));

        cleanData(user, seller);
    }

    // ========== 3. 消息读取 ==========

    /**
     * 拉取最近消息（正序，包含所有已发送消息）
     * <p>
     * @author ZuiM
     */
    @Test
    void 拉取最近消息() {
        User user = buildUser();
        Seller seller = buildSeller();
        Conversation conv = chatService.getOrCreateConversation(user.getId(), seller.getId());

        chatService.sendMessage(user.getId(), seller.getId(), "USER", "消息1");
        chatService.sendMessage(user.getId(), seller.getId(), "SELLER", "消息2");

        List<ChatMessage> messages = chatService.getMessages(conv.getId(), 10);
        assertEquals(2, messages.size());
        assertEquals("消息1", messages.get(0).getContent());
        assertEquals("消息2", messages.get(1).getContent());

        trackCacheKey("demo:chat:conversation:" + conv.getId());
        trackCacheKey("demo:chat:messages:" + conv.getId() + ":*");
        cleanData(user, seller);
    }

    /**
     * 增量拉取消息（afterId 之后的新消息）
     * <p>
     * @author ZuiM
     */
    @Test
    void 增量拉取新消息() {
        User user = buildUser();
        Seller seller = buildSeller();
        Conversation conv = chatService.getOrCreateConversation(user.getId(), seller.getId());

        ChatMessage first = chatService.sendMessage(user.getId(), seller.getId(), "USER", "第一条消息");
        List<ChatMessage> after = chatService.getMessagesAfter(conv.getId(), first.getId());
        assertTrue(after.isEmpty());

        chatService.sendMessage(user.getId(), seller.getId(), "SELLER", "第二条消息");
        after = chatService.getMessagesAfter(conv.getId(), first.getId());
        assertEquals(1, after.size());
        assertEquals("第二条消息", after.get(0).getContent());

        trackCacheKey("demo:chat:conversation:" + conv.getId());
        trackCacheKey("demo:chat:messages:" + conv.getId() + ":*");
        cleanData(user, seller);
    }

    // ========== 4. 已读回执 ==========

    /**
     * 买家已读后卖家发送的消息标记已读，买家未读数清零
     * <p>
     * @author ZuiM
     */
    @Test
    void 买家已读后消息标记已读未读数清零() {
        User user = buildUser();
        Seller seller = buildSeller();
        Conversation conv = chatService.getOrCreateConversation(user.getId(), seller.getId());

        // 卖家发消息给买家
        chatService.sendMessage(user.getId(), seller.getId(), "SELLER", "卖家消息");

        // 买家已读
        chatService.markRead(conv.getId(), "USER");

        // 消息已读
        List<ChatMessage> messages = chatMessageMapper.findRecent(conv.getId(), 10);
        assertTrue(messages.stream().allMatch(m -> m.getIsRead() == 1));

        // 买家未读数清零
        Conversation updated = conversationMapper.selectById(conv.getId());
        assertEquals(0, updated.getUnreadUser());

        trackCacheKey("demo:chat:conversation:" + conv.getId());
        trackCacheKey("demo:chat:messages:" + conv.getId() + ":*");
        cleanData(user, seller);
    }

    // ========== 5. 会话列表 ==========

    /**
     * 买家会话列表包含卖家信息
     * <p>
     * @author ZuiM
     */
    @Test
    void 买家会话列表包含卖家信息() {
        User user = buildUser();
        Seller seller = buildSeller();
        chatService.getOrCreateConversation(user.getId(), seller.getId());
        chatService.sendMessage(user.getId(), seller.getId(), "USER", "咨询消息");

        List<Map<String, Object>> list = chatService.getUserConversations(user.getId());
        assertFalse(list.isEmpty());
        assertTrue(list.stream().anyMatch(item -> seller.getSellerName().equals(item.get("sellerName"))));

        trackCacheKey("demo:chat:userConversations:" + user.getId());
        trackCacheKey("demo:chat:sellerConversations:" + seller.getId());
        trackCacheKey("demo:chat:conversation:" + list.get(0).get("id"));
        cleanData(user, seller);
    }

    /**
     * 卖家会话列表包含买家信息
     * <p>
     * @author ZuiM
     */
    @Test
    void 卖家会话列表包含买家信息() {
        User user = buildUser();
        Seller seller = buildSeller();
        chatService.getOrCreateConversation(user.getId(), seller.getId());
        chatService.sendMessage(user.getId(), seller.getId(), "USER", "咨询消息");

        List<Map<String, Object>> list = chatService.getSellerConversations(seller.getId());
        assertFalse(list.isEmpty());
        assertTrue(list.stream().anyMatch(item -> user.getNickname().equals(item.get("nickname"))));

        trackCacheKey("demo:chat:userConversations:" + user.getId());
        trackCacheKey("demo:chat:sellerConversations:" + seller.getId());
        cleanData(user, seller);
    }
}