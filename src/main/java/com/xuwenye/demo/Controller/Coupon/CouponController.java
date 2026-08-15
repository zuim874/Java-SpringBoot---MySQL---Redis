package com.xuwenye.demo.Controller.Coupon;

import com.xuwenye.demo.Entity.Coupon;
import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Service.CouponService;
import com.xuwenye.demo.annotation.OperationLog;
import com.xuwenye.demo.annotation.RateLimit;
import com.xuwenye.demo.annotation.UserCheck;
import com.xuwenye.demo.common.Result;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Min;

/**
 * 优惠券接口
 * 1.管理端：创建、分页查询、向用户/向VIP会员批量发放（@UserCheck 切面校验 ROLE_ADMIN 角色）
 * 2.用户端：可用优惠券查询、全部优惠券查询（@UserCheck 切面校验登录态）
 * 3.领券中心：可领模板列表（公开，无需登录）、用户自助领取（@UserCheck 切面校验登录态）
 * <p>
 * 适用人群：targetType=1 普通券（全部用户可领）；targetType=2 VIP券（受众=VIP用户 + VIP卖家）
 * <p>
 * @author ZuiM
 */
@RestController
@RequestMapping("/api/coupon")
@Validated
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    // ======================== 管理员接口 ========================

    @OperationLog("创建优惠券")
    @UserCheck(roles = {"ROLE_ADMIN"}, roleErrorMessage = "权限不足，仅管理员可操作")
    @PostMapping("/admin/create")
    @RateLimit(window = 60, maxRequests = 5, message = "操作过于频繁，请稍后再试")
    public Result<?> createCoupon(
            User currentUser,
            @RequestBody Coupon coupon) {
        try {
            couponService.createCoupon(coupon);
            return Result.ok("优惠券创建成功");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @UserCheck(roles = {"ROLE_ADMIN"}, roleErrorMessage = "权限不足，仅管理员可操作")
    @GetMapping("/admin/list")
    @RateLimit(window = 60, maxRequests = 20, message = "请求过于频繁，请稍后再试")
    public Result<?> listCoupons(
            User currentUser,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(couponService.getCouponPage(page, size));
    }

    @OperationLog("发放优惠券")
    @UserCheck(roles = {"ROLE_ADMIN"}, roleErrorMessage = "权限不足，仅管理员可操作")
    @PostMapping("/admin/grant")
    @RateLimit(window = 60, maxRequests = 5, message = "操作过于频繁，请稍后再试")
    public Result<?> grantCoupon(
            User currentUser,
            @RequestParam @Min(1) Long userId,
            @RequestParam @Min(1) Long couponId,
            @RequestParam(defaultValue = "30") int expireDays) {
        try {
            couponService.grantToUser(userId, couponId, expireDays);
            return Result.ok("优惠券发放成功");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @OperationLog("批量发放优惠券")
    @UserCheck(roles = {"ROLE_ADMIN"}, roleErrorMessage = "权限不足，仅管理员可操作")
    @PostMapping("/admin/grant-all-vip")
    @RateLimit(window = 60, maxRequests = 3, message = "操作过于频繁，请稍后再试")
    public Result<?> grantAllVip(
            User currentUser,
            @RequestParam @Min(1) Long couponId,
            @RequestParam(defaultValue = "30") int expireDays) {
        try {
            // 异步提交：实际发放由消息队列在后台削峰执行，避免长时间占用请求线程与数据库连接
            couponService.grantToAllVipUsersAsync(couponId, expireDays);
            return Result.ok("批量发放任务已提交，正在后台向会员买家发放");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @OperationLog("向全部用户发放优惠券")
    @UserCheck(roles = {"ROLE_ADMIN"}, roleErrorMessage = "权限不足，仅管理员可操作")
    @PostMapping("/admin/grant-all-users")
    @RateLimit(window = 60, maxRequests = 3, message = "操作过于频繁，请稍后再试")
    public Result<?> grantAllUsers(
            User currentUser,
            @RequestParam @Min(1) Long couponId,
            @RequestParam(defaultValue = "30") int expireDays) {
        try {
            // 异步提交：实际发放由消息队列在后台削峰执行，避免长时间占用请求线程与数据库连接
            couponService.grantToAllUsersAsync(couponId, expireDays);
            return Result.ok("批量发放任务已提交，正在后台向注册用户发放");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.error(400, e.getMessage());
        }
    }

    // ======================== 领券中心（用户自助领取） ========================

    /**
     * 查询可自助领取的优惠券模板列表（公开，无需登录）
     * 供首页「领券中心」展示
     * <p>
     * @author ZuiM
     * @return Result<List<Coupon>> 可领取的模板列表
     */
    @GetMapping("/user/templates")
    @RateLimit(window = 60, maxRequests = 30, message = "请求过于频繁，请稍后再试")
    public Result<?> claimableTemplates() {
        return Result.ok(couponService.getClaimableTemplates());
    }

    /**
     * 用户自助领取优惠券
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（@UserCheck 切面注入）
     * @param couponId 优惠券模板ID
     * @return Result<?> 领取结果
     */
    @OperationLog("领取优惠券")
    @UserCheck
    @PostMapping("/user/claim")
    @RateLimit(window = 60, maxRequests = 10, message = "操作过于频繁，请稍后再试")
    public Result<?> claimCoupon(
            User currentUser,
            @RequestParam @Min(1) Long couponId) {
        try {
            couponService.claimCoupon(currentUser.getId(), couponId);
            return Result.ok("优惠券领取成功");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.error(400, e.getMessage());
        }
    }

    // ======================== 用户接口 ========================

    @UserCheck
    @GetMapping("/user/available")
    @RateLimit(window = 60, maxRequests = 30, message = "请求过于频繁，请稍后再试")
    public Result<?> availableCoupons(User currentUser) {
        return Result.ok(couponService.getAvailableCoupons(currentUser.getId()));
    }

    @UserCheck
    @GetMapping("/user/all")
    @RateLimit(window = 60, maxRequests = 30, message = "请求过于频繁，请稍后再试")
    public Result<?> allCoupons(User currentUser) {
        return Result.ok(couponService.getUserCoupons(currentUser.getId()));
    }
}
