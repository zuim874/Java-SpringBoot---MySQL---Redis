package com.xuwenye.demo.Controller.Chat;

import com.xuwenye.demo.Entity.ChatMessage;
import com.xuwenye.demo.Entity.Conversation;
import com.xuwenye.demo.Entity.Seller;
import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Service.ChatService;
import com.xuwenye.demo.Service.SellerService;
import com.xuwenye.demo.Service.UserService;
import com.xuwenye.demo.annotation.OperationLog;
import com.xuwenye.demo.annotation.RateLimit;
import com.xuwenye.demo.common.Result;
import com.xuwenye.demo.util.auth.JwtUtil;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 买卖双方聊天接口
 * 1.买家：会话列表、发送消息、拉取消息、标记已读
 * 2.卖家：会话列表、发送消息、拉取消息、标记已读
 * 3.所有接口需登录，且严格校验会话归属（越权访问直接 403）
 * <p>
 * @author ZuiM
 */
@RestController
@RequestMapping("/api/chat")
@Validated
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;
    private final SellerService sellerService;
    private final JwtUtil jwtUtil;

    public ChatController(ChatService chatService,
                          UserService userService,
                          SellerService sellerService,
                          JwtUtil jwtUtil) {
        this.chatService = chatService;
        this.userService = userService;
        this.sellerService = sellerService;
        this.jwtUtil = jwtUtil;
    }

    // ======================== 买家接口 ========================

    /**
     * 买家获取或创建与卖家的会话（用于打开聊天抽屉）
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param sellerId 卖家ID
     * @return Result 会话信息（含ID）
     */
    @PostMapping("/user/open")
    @RateLimit(window = 60, maxRequests = 30, message = "请求过于频繁，请稍后再试")
    public Result<?> userOpen(
            @RequestHeader("Authorization") String token,
            @RequestParam @Min(1) Long sellerId) {
        Long userId = validateLogin(token);
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期");
        }
        try {
            Conversation conv = chatService.getOrCreateConversation(userId, sellerId);
            return Result.ok(conv);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 买家会话列表
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @return Result 会话列表（含卖家信息与未读数）
     */
    @GetMapping("/user/conversations")
    @RateLimit(window = 60, maxRequests = 30, message = "请求过于频繁，请稍后再试")
    public Result<?> getUserConversations(@RequestHeader("Authorization") String token) {
        Long userId = validateLogin(token);
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期");
        }
        return Result.ok(chatService.getUserConversations(userId));
    }

    /**
     * 买家发送消息
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param sellerId 卖家ID
     * @param content 消息内容
     * @return Result 200 发送成功
     */
    @OperationLog("买家发送消息")
    @PostMapping("/user/send")
    @RateLimit(window = 60, maxRequests = 20, message = "发送过于频繁，请稍后再试")
    public Result<?> userSend(
            @RequestHeader("Authorization") String token,
            @RequestParam @Min(1) Long sellerId,
            @RequestParam @Size(min = 1, max = 500) String content) {
        Long userId = validateLogin(token);
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期");
        }
        try {
            ChatMessage msg = chatService.sendMessage(userId, sellerId, "USER", content);
            return Result.ok(msg);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 买家拉取会话消息
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param conversationId 会话ID
     * @param afterId 增量拉取起点（可选，为空则返回最近50条）
     * @return Result 消息列表
     */
    @GetMapping("/user/messages")
    @RateLimit(window = 60, maxRequests = 60, message = "请求过于频繁，请稍后再试")
    public Result<?> userMessages(
            @RequestHeader("Authorization") String token,
            @RequestParam @Min(1) Long conversationId,
            @RequestParam(required = false) Long afterId) {
        Long userId = validateLogin(token);
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期");
        }
        if (!checkUserConversationOwner(conversationId, userId)) {
            return Result.error(403, "无权访问该会话");
        }
        List<ChatMessage> messages = afterId == null
                ? chatService.getMessages(conversationId, 50)
                : chatService.getMessagesAfter(conversationId, afterId);
        return Result.ok(messages);
    }

    /**
     * 买家标记会话已读
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param conversationId 会话ID
     * @return Result 200 已读
     */
    @PostMapping("/user/read")
    @RateLimit(window = 60, maxRequests = 60, message = "请求过于频繁，请稍后再试")
    public Result<?> userRead(
            @RequestHeader("Authorization") String token,
            @RequestParam @Min(1) Long conversationId) {
        Long userId = validateLogin(token);
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期");
        }
        if (!checkUserConversationOwner(conversationId, userId)) {
            return Result.error(403, "无权访问该会话");
        }
        chatService.markRead(conversationId, "USER");
        return Result.ok("已读");
    }

    // ======================== 卖家接口 ========================

    /**
     * 卖家会话列表
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @return Result 会话列表（含买家信息与未读数）
     */
    @GetMapping("/seller/conversations")
    @RateLimit(window = 60, maxRequests = 30, message = "请求过于频繁，请稍后再试")
    public Result<?> getSellerConversations(@RequestHeader("Authorization") String token) {
        Long sellerId = validateSeller(token);
        if (sellerId == null) {
            return Result.error(403, "权限不足，仅商家可操作");
        }
        return Result.ok(chatService.getSellerConversations(sellerId));
    }

    /**
     * 卖家发送消息
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param userId 买家用户ID
     * @param content 消息内容
     * @return Result 200 发送成功
     */
    @OperationLog("卖家发送消息")
    @PostMapping("/seller/send")
    @RateLimit(window = 60, maxRequests = 20, message = "发送过于频繁，请稍后再试")
    public Result<?> sellerSend(
            @RequestHeader("Authorization") String token,
            @RequestParam @Min(1) Long userId,
            @RequestParam @Size(min = 1, max = 500) String content) {
        Long sellerId = validateSeller(token);
        if (sellerId == null) {
            return Result.error(403, "权限不足，仅商家可操作");
        }
        try {
            ChatMessage msg = chatService.sendMessage(userId, sellerId, "SELLER", content);
            return Result.ok(msg);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 卖家拉取会话消息
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param conversationId 会话ID
     * @param afterId 增量拉取起点（可选，为空则返回最近50条）
     * @return Result 消息列表
     */
    @GetMapping("/seller/messages")
    @RateLimit(window = 60, maxRequests = 60, message = "请求过于频繁，请稍后再试")
    public Result<?> sellerMessages(
            @RequestHeader("Authorization") String token,
            @RequestParam @Min(1) Long conversationId,
            @RequestParam(required = false) Long afterId) {
        Long sellerId = validateSeller(token);
        if (sellerId == null) {
            return Result.error(403, "权限不足，仅商家可操作");
        }
        if (!checkSellerConversationOwner(conversationId, sellerId)) {
            return Result.error(403, "无权访问该会话");
        }
        List<ChatMessage> messages = afterId == null
                ? chatService.getMessages(conversationId, 50)
                : chatService.getMessagesAfter(conversationId, afterId);
        return Result.ok(messages);
    }

    /**
     * 卖家标记会话已读
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param conversationId 会话ID
     * @return Result 200 已读
     */
    @PostMapping("/seller/read")
    @RateLimit(window = 60, maxRequests = 60, message = "请求过于频繁，请稍后再试")
    public Result<?> sellerRead(
            @RequestHeader("Authorization") String token,
            @RequestParam @Min(1) Long conversationId) {
        Long sellerId = validateSeller(token);
        if (sellerId == null) {
            return Result.error(403, "权限不足，仅商家可操作");
        }
        if (!checkSellerConversationOwner(conversationId, sellerId)) {
            return Result.error(403, "无权访问该会话");
        }
        chatService.markRead(conversationId, "SELLER");
        return Result.ok("已读");
    }

    // ======================== 内部工具方法 ========================

    /**
     * 校验登录，返回用户ID
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @return Long 用户ID（null=未登录）
     */
    private Long validateLogin(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            return null;
        }
        String realToken = token.substring(7);
        if (!jwtUtil.validate(realToken)) {
            return null;
        }
        String username = jwtUtil.parseUsername(realToken);
        User user = userService.findUserableUser(username);
        return user == null ? null : user.getId();
    }

    /**
     * 校验卖家身份，返回卖家ID
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @return Long 卖家ID（null=校验失败）
     */
    private Long validateSeller(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            return null;
        }
        String realToken = token.substring(7);
        if (!jwtUtil.validate(realToken)) {
            return null;
        }
        String username = jwtUtil.parseUsername(realToken);
        User user = userService.findAllUser(username);
        if (user == null || user.getUserRole() == null || !user.getUserRole().contains("SELLER")) {
            return null;
        }
        Seller seller = sellerService.getSellerBySellerName(username);
        return seller == null ? null : seller.getId();
    }

    /**
     * 校验会话是否为该买家所有
     * <p>
     * @author ZuiM
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @return boolean true=归属正确
     */
    private boolean checkUserConversationOwner(Long conversationId, Long userId) {
        Conversation conv = chatService.getConversationById(conversationId);
        return conv != null && conv.getUserId().equals(userId);
    }

    /**
     * 校验会话是否为该卖家所有
     * <p>
     * @author ZuiM
     * @param conversationId 会话ID
     * @param sellerId 卖家ID
     * @return boolean true=归属正确
     */
    private boolean checkSellerConversationOwner(Long conversationId, Long sellerId) {
        Conversation conv = chatService.getConversationById(conversationId);
        return conv != null && conv.getSellerId().equals(sellerId);
    }
}
