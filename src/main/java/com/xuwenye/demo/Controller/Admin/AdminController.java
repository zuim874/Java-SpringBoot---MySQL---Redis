package com.xuwenye.demo.Controller.Admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuwenye.demo.Entity.Order;
import com.xuwenye.demo.Entity.Product;
import com.xuwenye.demo.Entity.RechargeRequest;
import com.xuwenye.demo.Entity.Seller;
import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Service.OrderService;
import com.xuwenye.demo.Service.ProductService;
import com.xuwenye.demo.Service.RechargeRequestService;
import com.xuwenye.demo.Service.SellerService;
import com.xuwenye.demo.Service.UserService;
import com.xuwenye.demo.annotation.OperationLog;
import com.xuwenye.demo.annotation.RateLimit;
import com.xuwenye.demo.common.Result;
import com.xuwenye.demo.util.auth.JwtUtil;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

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
    private final RechargeRequestService rechargeRequestService;

    public AdminController(UserService userService,
                           ProductService productService,
                           OrderService orderService,
                           SellerService sellerService,
                           JwtUtil jwtUtil,
                           RechargeRequestService rechargeRequestService) {
        this.userService = userService;
        this.productService = productService;
        this.orderService = orderService;
        this.sellerService = sellerService;
        this.jwtUtil = jwtUtil;
        this.rechargeRequestService = rechargeRequestService;
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
    @OperationLog("启用/禁用用户")
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
    @OperationLog("管理员删除用户")
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
    @OperationLog("管理员恢复用户")
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
    @OperationLog("管理员用户充值")
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
     * 卖家列表（分页）
     * 1.校验管理员权限
     * 2.返回分页卖家列表
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param page 页码
     * @param size 每页条数
     * @return Result 分页卖家列表
     */
    @GetMapping("/sellers")
    @RateLimit(window = 60, maxRequests = 20, message = "请求过于频繁，请稍后再试")
    public Result<?> getSellerList(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (!validateAdmin(token)) {
            return Result.error(403, "权限不足，仅管理员可操作");
        }

        Page<Seller> sellerPage = sellerService.getSellerPage(page, size);
        return Result.ok(sellerPage);
    }

    // ======================== 充值审核管理 ========================

    /**
     * 充值申请列表（分页）
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param page 页码
     * @param size 每页条数
     * @return Result 分页充值申请列表
     */
    @GetMapping("/recharge-requests")
    @RateLimit(window = 60, maxRequests = 20, message = "请求过于频繁，请稍后再试")
    public Result<?> getRechargeRequests(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (!validateAdmin(token)) {
            return Result.error(403, "权限不足，仅管理员可操作");
        }
        return Result.ok(rechargeRequestService.getRequestPage(page, size));
    }

    /**
     * 审核通过充值申请
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param id 申请ID
     * @return Result<?>
     */
    @OperationLog("审核通过充值申请")
    @PostMapping("/recharge-requests/{id}/approve")
    @RateLimit(window = 60, maxRequests = 10, message = "操作过于频繁，请稍后再试")
    public Result<?> approveRecharge(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        if (!validateAdmin(token)) {
            return Result.error(403, "权限不足，仅管理员可操作");
        }
        try {
            rechargeRequestService.approveRequest(id);
            return Result.ok("已通过该充值申请，余额已更新");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "审核失败：" + e.getMessage());
        }
    }

    /**
     * 拒绝充值申请
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param id 申请ID
     * @param reason 拒绝原因（可选）
     * @return Result<?>
     */
    @OperationLog("拒绝充值申请")
    @PostMapping("/recharge-requests/{id}/reject")
    @RateLimit(window = 60, maxRequests = 10, message = "操作过于频繁，请稍后再试")
    public Result<?> rejectRecharge(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        if (!validateAdmin(token)) {
            return Result.error(403, "权限不足，仅管理员可操作");
        }
        try {
            rechargeRequestService.rejectRequest(id, reason);
            return Result.ok("已拒绝该充值申请");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "操作失败：" + e.getMessage());
        }
    }

    // ======================== 会员（VIP）管理 ========================

    /**
     * 升级/降级买家为会员买家（ROLE_VIP_USER）
     * 会员买家可享受更多/更强的优惠券福利
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param id 用户ID
     * @param enable true=升级为VIP false=取消VIP
     * @return Result 200 操作成功
     */
    @OperationLog("买家会员升级/降级")
    @PutMapping("/user/vip/{id}")
    @RateLimit(window = 60, maxRequests = 10, message = "操作过于频繁，请稍后再试")
    public Result<?> setUserVip(
            @RequestHeader("Authorization") String token,
            @PathVariable @Min(1) Long id,
            @RequestParam boolean enable) {
        if (!validateAdmin(token)) {
            return Result.error(403, "权限不足，仅管理员可操作");
        }
        boolean success = userService.updateUserRole(id, "ROLE_VIP_USER", enable);
        if (success) {
            return Result.ok(enable ? "已升级为会员买家" : "已取消会员买家");
        }
        return Result.error(400, "操作失败，用户不存在");
    }

    /**
     * 升级/降级卖家为会员卖家（ROLE_VIP_SELLER）
     * 会员卖家福利：旗舰店标识、商品推荐位（商城置顶曝光）
     * 通过卖家名称匹配关联的用户账号进行角色变更
     * <p>
     * @author ZuiM
     * @param token 登录令牌
     * @param id 卖家ID（关联 sys_seller.id）
     * @param enable true=升级为VIP卖家 false=取消VIP卖家
     * @return Result 200 操作成功
     */
    @OperationLog("卖家会员升级/降级")
    @PutMapping("/seller/vip/{id}")
    @RateLimit(window = 60, maxRequests = 10, message = "操作过于频繁，请稍后再试")
    public Result<?> setSellerVip(
            @RequestHeader("Authorization") String token,
            @PathVariable @Min(1) Long id,
            @RequestParam boolean enable) {
        if (!validateAdmin(token)) {
            return Result.error(403, "权限不足，仅管理员可操作");
        }
        Seller seller = sellerService.getSellerById(id);
        if (seller == null) {
            return Result.error(400, "卖家不存在");
        }
        // 卖家与用户通过名称关联（卖家账号的用户名 = 卖家名称）
        com.xuwenye.demo.Entity.User user = userService.findAllUser(seller.getSellerName());
        if (user == null) {
            return Result.error(400, "未找到该卖家关联的用户账号");
        }
        boolean success = userService.updateUserRole(user.getId(), "ROLE_VIP_SELLER", enable);
        if (success) {
            return Result.ok(enable ? "已升级为会员卖家" : "已取消会员卖家");
        }
        return Result.error(400, "操作失败");
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