package com.xuwenye.demo.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuwenye.demo.Entity.Coupon;
import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Entity.UserCoupon;
import com.xuwenye.demo.Mapper.CouponMapper;
import com.xuwenye.demo.Mapper.UserCouponMapper;
import com.xuwenye.demo.util.redis.RedisUtil;
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
 * 1.模板管理：管理员创建/分页查询优惠券模板（支持适用人群区分：普通券/VIP券）
 * 2.发放：向单个用户或全部VIP会员发放（VIP会员 = VIP用户 + VIP卖家）
 * 3.自助领取：领券中心按适用人群校验后发放
 * 4.使用：校验归属/状态/过期时间/门槛/适用人群后原子核销，防止并发重复使用
 * 5.计算：根据类型（满减/折扣）计算优惠金额
 * 查询均带 Redis 缓存（Cache-Aside），数据变更后通过 MQ 异步清理关联缓存
 * <p>
 * @author ZuiM
 */
@Service
@Slf4j
public class CouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final UserService userService;
    private final RedisUtil redisUtil;
    private final MQProducer mqProducer;

    // Redis缓存Key前缀
    private static final String COUPON_PAGE_CACHE_PREFIX = "demo:coupon:page:";
    private static final String USER_COUPON_CACHE_PREFIX = "demo:coupon:user:";
    private static final String CLAIMABLE_TEMPLATE_CACHE_PREFIX = "demo:coupon:claimable:";

    public CouponService(CouponMapper couponMapper,
                         UserCouponMapper userCouponMapper,
                         UserService userService,
                         RedisUtil redisUtil,
                         MQProducer mqProducer) {
        this.couponMapper = couponMapper;
        this.userCouponMapper = userCouponMapper;
        this.userService = userService;
        this.redisUtil = redisUtil;
        this.mqProducer = mqProducer;
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
        // 校验适用人群：1全部用户（普通券） 2仅VIP（VIP券，受众=VIP用户+VIP卖家）
        if (coupon.getTargetType() == null || (coupon.getTargetType() != 1 && coupon.getTargetType() != 2)) {
            throw new IllegalArgumentException("适用人群必须为 1全部用户 或 2仅VIP");
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
        boolean result = couponMapper.insert(coupon) > 0;
        if (result) {
            // 新增模板后异步清理优惠券模板分页缓存与领券中心可领列表缓存
            sendCouponPageCacheRefresh();
            sendClaimableTemplateCacheRefresh();
        }
        return result;
    }

    /**
     * 分页查询优惠券模板（管理员，Cache-Aside 缓存模式）
     * <p>
     * @author ZuiM
     * @param page 页码
     * @param size 每页条数
     * @return Page<Coupon> 分页模板
     */
    @SuppressWarnings("unchecked")
    public Page<Coupon> getCouponPage(int page, int size) {
        String cacheKey = COUPON_PAGE_CACHE_PREFIX + page + ":" + size;
        // 第 1 步：先从 Redis 查
        Page<Coupon> cached = (Page<Coupon>) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // 第 2 步：Redis 没有，查 MySQL
        Page<Coupon> pageObj = new Page<>(page, size);
        QueryWrapper<Coupon> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time");
        Page<Coupon> result = couponMapper.selectPage(pageObj, wrapper);
        // 第 3 步：写入 Redis
        if (result != null && !result.getRecords().isEmpty()) {
            redisUtil.set(cacheKey, result);
        }
        return result;
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
        // VIP券仅可发放给VIP会员（VIP用户+VIP卖家）
        if (coupon.getTargetType() != null && coupon.getTargetType() == 2 && !isVipMember(user)) {
            throw new IllegalArgumentException("VIP优惠券仅可发放给会员用户或会员卖家");
        }
        // 原子扣减剩余数量
        if (couponMapper.decrementRemain(couponId) <= 0) {
            throw new IllegalStateException("优惠券库存不足，发放失败");
        }
        UserCoupon uc = buildUserCoupon(userId, coupon, expireDays);
        userCouponMapper.insert(uc);
        // 发放后异步清理该用户优惠券缓存
        sendUserCouponCacheRefresh(userId);
        log.info("向用户 {} 发放优惠券 {} 成功", userId, coupon.getName());
    }

    /**
     * 向全部 VIP 会员（VIP用户 + VIP卖家）发放优惠券
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
        // 查询全部启用且为 VIP 会员（VIP用户 + VIP卖家）的用户
        List<User> vipUsers = listVipUsers();
        if (vipUsers.isEmpty()) {
            log.info("没有符合条件的 VIP 会员，跳过批量发放");
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
        if (granted > 0) {
            // 批量发放后异步清理全部用户优惠券缓存
            sendAllUserCouponCacheRefresh();
        }
        log.info("向 VIP 会员批量发放优惠券 {} 完成，共 {} 张", coupon.getName(), granted);
        return granted;
    }

    /**
     * 向全部启用用户发放优惠券（含普通用户、VIP用户、卖家、VIP卖家）
     * 逐页加载全部启用用户，逐个发放，某用户失败不影响其余用户
     * <p>
     * @author ZuiM
     * @param couponId 优惠券模板ID
     * @param expireDays 有效天数
     * @return int 成功发放数量
     */
    @Transactional
    public int grantToAllUsers(Long couponId, int expireDays) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || coupon.getStatus() != 1) {
            throw new IllegalArgumentException("优惠券不存在或已停用");
        }
        // 分页加载全部启用用户（is_deleted=0 由 @TableLogic 自动过滤）
        int pageSize = 100;
        int granted = 0;
        Page<User> page = new Page<>(1, pageSize);
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        while (true) {
            Page<User> p = userService.getUserListByQuery(wrapper, page);
            if (p.getRecords().isEmpty()) {
                break;
            }
            for (User u : p.getRecords()) {
                if (couponMapper.decrementRemain(couponId) <= 0) {
                    // 库存不足，停止发放
                    log.info("优惠券 {} 库存不足，发放提前结束", coupon.getName());
                    if (granted > 0) {
                        sendAllUserCouponCacheRefresh();
                    }
                    return granted;
                }
                userCouponMapper.insert(buildUserCoupon(u.getId(), coupon, expireDays));
                granted++;
            }
            if (p.getRecords().size() < pageSize) {
                break;
            }
            page.setCurrent(page.getCurrent() + 1);
        }
        if (granted > 0) {
            // 批量发放后异步清理全部用户优惠券缓存
            sendAllUserCouponCacheRefresh();
        }
        log.info("向全部用户批量发放优惠券 {} 完成，共 {} 张", coupon.getName(), granted);
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
        // 适用人群快照（1普通 2VIP专属）
        uc.setTargetType(coupon.getTargetType() == null ? 1 : coupon.getTargetType());
        uc.setStatus(0);
        uc.setReceiveTime(LocalDateTime.now());
        int days = Math.max(expireDays, 1);
        uc.setExpireTime(LocalDateTime.now().plusDays(days));
        return uc;
    }

    /**
     * 查询所有启用且为 VIP 会员的用户（VIP买家 + VIP卖家）
     * <p>
     * @author ZuiM
     * @return List<User> VIP 会员列表
     */
    private List<User> listVipUsers() {
        List<User> result = new ArrayList<>();
        Page<User> page = new Page<>(1, 100);
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        Page<User> p = userService.getUserListByQuery(wrapper, page);
        for (User u : p.getRecords()) {
            if (isVipMember(u)) {
                result.add(u);
            }
        }
        return result;
    }

    /**
     * 判断用户是否为 VIP 会员（VIP买家 ROLE_VIP_USER 或 VIP卖家 ROLE_VIP_SELLER）
     * <p>
     * @author ZuiM
     * @param user 用户实体
     * @return boolean 是否为 VIP 会员
     */
    private boolean isVipMember(User user) {
        String role = user.getUserRole();
        return hasRole(role, "ROLE_VIP_USER") || hasRole(role, "ROLE_VIP_SELLER");
    }

    // ======================== 查询（用户） ========================

    /**
     * 查询用户的可用优惠券（未使用且未过期，Cache-Aside 缓存模式）
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     * @return List<UserCoupon> 可用优惠券
     */
    @SuppressWarnings("unchecked")
    public List<UserCoupon> getAvailableCoupons(Long userId) {
        String cacheKey = USER_COUPON_CACHE_PREFIX + userId + ":available";
        // 第 1 步：先从 Redis 查
        List<UserCoupon> cached = (List<UserCoupon>) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // 第 2 步：Redis 没有，查 MySQL
        List<UserCoupon> coupons = userCouponMapper.findAvailableByUserId(userId);
        // 第 3 步：写入 Redis
        if (coupons != null && !coupons.isEmpty()) {
            redisUtil.set(cacheKey, coupons);
        }
        return coupons;
    }

    /**
     * 查询用户的全部优惠券（含已用/已过期，Cache-Aside 缓存模式）
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     * @return List<UserCoupon> 全部优惠券
     */
    @SuppressWarnings("unchecked")
    public List<UserCoupon> getUserCoupons(Long userId) {
        String cacheKey = USER_COUPON_CACHE_PREFIX + userId + ":all";
        // 第 1 步：先从 Redis 查
        List<UserCoupon> cached = (List<UserCoupon>) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // 第 2 步：Redis 没有，查 MySQL
        List<UserCoupon> coupons = userCouponMapper.findAllByUserId(userId);
        // 第 3 步：写入 Redis
        if (coupons != null && !coupons.isEmpty()) {
            redisUtil.set(cacheKey, coupons);
        }
        return coupons;
    }

    // ======================== 自助领取（领券中心） ========================

    /**
     * 查询可自助领取的优惠券模板（启用且仍有剩余，Cache-Aside 缓存模式）
     * 供首页「领券中心」展示，公开接口无需登录
     * <p>
     * @author ZuiM
     * @return List<Coupon> 可领取的模板列表
     */
    @SuppressWarnings("unchecked")
    public List<Coupon> getClaimableTemplates() {
        String cacheKey = CLAIMABLE_TEMPLATE_CACHE_PREFIX + "list";
        // 第 1 步：先从 Redis 查
        List<Coupon> cached = (List<Coupon>) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // 第 2 步：Redis 没有，查 MySQL
        List<Coupon> templates = couponMapper.selectClaimableTemplates();
        // 第 3 步：写入 Redis
        if (templates != null && !templates.isEmpty()) {
            redisUtil.set(cacheKey, templates);
        }
        return templates;
    }

    /**
     * 用户自助领取优惠券（首页「领券中心」）
     * 1.校验模板存在且启用、剩余数量充足
     * 2.校验适用人群：VIP券仅限 VIP 用户 / VIP 卖家领取
     * 3.校验同一用户未重复领取同一模板
     * 4.原子扣减剩余数量（防止超发）
     * 5.写入用户优惠券（含快照信息，默认有效期 30 天）
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     * @param couponId 优惠券模板ID
     */
    @Transactional
    public void claimCoupon(Long userId, Long couponId) {
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || coupon.getStatus() != 1) {
            throw new IllegalArgumentException("优惠券不存在或已停用");
        }
        // VIP券仅限 VIP 用户 / VIP 卖家领取
        if (coupon.getTargetType() != null && coupon.getTargetType() == 2 && !isVipMember(user)) {
            throw new IllegalArgumentException("该优惠券为VIP专属，仅会员用户或会员卖家可领取");
        }
        if (coupon.getRemainCount() == null || coupon.getRemainCount() <= 0) {
            throw new IllegalStateException("优惠券已被领完");
        }
        // 同一用户对同一模板仅可领取一次
        if (userCouponMapper.countByUserAndCoupon(userId, couponId) > 0) {
            throw new IllegalStateException("您已领取过该优惠券，请勿重复领取");
        }
        // 原子扣减剩余数量
        if (couponMapper.decrementRemain(couponId) <= 0) {
            throw new IllegalStateException("优惠券已被领完，请稍后再试");
        }
        UserCoupon uc = buildUserCoupon(userId, coupon, 30);
        userCouponMapper.insert(uc);
        // 领取后异步清理该用户优惠券缓存与可领模板缓存
        sendUserCouponCacheRefresh(userId);
        sendClaimableTemplateCacheRefresh();
        log.info("用户 {} 自助领取优惠券 {} 成功", userId, coupon.getName());
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
        // 核销后异步清理该用户优惠券缓存
        sendUserCouponCacheRefresh(userId);
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

    // ======================== 缓存刷新工具方法 ========================

    /**
     * 发送「优惠券模板分页」缓存刷新任务（新增模板时调用）
     * <p>
     * @author ZuiM
     */
    private void sendCouponPageCacheRefresh() {
        List<String> keys = new ArrayList<>();
        keys.add(COUPON_PAGE_CACHE_PREFIX + "*");
        mqProducer.sendCacheRefreshTask("coupon", "clear", keys);
    }

    /**
     * 发送「单个用户优惠券」缓存刷新任务（发放/核销时调用）
     * 同时清理该用户的可用券与全部券缓存
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     */
    private void sendUserCouponCacheRefresh(Long userId) {
        List<String> keys = new ArrayList<>();
        keys.add(USER_COUPON_CACHE_PREFIX + userId + ":*");
        mqProducer.sendCacheRefreshTask("coupon", "clear", keys);
    }

    /**
     * 发送「全部用户优惠券」缓存刷新任务（批量发放时调用）
     * <p>
     * @author ZuiM
     */
    private void sendAllUserCouponCacheRefresh() {
        List<String> keys = new ArrayList<>();
        keys.add(USER_COUPON_CACHE_PREFIX + "*");
        mqProducer.sendCacheRefreshTask("coupon", "clear", keys);
    }

    /**
     * 发送「可领模板列表」缓存刷新任务（自助领取导致剩余数量变化时调用）
     * <p>
     * @author ZuiM
     */
    private void sendClaimableTemplateCacheRefresh() {
        List<String> keys = new ArrayList<>();
        keys.add(CLAIMABLE_TEMPLATE_CACHE_PREFIX + "*");
        mqProducer.sendCacheRefreshTask("coupon", "clear", keys);
    }
}
