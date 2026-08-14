package com.xuwenye.demo.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuwenye.demo.Entity.Coupon;
import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Entity.UserCoupon;
import com.xuwenye.demo.Mapper.CouponMapper;
import com.xuwenye.demo.Mapper.UserCouponMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 优惠券业务层
 * 1.模板管理：管理员创建/分页查询优惠券模板
 * 2.发放：向单个用户或全部VIP用户发放（原子扣减剩余数量，防止超发）
 * 3.使用：校验归属/状态/过期时间/门槛后原子核销，防止并发重复使用
 * 4.计算：根据类型（满减/折扣）计算优惠金额
 * <p>
 * @author ZuiM
 */
@Service
@Slf4j
public class CouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final UserService userService;

    public CouponService(CouponMapper couponMapper,
                         UserCouponMapper userCouponMapper,
                         UserService userService) {
        this.couponMapper = couponMapper;
        this.userCouponMapper = userCouponMapper;
        this.userService = userService;
    }

    // ======================== 模板管理（管理员） ========================

    /**
     * 创建优惠券模板
     * <p>
     * @author ZuiM
     * @param coupon 模板实体（name/type/discountValue/minAmount/totalCount）
     * @return boolean true=创建成功
     */
    public boolean createCoupon(Coupon coupon) {
        if (coupon.getName() == null || coupon.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("优惠券名称不能为空");
        }
        if (coupon.getType() == null || (coupon.getType() != 1 && coupon.getType() != 2)) {
            throw new IllegalArgumentException("优惠类型必须为 1满减 或 2折扣");
        }
        if (coupon.getDiscountValue() == null
                || coupon.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("优惠值必须大于 0");
        }
        if (coupon.getType() == 2
                && (coupon.getDiscountValue().compareTo(new BigDecimal("10")) >= 0
                    || coupon.getDiscountValue().compareTo(new BigDecimal("1")) < 0)) {
            throw new IllegalArgumentException("折扣必须为 1~10 之间的折数（如 8.5 表示 8.5 折）");
        }
        if (coupon.getMinAmount() == null) {
            coupon.setMinAmount(BigDecimal.ZERO);
        }
        if (coupon.getTotalCount() == null || coupon.getTotalCount() <= 0) {
            throw new IllegalArgumentException("发行总量必须大于 0");
        }
        coupon.setRemainCount(coupon.getTotalCount());
        coupon.setStatus(1);
        return couponMapper.insert(coupon) > 0;
    }

    /**
     * 分页查询优惠券模板（管理员）
     * <p>
     * @author ZuiM
     * @param page 页码
     * @param size 每页条数
     * @return Page<Coupon> 分页模板
     */
    public Page<Coupon> getCouponPage(int page, int size) {
        Page<Coupon> pageObj = new Page<>(page, size);
        QueryWrapper<Coupon> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time");
        return couponMapper.selectPage(pageObj, wrapper);
    }

    // ======================== 发放（管理员） ========================

    /**
     * 向单个用户发放优惠券
     * 1.校验模板存在且启用、剩余数量充足
     * 2.原子扣减剩余数量（防止超发）
     * 3.写入用户优惠券（含快照信息）
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     * @param couponId 优惠券模板ID
     * @param expireDays 有效天数
     */
    @Transactional
    public void grantToUser(Long userId, Long couponId, int expireDays) {
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || coupon.getStatus() != 1) {
            throw new IllegalArgumentException("优惠券不存在或已停用");
        }
        // 原子扣减剩余数量
        if (couponMapper.decrementRemain(couponId) <= 0) {
            throw new IllegalStateException("优惠券库存不足，发放失败");
        }
        UserCoupon uc = buildUserCoupon(userId, coupon, expireDays);
        userCouponMapper.insert(uc);
        log.info("向用户 {} 发放优惠券 {} 成功", userId, coupon.getName());
    }

    /**
     * 向全部会员买家（ROLE_VIP_USER）发放优惠券
     * 逐个发放，某用户失败不影响其余用户
     * <p>
     * @author ZuiM
     * @param couponId 优惠券模板ID
     * @param expireDays 有效天数
     * @return int 成功发放数量
     */
    @Transactional
    public int grantToAllVipUsers(Long couponId, int expireDays) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || coupon.getStatus() != 1) {
            throw new IllegalArgumentException("优惠券不存在或已停用");
        }
        // 查询全部启用且包含 VIP 买家角色的用户
        List<User> vipUsers = listVipUsers();
        if (vipUsers.isEmpty()) {
            log.info("没有符合条件的会员买家，跳过批量发放");
            return 0;
        }
        int granted = 0;
        for (User u : vipUsers) {
            if (couponMapper.decrementRemain(couponId) <= 0) {
                break; // 库存不足，停止发放
            }
            userCouponMapper.insert(buildUserCoupon(u.getId(), coupon, expireDays));
            granted++;
        }
        log.info("向会员买家批量发放优惠券 {} 完成，共 {} 张", coupon.getName(), granted);
        return granted;
    }

    /**
     * 构建用户优惠券快照
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     * @param coupon 模板
     * @param expireDays 有效天数
     * @return UserCoupon 用户优惠券
     */
    private UserCoupon buildUserCoupon(Long userId, Coupon coupon, int expireDays) {
        UserCoupon uc = new UserCoupon();
        uc.setUserId(userId);
        uc.setCouponId(coupon.getId());
        uc.setName(coupon.getName());
        uc.setType(coupon.getType());
        uc.setDiscountValue(coupon.getDiscountValue());
        uc.setMinAmount(coupon.getMinAmount());
        uc.setStatus(0);
        uc.setReceiveTime(LocalDateTime.now());
        int days = Math.max(expireDays, 1);
        uc.setExpireTime(LocalDateTime.now().plusDays(days));
        return uc;
    }

    /**
     * 查询所有启用且为会员买家的用户
     * <p>
     * @author ZuiM
     * @return List<User> 会员买家列表
     */
    private List<User> listVipUsers() {
        List<User> result = new ArrayList<>();
        Page<User> page = new Page<>(1, 100);
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        Page<User> p = userService.getUserListByQuery(wrapper, page);
        for (User u : p.getRecords()) {
            if (hasRole(u.getUserRole(), "ROLE_VIP_USER")) {
                result.add(u);
            }
        }
        return result;
    }

    // ======================== 查询（用户） ========================

    /**
     * 查询用户的可用优惠券（未使用且未过期）
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     * @return List<UserCoupon> 可用优惠券
     */
    public List<UserCoupon> getAvailableCoupons(Long userId) {
        return userCouponMapper.findAvailableByUserId(userId);
    }

    /**
     * 查询用户的全部优惠券（含已用/已过期）
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     * @return List<UserCoupon> 全部优惠券
     */
    public List<UserCoupon> getUserCoupons(Long userId) {
        return userCouponMapper.findAllByUserId(userId);
    }

    // ======================== 使用（结算时调用） ========================

    /**
     * 校验优惠券并计算优惠金额（不核销，仅计算）
     * 供下单前展示与最终下单双重校验
     * <p>
     * @author ZuiM
     * @param userCouponId 用户优惠券ID
     * @param userId 用户ID
     * @param orderAmount 订单原金额
     * @return BigDecimal 优惠金额
     */
    public BigDecimal validateAndCalcDiscount(Long userCouponId, Long userId, BigDecimal orderAmount) {
        UserCoupon uc = userCouponMapper.selectById(userCouponId);
        if (uc == null || !uc.getUserId().equals(userId)) {
            throw new IllegalArgumentException("优惠券不存在或不属于当前用户");
        }
        if (uc.getStatus() != 0) {
            throw new IllegalArgumentException("优惠券已使用或已过期");
        }
        if (uc.getExpireTime() != null && uc.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("优惠券已过期");
        }
        if (uc.getMinAmount() != null && orderAmount.compareTo(uc.getMinAmount()) < 0) {
            throw new IllegalArgumentException("订单金额未达到优惠券使用门槛（满 "
                    + uc.getMinAmount().stripTrailingZeros().toPlainString() + " 元可用）");
        }
        return calcDiscount(uc, orderAmount);
    }

    /**
     * 核销用户优惠券（原子操作，防止并发重复使用）
     * <p>
     * @author ZuiM
     * @param userCouponId 用户优惠券ID
     * @param userId 用户ID
     * @param orderId 使用订单ID
     */
    public void markUsed(Long userCouponId, Long userId, Long orderId) {
        if (userCouponMapper.markUsed(userCouponId, userId, orderId) <= 0) {
            throw new IllegalStateException("优惠券使用失败，可能已被使用或已过期");
        }
    }

    /**
     * 根据优惠券类型计算优惠金额
     * 1.满减：直接减免 discountValue（不超过订单金额）
     * 2.折扣：订单金额 * (1 - discountValue/10)，保留两位小数
     * <p>
     * @author ZuiM
     * @param uc 用户优惠券
     * @param orderAmount 订单原金额
     * @return BigDecimal 优惠金额
     */
    private BigDecimal calcDiscount(UserCoupon uc, BigDecimal orderAmount) {
        BigDecimal discount;
        if (uc.getType() == 2) {
            // 折扣：如 8.5 折 → 优惠 = 金额 * (1 - 8.5/10)
            BigDecimal factor = BigDecimal.ONE.subtract(
                    uc.getDiscountValue().divide(BigDecimal.TEN, 4, RoundingMode.HALF_UP));
            discount = orderAmount.multiply(factor).setScale(2, RoundingMode.HALF_UP);
        } else {
            // 满减：减免金额不超过订单金额
            discount = uc.getDiscountValue().min(orderAmount);
        }
        return discount;
    }

    /**
     * 判断用户角色字符串是否包含指定角色编码
     * <p>
     * @author ZuiM
     * @param userRole 用户角色（逗号分隔，可为空）
     * @param role 角色编码（如 ROLE_VIP_USER）
     * @return boolean 是否包含
     */
    public static boolean hasRole(String userRole, String role) {
        if (userRole == null || role == null) {
            return false;
        }
        for (String r : userRole.split(",")) {
            if (role.equals(r.trim())) {
                return true;
            }
        }
        return false;
    }
}
