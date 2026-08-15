package com.xuwenye.demo.Controller.Chat;

import com.xuwenye.demo.Entity.ChatMessage;
import com.xuwenye.demo.Entity.Conversation;
import com.xuwenye.demo.Entity.Seller;
import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Service.ChatService;
import com.xuwenye.demo.Service.SellerService;
import com.xuwenye.demo.annotation.OperationLog;
import com.xuwenye.demo.annotation.RateLimit;
import com.xuwenye.demo.annotation.UserCheck;
import com.xuwenye.demo.common.Result;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 买卖双方聊天接口
 * 1.买家：会话列表、发送消息、拉取消息、标记已读
 * 2.卖家：会话列表、发送消息、拉取消息、标记已读
 * 3.所有接口需登录，且严格校验会话归属（越权访问直接 403）
 * 4.登录态与卖家角色校验统一由 @UserCheck 切面完成
 * <p>
 * @author ZuiM
 */
@RestController
@RequestMapping("/api/chat")
@Validated
public class ChatController {

    private final ChatService chatService;
    private final SellerService sellerService;

    public ChatController(ChatService chatService,
                          SellerService sellerService) {
        this.chatService = chatService;
        this.sellerService = sellerService;
    }

    // ======================== 买家接口 ========================

    /**
     * 买家获取或创建与卖家的会话（用于打开聊天抽屉）
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @param sellerId 卖家ID
     * @return Result 会话信息（含ID）
     */
    @UserCheck
    @PostMapping("/user/open")
    @RateLimit(window = 60, maxRequests = 30, message = "请求过于频繁，请稍后再试")
    public Result<?> userOpen(
            User currentUser,
            @RequestParam @Min(1) Long sellerId) {
        try {
            Conversation conv = chatService.getOrCreateConversation(currentUser.getId(), sellerId);
            return Result.ok(conv);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 买家会话列表
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @return Result 会话列表（含卖家信息与未读数）
     */
    @UserCheck
    @GetMapping("/user/conversations")
    @RateLimit(window = 60, maxRequests = 30, message = "请求过于频繁，请稍后再试")
    public Result<?> getUserConversations(User currentUser) {
        return Result.ok(chatService.getUserConversations(currentUser.getId()));
    }

    /**
     * 买家发送消息
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @param sellerId 卖家ID
     * @param content 消息内容
     * @return Result 200 发送成功
     */
    @OperationLog("买家发送消息")
    @UserCheck
    @PostMapping("/user/send")
    @RateLimit(window = 60, maxRequests = 20, message = "发送过于频繁，请稍后再试")
    public Result<?> userSend(
            User currentUser,
            @RequestParam @Min(1) Long sellerId,
            @RequestParam @Size(min = 1, max = 500) String content) {
        try {
            ChatMessage msg = chatService.sendMessage(currentUser.getId(), sellerId, "USER", content);
            return Result.ok(msg);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 买家拉取会话消息
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @param conversationId 会话ID
     * @param afterId 增量拉取起点（可选，为空则返回最近50条）
     * @return Result 消息列表
     */
    @UserCheck
    @GetMapping("/user/messages")
    @RateLimit(window = 60, maxRequests = 60, message = "请求过于频繁，请稍后再试")
    public Result<?> userMessages(
            User currentUser,
            @RequestParam @Min(1) Long conversationId,
            @RequestParam(required = false) Long afterId) {
        if (!checkUserConversationOwner(conversationId, currentUser.getId())) {
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
     * @param currentUser 当前登录用户（切面注入）
     * @param conversationId 会话ID
     * @return Result 200 已读
     */
    @UserCheck
    @PostMapping("/user/read")
    @RateLimit(window = 60, maxRequests = 60, message = "请求过于频繁，请稍后再试")
    public Result<?> userRead(
            User currentUser,
            @RequestParam @Min(1) Long conversationId) {
        if (!checkUserConversationOwner(conversationId, currentUser.getId())) {
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
     * @param currentUser 当前登录用户（切面注入，需 ROLE_SELLER 角色）
     * @return Result 会话列表（含买家信息与未读数）
     */
    @UserCheck(roles = {"ROLE_SELLER", "ROLE_VIP_SELLER"}, roleErrorMessage = "权限不足，仅商家可操作")
    @GetMapping("/seller/conversations")
    @RateLimit(window = 60, maxRequests = 30, message = "请求过于频繁，请稍后再试")
    public Result<?> getSellerConversations(User currentUser) {
        Long sellerId = resolveSellerId(currentUser);
        if (sellerId == null) {
            return Result.error(403, "权限不足，仅商家可操作");
        }
        return Result.ok(chatService.getSellerConversations(sellerId));
    }

    /**
     * 卖家发送消息
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入，需 ROLE_SELLER 角色）
     * @param userId 买家用户ID
     * @param content 消息内容
     * @return Result 200 发送成功
     */
    @OperationLog("卖家发送消息")
    @UserCheck(roles = {"ROLE_SELLER", "ROLE_VIP_SELLER"}, roleErrorMessage = "权限不足，仅商家可操作")
    @PostMapping("/seller/send")
    @RateLimit(window = 60, maxRequests = 20, message = "发送过于频繁，请稍后再试")
    public Result<?> sellerSend(
            User currentUser,
            @RequestParam @Min(1) Long userId,
            @RequestParam @Size(min = 1, max = 500) String content) {
        Long sellerId = resolveSellerId(currentUser);
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
     * @param currentUser 当前登录用户（切面注入，需 ROLE_SELLER 角色）
     * @param conversationId 会话ID
     * @param afterId 增量拉取起点（可选，为空则返回最近50条）
     * @return Result 消息列表
     */
    @UserCheck(roles = {"ROLE_SELLER", "ROLE_VIP_SELLER"}, roleErrorMessage = "权限不足，仅商家可操作")
    @GetMapping("/seller/messages")
    @RateLimit(window = 60, maxRequests = 60, message = "请求过于频繁，请稍后再试")
    public Result<?> sellerMessages(
            User currentUser,
            @RequestParam @Min(1) Long conversationId,
            @RequestParam(required = false) Long afterId) {
        Long sellerId = resolveSellerId(currentUser);
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
     * @param currentUser 当前登录用户（切面注入，需 ROLE_SELLER 角色）
     * @param conversationId 会话ID
     * @return Result 200 已读
     */
    @UserCheck(roles = {"ROLE_SELLER", "ROLE_VIP_SELLER"}, roleErrorMessage = "权限不足，仅商家可操作")
    @PostMapping("/seller/read")
    @RateLimit(window = 60, maxRequests = 60, message = "请求过于频繁，请稍后再试")
    public Result<?> sellerRead(
            User currentUser,
            @RequestParam @Min(1) Long conversationId) {
        Long sellerId = resolveSellerId(currentUser);
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
     * 根据登录用户名解析卖家ID（卖家账号的用户名 = 卖家名称）
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @return Long 卖家ID（null=无对应卖家）
     */
    private Long resolveSellerId(User currentUser) {
        if (currentUser == null) {
            return null;
        }
        Seller seller = sellerService.getSellerBySellerName(currentUser.getUsername());
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
