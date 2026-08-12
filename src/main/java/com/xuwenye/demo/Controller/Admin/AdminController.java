package com.xuwenye.demo.Controller.Admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuwenye.demo.Entity.Order;
import com.xuwenye.demo.Entity.Product;
import com.xuwenye.demo.Entity.Seller;
import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Service.OrderService;
import com.xuwenye.demo.Service.ProductService;
import com.xuwenye.demo.Service.SellerService;
import com.xuwenye.demo.Service.UserService;
import com.xuwenye.demo.annotation.RateLimit;
import com.xuwenye.demo.common.Result;
import com.xuwenye.demo.util.auth.JwtUtil;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理员后台管理接口
 * 1.用户管理：用户列表、启用/禁用、删除、恢复
 * 2.商品管理：所有商品列表（含已下架）
 * 3.订单管理：订单列表
 * 4.卖家管理：卖家列表
 * 所有接口需校验管理员权限
 * <p>
 * @author ZuiM
 */
@RestController
@RequestMapping("/api/admin")
@Validated
public class AdminController {

    private final UserService userService;
    private final ProductService productService;
    private final OrderService orderService;
    private final SellerService sellerService;
    private final JwtUtil jwtUtil;

    public AdminController(UserService userService,
                           ProductService productService,
                           OrderService orderService,
                           SellerService sellerService,
                           JwtUtil jwtUtil) {
        this.userService = userService;
        this.productService = productService;
        this.orderService = orderService;
        this.sellerService = sellerService;
        this.jwtUtil = jwtUtil;
    }

    // ======================== 用户管理 ========================

    /**
     * 用户列表（分页）
     * 1.校验管理员权限
     * 2.返回分页用户列表
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param page 页码
     * @param size 每页条数
     * @return Result 分页用户列表
     */
    @GetMapping("/users")
    @RateLimit(window = 60, maxRequests = 20, message = "请求过于频繁，请稍后再试")
    public Result<?> getUserList(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (!validateAdmin(token)) {
            return Result.error(403, "权限不足，仅管理员可操作");
        }

        // 使用 UserService 分页查询
        Page<User> userPage = userService.getUserList(page, size);

        // 脱敏：隐藏密码
        if (userPage.getRecords() != null) {
            userPage.getRecords().forEach(u -> u.setPassword(null));
        }

        return Result.ok(userPage);
    }

    /**
     * 启用/禁用用户
     * 1.校验管理员权限
     * 2.更新用户状态（0禁用 1启用）
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param id 用户ID
     * @param status 状态（0禁用 1启用）
     * @return Result 200 操作成功
     */
    @PutMapping("/user/status/{id}")
    @RateLimit(window = 60, maxRequests = 10, message = "操作过于频繁，请稍后再试")
    public Result<?> updateUserStatus(
            @RequestHeader("Authorization") String token,
            @PathVariable @Min(1) Long id,
            @RequestParam int status) {
        if (!validateAdmin(token)) {
            return Result.error(403, "权限不足，仅管理员可操作");
        }

        if (status != 0 && status != 1) {
            return Result.error(400, "状态值无效（0禁用 1启用）");
        }

        boolean success = userService.updateUserStatus(id, status);
        if (success) {
            return Result.ok(status == 1 ? "用户已启用" : "用户已禁用");
        }
        return Result.error(400, "操作失败，用户不存在");
    }

    /**
     * 删除用户（逻辑删除）
     * 1.校验管理员权限
     * 2.逻辑删除用户
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param id 用户ID
     * @return Result 200 删除成功
     */
    @DeleteMapping("/user/{id}")
    @RateLimit(window = 60, maxRequests = 10, message = "操作过于频繁，请稍后再试")
    public Result<?> deleteUser(
            @RequestHeader("Authorization") String token,
            @PathVariable @Min(1) Long id) {
        if (!validateAdmin(token)) {
            return Result.error(403, "权限不足，仅管理员可操作");
        }

        boolean success = userService.deleteUserById(id);
        if (success) {
            return Result.ok("用户删除成功");
        }
        return Result.error(400, "删除失败，用户不存在");
    }

