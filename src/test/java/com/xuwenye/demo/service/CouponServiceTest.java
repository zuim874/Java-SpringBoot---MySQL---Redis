package com.xuwenye.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xuwenye.demo.Entity.Coupon;
import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Entity.UserCoupon;
import com.xuwenye.demo.Mapper.CouponMapper;
import com.xuwenye.demo.Mapper.UserCouponMapper;
import com.xuwenye.demo.Service.CouponService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 优惠券业务层集成测试（需 MySQL + Redis + ActiveMQ）
 * 1.模板管理：创建校验（名称/类型/优惠值/总量）
 * 2.发放：向单个用户发放、校验归属
 * 3.优惠计算：满减 / 折扣
 * 4.核销：使用后不可再次使用
 * <p>
 * @author ZuiM
 */
class CouponServiceTest extends AbstractServiceTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private UserCouponMapper userCouponMapper;

    // ========== 测试数据构造 ==========

    /**
     * 构造测试用户
     * <p>
     * @author ZuiM
     * @return User 用户
     */
    private User buildUser() {
        User user = new User();
        user.setUsername(uniqueName("ut_coupon_user"));
        user.setPassword("$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa");
        user.setNickname("券测试用户" + SUFFIX);
        user.setEmail(uniqueName("utc") + "@test.com");
        user.setStatus(1);
        user.setUserRole("ROLE_USER");
        user.setBalance(BigDecimal.ZERO);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    /**
     * 构造满减优惠券模板
     * <p>
     * @author ZuiM
     * @return Coupon 模板
     */
    private Coupon buildCoupon(int type, String discountValue, String minAmount) {
        Coupon coupon = new Coupon();
        coupon.setName("测试优惠券" + SUFFIX + "_" + type);
        coupon.setType(type);
        coupon.setDiscountValue(new BigDecimal(discountValue));
        coupon.setMinAmount(new BigDecimal(minAmount));
        coupon.setTotalCount(100);
        coupon.setCreateTime(LocalDateTime.now());
        return coupon;
    }

    /**
     * 清理测试数据（优惠券/用户券/用户）
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     * @param couponId 模板ID
     */
    private void cleanData(Long userId, Long couponId) {
        if (userId != null) {
            userCouponMapper.delete(new QueryWrapper<UserCoupon>().eq("user_id", userId));
            userMapper.deleteById(userId);
            trackCacheKey("demo:coupon:user:" + userId + ":available");
            trackCacheKey("demo:coupon:user:" + userId + ":all");
        }
        if (couponId != null) {
            couponMapper.deleteById(couponId);
        }
        trackCacheKey("demo:coupon:page:*");
    }

    // ========== 1. 模板创建 ==========

    /**
     * 创建满减优惠券成功
     * <p>
     * @author ZuiM
     */
    @Test
    void 创建满减优惠券() {
        Coupon coupon = buildCoupon(1, "20.00", "100.00");
        assertTrue(couponService.createCoupon(coupon));
        assertNotNull(coupon.getId());
        assertEquals(1, coupon.getStatus());
        assertEquals(coupon.getTotalCount(), coupon.getRemainCount());
        cleanData(null, coupon.getId());
    }

    /**
     * 非法参数创建被拒绝
     * <p>
     * @author ZuiM
     */
    @Test
    void 非法参数创建被拒绝() {
        // 名称为空
        assertThrows(IllegalArgumentException.class, () -> {
            Coupon c = buildCoupon(1, "20.00", "0");
            c.setName(null);
            couponService.createCoupon(c);
        });
        // 类型非法
        assertThrows(IllegalArgumentException.class, () -> {
            Coupon c = buildCoupon(3, "20.00", "0");
            couponService.createCoupon(c);
        });
        // 折扣值超范围（10折及以上非法）
        assertThrows(IllegalArgumentException.class, () -> {
            Coupon c = buildCoupon(2, "10.00", "0");
            couponService.createCoupon(c);
        });
    }

    /**
     * 分页查询优惠券模板
     * <p>
     * @author ZuiM
     */
    @Test
    void 分页查询优惠券模板() {
        var page = couponService.getCouponPage(1, 10);
        assertNotNull(page);
        assertTrue(page.getRecords().size() <= 10);
    }

    // ========== 2. 发放与查询 ==========

    /**
     * 向用户发放优惠券后可在可用列表中查询到
     * <p>
     * @author ZuiM
     */
    @Test
    void 向用户发放优惠券() {
        User user = buildUser();
        Coupon coupon = buildCoupon(1, "20.00", "100.00");
        couponService.createCoupon(coupon);

        couponService.grantToUser(user.getId(), coupon.getId(), 30);

        List<UserCoupon> available = couponService.getAvailableCoupons(user.getId());
        assertFalse(available.isEmpty());
        assertTrue(available.stream().anyMatch(uc -> uc.getCouponId().equals(coupon.getId())));

        cleanData(user.getId(), coupon.getId());
    }

    // ========== 3. 优惠计算 ==========

    /**
     * 满减计算：订单 200 减 20
     * <p>
     * @author ZuiM
     */
    @Test
    void 满减优惠计算() {
        User user = buildUser();
        Coupon coupon = buildCoupon(1, "20.00", "100.00");
        couponService.createCoupon(coupon);
        couponService.grantToUser(user.getId(), coupon.getId(), 30);
        List<UserCoupon> list = couponService.getAvailableCoupons(user.getId());
        Long ucId = list.stream().filter(uc -> uc.getCouponId().equals(coupon.getId()))
                .findFirst().orElseThrow().getId();

        BigDecimal discount = couponService.validateAndCalcDiscount(ucId, user.getId(), new BigDecimal("200.00"));
        assertEquals(0, new BigDecimal("20.00").compareTo(discount));

        cleanData(user.getId(), coupon.getId());
    }

    /**
     * 折扣计算：8.5 折订单 100 减 15
     * <p>
     * @author ZuiM
     */
    @Test
    void 折扣优惠计算() {
        User user = buildUser();
        Coupon coupon = buildCoupon(2, "8.50", "0.00");
        couponService.createCoupon(coupon);
        couponService.grantToUser(user.getId(), coupon.getId(), 30);
        List<UserCoupon> list = couponService.getAvailableCoupons(user.getId());
        Long ucId = list.stream().filter(uc -> uc.getCouponId().equals(coupon.getId()))
                .findFirst().orElseThrow().getId();

        BigDecimal discount = couponService.validateAndCalcDiscount(ucId, user.getId(), new BigDecimal("100.00"));
        assertEquals(0, new BigDecimal("15.00").compareTo(discount));

        cleanData(user.getId(), coupon.getId());
    }

    /**
     * 未达门槛（满 100 减 20，订单 50）被拒绝
     * <p>
     * @author ZuiM
     */
    @Test
    void 未达使用门槛被拒绝() {
        User user = buildUser();
        Coupon coupon = buildCoupon(1, "20.00", "100.00");
        couponService.createCoupon(coupon);
        couponService.grantToUser(user.getId(), coupon.getId(), 30);
        List<UserCoupon> list = couponService.getAvailableCoupons(user.getId());
        Long ucId = list.stream().filter(uc -> uc.getCouponId().equals(coupon.getId()))
                .findFirst().orElseThrow().getId();

        assertThrows(IllegalArgumentException.class,
                () -> couponService.validateAndCalcDiscount(ucId, user.getId(), new BigDecimal("50.00")));

        cleanData(user.getId(), coupon.getId());
    }

    // ========== 4. 核销 ==========

    /**
     * 核销后优惠券不可再次使用
     * <p>
     * @author ZuiM
     */
    @Test
    void 核销后不可再次使用() {
        User user = buildUser();
        Coupon coupon = buildCoupon(1, "20.00", "100.00");
        couponService.createCoupon(coupon);
        couponService.grantToUser(user.getId(), coupon.getId(), 30);
        List<UserCoupon> list = couponService.getAvailableCoupons(user.getId());
        Long ucId = list.stream().filter(uc -> uc.getCouponId().equals(coupon.getId()))
                .findFirst().orElseThrow().getId();

        couponService.markUsed(ucId, user.getId(), 999999L);
        assertThrows(IllegalArgumentException.class,
                () -> couponService.validateAndCalcDiscount(ucId, user.getId(), new BigDecimal("200.00")));

        cleanData(user.getId(), coupon.getId());
    }

    // ========== 5. 角色工具方法 ==========

    /**
     * 角色字符串包含判断
     * <p>
     * @author ZuiM
     */
    @Test
    void 角色包含判断() {
        assertTrue(CouponService.hasRole("ROLE_USER,ROLE_VIP_USER", "ROLE_VIP_USER"));
        assertFalse(CouponService.hasRole("ROLE_USER", "ROLE_VIP_USER"));
        assertFalse(CouponService.hasRole(null, "ROLE_VIP_USER"));
    }
}
