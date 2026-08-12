package com.xuwenye.demo.Controller.Order;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuwenye.demo.Entity.Order;
import com.xuwenye.demo.Entity.OrderItem;
import com.xuwenye.demo.Service.OrderService;
import com.xuwenye.demo.Service.OrderService.OrderItemRequest;
import com.xuwenye.demo.Service.UserService;
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
 * 订单接口
 * 1.用户接口：创建订单、订单列表、订单详情、支付、取消、退款（需登录）
 * 2.管理接口：管理员订单列表、发货、完成（需管理员权限）
 * <p>
 * @author ZuiM
 */
@RestController
@RequestMapping("/api/order")
@Validated
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public OrderController(OrderService orderService,
                           UserService userService,
                           JwtUtil jwtUtil) {
        this.orderService = orderService;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    // ======================== 用户接口（需登录） ========================

    /**
     * 创建订单
     * 1.校验登录状态
     * 2.创建订单（分布式锁扣库存）
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param request 创建订单请求体
     * @return Result 订单信息
     */
    @PostMapping("/create")
    @RateLimit(window = 60, maxRequests = 10, message = "订单创建过于频繁，请稍后再试")
    public Result<?> createOrder(
            @RequestHeader("Authorization") String token,
            @RequestBody CreateOrderRequest request) {
        // 校验登录
        Long userId = validateLogin(token);
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期");
        }

        try {
            Order order = orderService.createOrder(
                    userId,
                    request.getItems(),
                    request.getReceiverName(),
                    request.getReceiverPhone(),
                    request.getReceiverAddress(),
                    request.getRemark()
            );
            // 加载订单项
            List<OrderItem> items = orderService.getOrderItems(order.getId());
            Map<String, Object> result = new HashMap<>();
            result.put("order", order);
            result.put("items", items);
            return Result.ok(result);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 获取用户订单列表（分页）
     * 1.校验登录状态
     * 2.返回当前用户的订单列表
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param page 页码
     * @param size 每页条数
     * @return Result 分页订单列表
     */
    @GetMapping("/list")
    @RateLimit(window = 60, maxRequests = 20, message = "订单列表请求过于频繁，请稍后再试")
    public Result<?> getUserOrders(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        // 校验登录
        Long userId = validateLogin(token);
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期");
        }

        Page<Order> orderPage = orderService.getUserOrders(userId, page, size);
        return Result.ok(orderPage);
    }

    /**
     * 获取订单详情
     * 1.校验登录状态
     * 2.校验订单归属
     * 3.返回订单详情（含订单项）
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param id 订单ID
     * @return Result 订单详情
     */
    @GetMapping("/detail/{id}")
    @RateLimit(window = 60, maxRequests = 20, message = "订单详情请求过于频繁，请稍后再试")
    public Result<?> getOrderDetail(
            @RequestHeader("Authorization") String token,
            @PathVariable @Min(1) Long id) {
        // 校验登录
        Long userId = validateLogin(token);
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期");
        }

        Order order = orderService.getOrderDetail(id);
        if (order == null) {
            return Result.error(400, "订单不存在");
        }

        // 校验订单归属（管理员可查看所有订单）
        String username = jwtUtil.parseUsername(token.substring(7));
        boolean isAdmin = userService.isAdmin(username);
        if (!order.getUserId().equals(userId) && !isAdmin) {
            return Result.error(403, "无权查看其他用户的订单");
        }

        List<OrderItem> items = orderService.getOrderItems(id);
        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("items", items);
        return Result.ok(result);
    }

    /**
     * 支付订单（模拟）
     * 1.校验登录状态
     * 2.校验订单归属
     * 3.执行支付
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param id 订单ID
     * @return Result 200 支付成功
     */
    @PutMapping("/pay/{id}")
    @RateLimit(window = 60, maxRequests = 10, message = "支付操作过于频繁，请稍后再试")
    public Result<?> payOrder(
            @RequestHeader("Authorization") String token,
            @PathVariable @Min(1) Long id) {
        // 校验登录
        Long userId = validateLogin(token);
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期");
        }

        // 校验订单归属
        Order order = orderService.getOrderDetail(id);
        if (order == null) {
            return Result.error(400, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            return Result.error(403, "无权操作其他用户的订单");
        }

        try {
            boolean success = orderService.payOrder(id);
            if (success) {
                return Result.ok("支付成功");
            }
            return Result.error(400, "支付失败");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 取消订单
     * 1.校验登录状态
     * 2.校验订单归属
     * 3.取消订单（恢复库存）
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param id 订单ID
     * @return Result 200 取消成功
     */
    @PutMapping("/cancel/{id}")
    @RateLimit(window = 60, maxRequests = 10, message = "取消操作过于频繁，请稍后再试")
    public Result<?> cancelOrder(
            @RequestHeader("Authorization") String token,
            @PathVariable @Min(1) Long id) {
        // 校验登录
        Long userId = validateLogin(token);
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期");
        }

        // 校验订单归属
        Order order = orderService.getOrderDetail(id);
        if (order == null) {
            return Result.error(400, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            return Result.error(403, "无权操作其他用户的订单");
        }

        try {
            boolean success = orderService.cancelOrder(id);
            if (success) {
                return Result.ok("取消成功");
            }
            return Result.error(400, "取消失败");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 申请退款
     * 1.校验登录状态
     * 2.校验订单归属
     * 3.执行退款（恢复库存）
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param id 订单ID
     * @return Result 200 退款成功
     */
    @PutMapping("/refund/{id}")
    @RateLimit(window = 60, maxRequests = 10, message = "退款操作过于频繁，请稍后再试")
    public Result<?> requestRefund(
            @RequestHeader("Authorization") String token,
            @PathVariable @Min(1) Long id) {
        // 校验登录
        Long userId = validateLogin(token);
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期");
        }

        // 校验订单归属
        Order order = orderService.getOrderDetail(id);
        if (order == null) {
            return Result.error(400, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            return Result.error(403, "无权操作其他用户的订单");
        }

        try {
            boolean success = orderService.requestRefund(id);
            if (success) {
                return Result.ok("退款成功");
            }
            return Result.error(400, "退款失败");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    // ======================== 管理员接口 ========================

    /**
     * 管理员获取订单列表（分页，可按状态筛选）
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param page 页码
     * @param size 每页条数
     * @param status 订单状态（可选）
     * @return Result 分页订单列表
     */
    @GetMapping("/admin/list")
    @RateLimit(window = 60, maxRequests = 20, message = "订单列表请求过于频繁，请稍后再试")
    public Result<?> adminGetOrders(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        // 校验管理员权限
        if (!validateAdmin(token)) {
            return Result.error(403, "权限不足，仅管理员可操作");
        }

        Page<Order> orderPage = orderService.adminGetOrders(page, size, status);
        return Result.ok(orderPage);
    }

    /**
     * 管理员发货
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param id 订单ID
     * @return Result 200 发货成功
     */
    @PutMapping("/admin/ship/{id}")
    @RateLimit(window = 60, maxRequests = 10, message = "操作过于频繁，请稍后再试")
    public Result<?> shipOrder(
            @RequestHeader("Authorization") String token,
            @PathVariable @Min(1) Long id) {
        // 校验管理员权限
        if (!validateAdmin(token)) {
            return Result.error(403, "权限不足，仅管理员可操作");
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

    /**
     * 管理员完成订单
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param id 订单ID
     * @return Result 200 完成成功
     */
    @PutMapping("/admin/complete/{id}")
    @RateLimit(window = 60, maxRequests = 10, message = "操作过于频繁，请稍后再试")
    public Result<?> completeOrder(
            @RequestHeader("Authorization") String token,
            @PathVariable @Min(1) Long id) {
        // 校验管理员权限
        if (!validateAdmin(token)) {
            return Result.error(403, "权限不足，仅管理员可操作");
        }

        try {
            boolean success = orderService.completeOrder(id);
            if (success) {
                return Result.ok("订单完成成功");
            }
            return Result.error(400, "订单完成失败");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    // ======================== 内部工具方法 ========================

    /**
     * 校验登录状态，返回用户ID
     * <p>
     * @author ZuiM
     * @param token 登录令牌（Bearer xxx）
     * @return Long 用户ID（null表示未登录）
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
        com.xuwenye.demo.Entity.User user = userService.findUserableUser(username);
        if (user == null) {
            return null;
        }
        return user.getId();
    }

    /**
     * 校验管理员权限
     * <p>
     * @author ZuiM
     * @param token 登录令牌（Bearer xxx）
     * @return boolean true=管理员
     */
    private boolean validateAdmin(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            return false;
        }
        String realToken = token.substring(7);
        if (!jwtUtil.validate(realToken)) {
            return false;
        }
        String username = jwtUtil.parseUsername(realToken);
        return userService.isAdmin(username);
    }

    // ======================== 请求体类 ========================

    /**
     * 创建订单请求体
     * <p>
     * @author ZuiM
     */
    public static class CreateOrderRequest {
        private List<OrderItemRequest> items;
        private String receiverName;
        private String receiverPhone;
        private String receiverAddress;
        private String remark;

        public List<OrderItemRequest> getItems() { return items; }
        public void setItems(List<OrderItemRequest> items) { this.items = items; }
        public String getReceiverName() { return receiverName; }
        public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
        public String getReceiverPhone() { return receiverPhone; }
        public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }
        public String getReceiverAddress() { return receiverAddress; }
        public void setReceiverAddress(String receiverAddress) { this.receiverAddress = receiverAddress; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }
}