    /**
     * 恢复用户（逻辑删除恢复）
     * 1.校验管理员权限
     * 2.恢复用户
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param id 用户ID
     * @return Result 200 恢复成功
     */
    @PutMapping("/user/recover/{id}")
    @RateLimit(window = 60, maxRequests = 10, message = "操作过于频繁，请稍后再试")
    public Result<?> recoverUser(
            @RequestHeader("Authorization") String token,
            @PathVariable @Min(1) Long id) {
        if (!validateAdmin(token)) {
            return Result.error(403, "权限不足，仅管理员可操作");
        }

        boolean success = userService.recoverUserById(id);
        if (success) {
            return Result.ok("用户恢复成功");
        }
        return Result.error(400, "恢复失败，用户不存在");
    }

    /**
     * 管理员为用户充值（模拟货币，增加账户余额）
     * 1.校验管理员权限
     * 2.增加用户余额（分布式锁 + 原子 SQL）
     * <p>
     * @author ZuiM
     * @param token 登录令牌（Bearer xxx）
     * @param userId 目标用户ID
     * @param amount 充值金额（必须大于0）
     * @return Result 200 充值成功；400 参数错误/失败；403 权限不足
     */
    @PostMapping("/user/charge")
    @RateLimit(window = 60, maxRequests = 10, message = "充值操作过于频繁，请稍后再试")
    public Result<?> chargeUser(
            @RequestHeader("Authorization") String token,
            @RequestParam @Min(1) Long userId,
            @RequestParam BigDecimal amount) {
        if (!validateAdmin(token)) {
            return Result.error(403, "权限不足，仅管理员可操作");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.error(400, "充值金额必须大于0");
        }
        boolean success = userService.chargeBalance(userId, amount);
        if (success) {
            return Result.ok("充值成功");
        }
        return Result.error(400, "充值失败，请重试");
    }

    // ======================== 商品管理 ========================

    /**
     * 所有商品列表（分页，含已下架）
     * 1.校验管理员权限
     * 2.返回所有商品列表
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param page 页码
     * @param size 每页条数
     * @return Result 分页商品列表
     */
    @GetMapping("/products")
    @RateLimit(window = 60, maxRequests = 20, message = "请求过于频繁，请稍后再试")
    public Result<?> getAllProducts(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (!validateAdmin(token)) {
            return Result.error(403, "权限不足，仅管理员可操作");
        }

        Page<Product> productPage = productService.getAllProductsPage(page, size);
        return Result.ok(productPage);
    }

    // ======================== 订单管理 ========================

    /**
     * 订单列表（分页）
     * 1.校验管理员权限
     * 2.返回所有订单列表
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param page 页码
     * @param size 每页条数
     * @param status 订单状态（可选）
     * @return Result 分页订单列表
     */
    @GetMapping("/orders")
    @RateLimit(window = 60, maxRequests = 20, message = "请求过于频繁，请稍后再试")
    public Result<?> getAllOrders(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        if (!validateAdmin(token)) {
            return Result.error(403, "权限不足，仅管理员可操作");
        }

        Page<Order> orderPage = orderService.adminGetOrders(page, size, status);
        return Result.ok(orderPage);
    }

    // ======================== 卖家管理 ========================

    /**
     * 卖家列表
     * 1.校验管理员权限
     * 2.返回所有卖家列表
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @return Result 卖家列表
     */
    @GetMapping("/sellers")
    @RateLimit(window = 60, maxRequests = 20, message = "请求过于频繁，请稍后再试")
    public Result<?> getSellerList(
            @RequestHeader("Authorization") String token) {
        if (!validateAdmin(token)) {
            return Result.error(403, "权限不足，仅管理员可操作");
        }

        List<Seller> sellers = sellerService.getAllSellers();
        return Result.ok(sellers);
    }

    // ======================== 内部工具方法 ========================

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
}