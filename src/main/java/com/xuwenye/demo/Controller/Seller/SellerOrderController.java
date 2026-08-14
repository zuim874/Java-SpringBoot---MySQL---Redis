package com.xuwenye.demo.Controller.Seller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuwenye.demo.Entity.Order;
import com.xuwenye.demo.Entity.OrderItem;
import com.xuwenye.demo.Entity.Seller;
import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Service.OrderService;
import com.xuwenye.demo.Service.SellerService;
import com.xuwenye.demo.Service.UserService;
import com.xuwenye.demo.annotation.OperationLog;
import com.xuwenye.demo.annotation.RateLimit;
import com.xuwenye.demo.common.Result;
import com.xuwenye.demo.util.auth.JwtUtil;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 卖家订单管理接口
 * 1.卖家可查看自己店铺的订单（含商品归属校验）
 * 2.卖家可修改订单状态（发货、完成）
 * 3.卖家可查看店铺信息与更新
 * 4.校验卖家身份 + 商品归属双重保护
 * <p>
 * @author ZuiM
 */
@RestController
@RequestMapping("/api/seller")
@Validated
public class SellerOrderController {

    private final OrderService orderService;
    private final UserService userService;
    private final SellerService sellerService;
    private final JwtUtil jwtUtil;

    public SellerOrderController(OrderService orderService,
                                 UserService userService,
                                 SellerService sellerService,
                                 JwtUtil jwtUtil) {
        this.orderService = orderService;
        this.userService = userService;
        this.sellerService = sellerService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 卖家店铺信息
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @return Result 店铺信息
     */
    @GetMapping("/shop")
    @RateLimit(window = 60, maxRequests = 20, message = "请求过于频繁，请稍后再试")
    public Result<?> getShopInfo(@RequestHeader("Authorization") String token) {
        Long sellerId = validateSeller(token);
        if (sellerId == null) {
            return Result.error(403, "权限不足，仅商家可操作");
        }
        Seller seller = sellerService.getSellerById(sellerId);
        if (seller == null) {
            return Result.error(400, "店铺不存在");
        }
        // 判断是否为VIP卖家
        String username = jwtUtil.parseUsername(token.substring(7));
        User user = userService.findAllUser(username);
        boolean isVip = user != null && user.getUserRole() != null && user.getUserRole().contains("VIP_SELLER");
        Map<String, Object> result = new HashMap<>();
        result.put("seller", seller);
        result.put("isVip", isVip);
        return Result.ok(result);
    }

    /**
     * 卖家更新店铺信息
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param seller 更新信息
     * @return Result 200 更新成功
     */
    @OperationLog("卖家更新店铺")
    @PutMapping("/shop/update")
    @RateLimit(window = 60, maxRequests = 5, message = "操作过于频繁，请稍后再试")
    public Result<?> updateShop(
            @RequestHeader("Authorization") String token,
            @RequestBody Seller seller) {
        Long sellerId = validateSeller(token);
        if (sellerId == null) {
            return Result.error(403, "权限不足，仅商家可操作");
        }
        seller.setId(sellerId);
        boolean success = sellerService.updateSeller(seller);
        if (success) {
            return Result.ok("店铺信息更新成功");
        }
        return Result.error(400, "更新失败");
    }

    /**
     * 卖家订单列表（分页，含商品归属校验）
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param page 页码
     * @param size 每页条数
     * @param status 订单状态筛选（可选）
     * @return Result 分页订单列表
     */
    @GetMapping("/orders")
    @RateLimit(window = 60, maxRequests = 20, message = "请求过于频繁，请稍后再试")
    public Result<?> getSellerOrders(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        Long sellerId = validateSeller(token);
        if (sellerId == null) {
            return Result.error(403, "权限不足，仅商家可操作");
        }
        Page<Order> orders = orderService.getSellerOrders(sellerId, page, size, status);
        return Result.ok(orders);
    }

    /**
     * 卖家订单详情
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param id 订单ID
     * @return Result 订单详情（含订单项）
     */
    @GetMapping("/order/{id}")
    @RateLimit(window = 60, maxRequests = 20, message = "请求过于频繁，请稍后再试")
    public Result<?> getOrderDetail(
            @RequestHeader("Authorization") String token,
            @PathVariable @Min(1) Long id) {
        Long sellerId = validateSeller(token);
        if (sellerId == null) {
            return Result.error(403, "权限不足，仅商家可操作");
        }
        // 校验订单包含该卖家的商品
        if (!orderService.orderContainsSeller(id, sellerId)) {
            return Result.error(403, "无权查看其他商家的订单");
        }
        Order order = orderService.getOrderDetail(id);
        if (order == null) {
            return Result.error(400, "订单不存在");
        }
        List<OrderItem> items = orderService.getOrderItems(id);
        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("items", items);
        return Result.ok(result);
    }

    /**
     * 卖家发货
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param id 订单ID
     * @return Result 200 发货成功
     */
    @OperationLog("卖家发货")
    @PutMapping("/order/ship/{id}")
    @RateLimit(window = 60, maxRequests = 5, message = "操作过于频繁，请稍后再试")
    public Result<?> shipOrder(
            @RequestHeader("Authorization") String token,
            @PathVariable @Min(1) Long id) {
        Long sellerId = validateSeller(token);
        if (sellerId == null) {
            return Result.error(403, "权限不足，仅商家可操作");
        }
        if (!orderService.orderContainsSeller(id, sellerId)) {
            return Result.error(403, "无权操作其他商家的订单");
        }
        try {
            boolean success = orderService.shipOrder(id);
            if (success) {
                return Result.ok("发货成功");
            }
            return Result.error(400, "发货失败");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    // ======================== 内部工具 ========================

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
}