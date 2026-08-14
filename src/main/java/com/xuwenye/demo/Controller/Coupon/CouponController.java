package com.xuwenye.demo.Controller.Coupon;

import com.xuwenye.demo.Entity.Coupon;
import com.xuwenye.demo.Entity.Seller;
import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Service.CouponService;
import com.xuwenye.demo.Service.SellerService;
import com.xuwenye.demo.Service.UserService;
import com.xuwenye.demo.annotation.OperationLog;
import com.xuwenye.demo.annotation.RateLimit;
import com.xuwenye.demo.common.Result;
import com.xuwenye.demo.util.auth.JwtUtil;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Min;
import java.util.List;

/**
 * 优惠券接口
 * 1.管理端：创建、分页查询、向用户/向VIP买家批量发放
 * 2.用户端：可用优惠券查询、全部优惠券查询
 * 3.所有接口需登录
 * <p>
 * @author ZuiM
 */
@RestController
@RequestMapping("/api/coupon")
@Validated
public class CouponController {

    private final CouponService couponService;
    private final UserService userService;
    private final SellerService sellerService;
    private final JwtUtil jwtUtil;

    public CouponController(CouponService couponService,
                            UserService userService,
                            SellerService sellerService,
                            JwtUtil jwtUtil) {
        this.couponService = couponService;
        this.userService = userService;
        this.sellerService = sellerService;
        this.jwtUtil = jwtUtil;
    }

    // ======================== 管理员接口 ========================

    @OperationLog("创建优惠券")
    @PostMapping("/admin/create")
    @RateLimit(window = 60, maxRequests = 5, message = "操作过于频繁，请稍后再试")
    public Result<?> createCoupon(
            @RequestHeader("Authorization") String token,
            @RequestBody Coupon coupon) {
        if (!validateAdmin(token)) {
            return Result.error(403, "权限不足，仅管理员可操作");
        }
        try {
            couponService.createCoupon(coupon);
            return Result.ok("优惠券创建成功");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @GetMapping("/admin/list")
    @RateLimit(window = 60, maxRequests = 20, message = "请求过于频繁，请稍后再试")
    public Result<?> listCoupons(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (!validateAdmin(token)) {
            return Result.error(403, "权限不足，仅管理员可操作");
        }
        return Result.ok(couponService.getCouponPage(page, size));
    }

    @OperationLog("发放优惠券")
    @PostMapping("/admin/grant")
    @RateLimit(window = 60, maxRequests = 5, message = "操作过于频繁，请稍后再试")
    public Result<?> grantCoupon(
            @RequestHeader("Authorization") String token,
            @RequestParam @Min(1) Long userId,
            @RequestParam @Min(1) Long couponId,
            @RequestParam(defaultValue = "30") int expireDays) {
        if (!validateAdmin(token)) {
            return Result.error(403, "权限不足，仅管理员可操作");
        }
        try {
            couponService.grantToUser(userId, couponId, expireDays);
            return Result.ok("优惠券发放成功");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @OperationLog("批量发放优惠券")
    @PostMapping("/admin/grant-all-vip")
    @RateLimit(window = 60, maxRequests = 3, message = "操作过于频繁，请稍后再试")
    public Result<?> grantAllVip(
            @RequestHeader("Authorization") String token,
            @RequestParam @Min(1) Long couponId,
            @RequestParam(defaultValue = "30") int expireDays) {
        if (!validateAdmin(token)) {
            return Result.error(403, "权限不足，仅管理员可操作");
        }
        try {
            int count = couponService.grantToAllVipUsers(couponId, expireDays);
            return Result.ok("已向 " + count + " 名会员买家发放优惠券");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.error(400, e.getMessage());
        }
    }

    // ======================== 用户接口 ========================

    @GetMapping("/user/available")
    @RateLimit(window = 60, maxRequests = 30, message = "请求过于频繁，请稍后再试")
    public Result<?> availableCoupons(@RequestHeader("Authorization") String token) {
        Long userId = validateLogin(token);
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期");
        }
        return Result.ok(couponService.getAvailableCoupons(userId));
    }

    @GetMapping("/user/all")
    @RateLimit(window = 60, maxRequests = 30, message = "请求过于频繁，请稍后再试")
    public Result<?> allCoupons(@RequestHeader("Authorization") String token) {
        Long userId = validateLogin(token);
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期");
        }
        return Result.ok(couponService.getUserCoupons(userId));
    }

    // ======================== 内部工具 ========================

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
}