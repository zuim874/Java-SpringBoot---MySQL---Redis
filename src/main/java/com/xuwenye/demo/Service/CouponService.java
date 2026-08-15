package com.xuwenye.demo.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuwenye.demo.Entity.Coupon;
import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Entity.UserCoupon;
import com.xuwenye.demo.Mapper.CouponMapper;
import com.xuwenye.demo.Mapper.UserCouponMapper;
import com.xuwenye.demo.util.redis.RedisLockHelper;
import com.xuwenye.demo.util.redis.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 优惠券业务层
 * 1.模板管理：管理员创建/分页查询优惠券模板（支持适用人群区分：普通券/VIP券）
 * 2.发放：向单个用户发放；向全部用户/VIP会员批量发放（费时事务，走消息队列异步削峰）
 * 3.自助领取：领券中心按适用人群校验后发放
 * 4.使用：校验归属/状态/过期时间/门槛/适用人群后原子核销，防止并发重复使用
 * 5.计算：根据类型（满减/折扣）计算优惠金额
 * 6.限领约束：同类优惠券每人最多收到或领取一次（Redis 已领取集合预校验 + 数据库唯一索引兜底）
 * 7.数据安全：领取等敏感写操作使用分布式锁串行化，原子扣减库存防止超发
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
    private final RedisLockHelper redisLockHelper;
    private final TransactionTemplate transactionTemplate;

    // Redis缓存Key前缀
    private static final String COUPON_PAGE_CACHE_PREFIX = "demo:coupon:page:";
    private static final String USER_COUPON_CACHE_PREFIX = "demo:coupon:user:";
    private static final String CLAIMABLE_TEMPLATE_CACHE_PREFIX = "demo:coupon:claimable:";
    // 已领取用户集合：记录「已收到或已领取某模板」的全部用户ID（同类券每人限领一次的 O(1) 预校验）
    private static final String RECEIVED_SET_PREFIX = "demo:coupon:received:set:";
    // 已领取集合 TTL（天）：过期后由数据库唯一索引兜底，避免集合无限膨胀
    private static final int RECEIVED_SET_TTL_DAYS = 30;
    // 领取/发放分布式锁前缀（粒度：用户 + 优惠券，串行化同一用户的同一券操作）
    private static final String COUPON_CLAIM_LOCK_PREFIX = "demo:coupon:claim:lock:";
    // 批量发放每页加载用户数
    private static final int BATCH_PAGE_SIZE = 100;

    public CouponService(CouponMapper couponMapper,
                         UserCouponMapper userCouponMapper,
                         UserService userService,
                         RedisUtil redisUtil,
                         MQProducer mqProducer,
                         RedisLockHelper redisLockHelper,
                         PlatformTransactionManager transactionManager) {
        this.couponMapper = couponMapper;
        this.userCouponMapper = userCouponMapper;
        this.userService = userService;
        this.redisUtil = redisUtil;
        this.mqProducer = mqProducer;
        this.redisLockHelper = redisLockHelper;
        // 程序化事务模板：批量发放时逐用户开启独立小事务，避免单个长事务占用连接池
        this.transactionTemplate = new TransactionTemplate(transactionManager);
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
     * 2.分布式锁串行化同一用户对同一券的发放操作
     * 3.Redis 已领取集合 O(1) 预校验：同类券每人限领一次
     * 4.原子扣减剩余数量（防止超发）
     * 5.写入用户优惠券，失败时利用数据库唯一索引兜底回滚扣减
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

        // 分布式锁：粒度 = 用户 + 优惠券，防止并发重复发放/领取
        String lockKey = COUPON_CLAIM_LOCK_PREFIX + userId + ":" + couponId;
        try {
            boolean locked = redisLockHelper.tryLock(lockKey, 3, TimeUnit.SECONDS);
            if (!locked) {
                throw new IllegalStateException("系统繁忙，请稍后再试");
            }
            // Redis 已领取集合预校验：O(1) 判断，同类券每人限领一次
            String receivedSetKey = RECEIVED_SET_PREFIX + couponId;
            if (redisUtil.sIsMember(receivedSetKey, userId)) {
                throw new IllegalStateException("该用户已收到过同类优惠券，请勿重复发放");
            }
            // 数据库兜底校验（防止 Redis 缓存过期后数据不一致）
            if (userCouponMapper.countByUserAndCoupon(userId, couponId) > 0) {
                // 缓存已过期，同步写入 Redis Set
                redisUtil.sAdd(receivedSetKey, userId);
                redisUtil.expire(receivedSetKey, RECEIVED_SET_TTL_DAYS, TimeUnit.DAYS);
                throw new IllegalStateException("该用户已收到过同类优惠券，请勿重复发放");
            }
            // 原子扣减剩余数量
            if (couponMapper.decrementRemain(couponId) <= 0) {
                throw new IllegalStateException("优惠券库存不足，发放失败");
            }
            UserCoupon uc = buildUserCoupon(userId, coupon, expireDays);
            try {
                userCouponMapper.insert(uc);
            } catch (DuplicateKeyException e) {
                // 唯一索引兜底：已有相同 (user_id, coupon_id) 记录
                // @Transactional 会随异常整体回滚，原子扣减一并还原
                throw new IllegalStateException("该用户已收到过同类优惠券，请勿重复发放");
            }
            // 发放后写入 Redis 已领取集合
            redisUtil.sAdd(receivedSetKey, userId);
            redisUtil.expire(receivedSetKey, RECEIVED_SET_TTL_DAYS, TimeUnit.DAYS);
            // 异步清理该用户优惠券缓存
            sendUserCouponCacheRefresh(userId);
            log.info("向用户 {} 发放优惠券 {} 成功", userId, coupon.getName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("系统中断，请稍后再试", e);
        } finally {
            redisLockHelper.unlock(lockKey);
        }
    }

    /**
     * 向全部 VIP 会员（VIP用户 + VIP卖家）批量发放优惠券（异步提交）
     * 1.仅做模板存在/启用的快速校验，随后立即返回，不阻塞管理员请求
     * 2.实际发放通过消息队列在后台削峰执行（用户量大时为费时事务，避免拖垮后端）
     * <p>
     * @author ZuiM
     * @param couponId 优惠券模板ID
     * @param expireDays 有效天数
     */
    public void grantToAllVipUsersAsync(Long couponId, int expireDays) {
        validateGrantTarget(couponId);
        mqProducer.sendCouponGrantTask(couponId, expireDays, 2);
    }

    /**
     * 向全部启用用户批量发放优惠券（异步提交）
     * 1.仅做模板存在/启用的快速校验，随后立即返回，不阻塞管理员请求
     * 2.实际发放通过消息队列在后台削峰执行（用户量大时为费时事务，避免拖垮后端）
     * <p>
     * @author ZuiM
     * @param couponId 优惠券模板ID
     * @param expireDays 有效天数
     */
    public void grantToAllUsersAsync(Long couponId, int expireDays) {
        validateGrantTarget(couponId);
        mqProducer.sendCouponGrantTask(couponId, expireDays, 1);
    }

    /**
     * 批量发放异步入口的快速校验：模板必须存在且启用
     * <p>
     * @author ZuiM
     * @param couponId 优惠券模板ID
     */
    private void validateGrantTarget(Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || coupon.getStatus() != 1) {
            throw new IllegalArgumentException("优惠券不存在或已停用");
        }
    }

    /**
     * 批量发放优惠券后台执行器（由 MQConsumer 异步调用，削峰执行）
     * 1.不占用 HTTP 请求线程，通过消息队列逐步消费
     * 2.逐用户开启独立小事务 + Redis 已领取集合去重，某用户失败不影响其余
     * 3.逐页加载用户，库存不足时提前终止
     * <p>
     * @author ZuiM
     * @param couponId 优惠券模板ID
     * @param expireDays 有效天数
     * @param target 发放目标：1全部用户 2全部VIP会员
     * @return int 成功发放数量
     */
    public int executeBatchGrant(Long couponId, int expireDays, int target) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || coupon.getStatus() != 1) {
            log.warn("批量发放终止：优惠券 {} 不存在或已停用", couponId);
            return 0;
        }
        // 预热 Redis 已领取集合（懒加载，避免每次发放都查数据库）
        String receivedSetKey = RECEIVED_SET_PREFIX + couponId;
        if (!redisUtil.hasKey(receivedSetKey)) {
            loadReceivedSet(couponId);
        }
        // 快照已领取集合到内存：批量循环内直接 O(1) 判断去重，避免每个用户一次 Redis 往返
        // （数据一致性仍由数据库唯一索引 uk_user_coupon 兜底，快照略滞后无安全风险）
        Set<Object> members = redisUtil.sMembers(receivedSetKey);
        Set<Object> receivedSetSnapshot = members == null ? new HashSet<>() : members;
        int granted = 0;
        int page = 1;
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        while (true) {
            Page<User> p = new Page<>(page, BATCH_PAGE_SIZE);
            Page<User> userPage = userService.getUserListByQuery(wrapper, p);
            if (userPage.getRecords().isEmpty()) {
                break;
            }
            for (User u : userPage.getRecords()) {
                // 目标过滤：仅VIP会员发放
                if (target == 2 && !isVipMember(u)) {
                    continue;
                }
                // 内存快照去重：同类券每人限领一次（O(1) 判断，避免逐用户 Redis 往返）
                if (receivedSetSnapshot.contains(u.getId())) {
                    continue;
                }
                // 逐用户小事务：独立提交，不阻塞整体进度
                Boolean ok = transactionTemplate.execute(status -> {
                    try {
                        if (couponMapper.decrementRemain(couponId) <= 0) {
                            return false; // 库存不足，跳过该用户
                        }
                        UserCoupon uc = buildUserCoupon(u.getId(), coupon, expireDays);
                        try {
                            userCouponMapper.insert(uc);
                        } catch (DuplicateKeyException e) {
                            // 唯一索引兜底，回滚库存
                            couponMapper.incrementRemain(couponId);
                            return false;
                        }
                        // 写入 Redis 已领取集合，并同步内存快照（TTL 已由懒加载/首次写入时设定）
                        redisUtil.sAdd(receivedSetKey, u.getId());
                        receivedSetSnapshot.add(u.getId());
                        return true;
                    } catch (Exception e) {
                        log.warn("向用户 {} 发放优惠券 {} 失败: {}", u.getId(), couponId, e.getMessage());
                        status.setRollbackOnly();
                        return false;
                    }
                });
                if (ok != null && ok) {
                    granted++;
                } else {
                    // 库存不足，检查是否还有剩余
                    Coupon latest = couponMapper.selectById(couponId);
                    if (latest == null || latest.getRemainCount() == null || latest.getRemainCount() <= 0) {
                        log.info("优惠券 {} 库存不足，批量发放提前终止，已发放 {} 张", coupon.getName(), granted);
                        // 强制刷新一次可领模板缓存（库存已变）
                        sendClaimableTemplateCacheRefresh();
                        sendAllUserCouponCacheRefresh();
                        return granted;
                    }
                }
            }
            if (userPage.getRecords().size() < BATCH_PAGE_SIZE) {
                break;
            }
            page++;
        }
        if (granted > 0) {
            sendClaimableTemplateCacheRefresh();
            sendAllUserCouponCacheRefresh();
        }
        log.info("批量发放完成: couponId={}, target={}, granted={}", couponId, target, granted);
        return granted;
    }

    /**
     * 懒加载 Redis「已领取用户集合」：从数据库查询已领取某模板的用户 ID 并写入 Redis Set
     * <p>
     * @author ZuiM
     * @param couponId 优惠券模板ID
     */
    private void loadReceivedSet(Long couponId) {
        String receivedSetKey = RECEIVED_SET_PREFIX + couponId;
        List<Long> userIds = userCouponMapper.selectUserIdsByCouponId(couponId);
        if (!userIds.isEmpty()) {
            redisUtil.sAddAll(receivedSetKey, userIds);
        }
        redisUtil.expire(receivedSetKey, RECEIVED_SET_TTL_DAYS, TimeUnit.DAYS);
        log.debug("已领取集合加载: couponId={}, userIds={}", couponId, userIds.size());
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
     * 1.分布式锁串行化同一用户对同一模板的领取，保证限领校验的原子性（防并发重复领取）
     * 2.Redis 已领取集合 O(1) 预校验，未命中时再查数据库兜底（减少 MySQL 查询）
     * 3.校验模板存在且启用、剩余数量充足、适用人群（VIP券仅限会员）
     * 4.原子扣减剩余数量（防止超发）
     * 5.写入用户优惠券（含快照信息，默认有效期 30 天）
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     * @param couponId 优惠券模板ID
     */
    @Transactional
    public void claimCoupon(Long userId, Long couponId) {
        String lockKey = COUPON_CLAIM_LOCK_PREFIX + userId + ":" + couponId;
        try {
            // 分布式锁：防止同一用户并发领取同一模板造成重复（粒度 = 用户 + 优惠券）
            boolean locked = redisLockHelper.tryLock(lockKey, 3, TimeUnit.SECONDS);
            if (!locked) {
                throw new IllegalStateException("系统繁忙，请稍后再试");
            }
            // 限领校验：Redis 已领取集合 O(1) 预判，减少数据库查询
            String receivedSetKey = RECEIVED_SET_PREFIX + couponId;
            if (redisUtil.sIsMember(receivedSetKey, userId)) {
                throw new IllegalStateException("您已领取过该优惠券，请勿重复领取");
            }
            // 数据库兜底校验（Redis 集合过期/未命中时，保证数据一致）
            if (userCouponMapper.countByUserAndCoupon(userId, couponId) > 0) {
                // 补写 Redis 集合，避免下次再次查库
                redisUtil.sAdd(receivedSetKey, userId);
                redisUtil.expire(receivedSetKey, RECEIVED_SET_TTL_DAYS, TimeUnit.DAYS);
                throw new IllegalStateException("您已领取过该优惠券，请勿重复领取");
            }
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
            // 原子扣减剩余数量
            if (couponMapper.decrementRemain(couponId) <= 0) {
                throw new IllegalStateException("优惠券已被领完，请稍后再试");
            }
            UserCoupon uc = buildUserCoupon(userId, coupon, 30);
            try {
                userCouponMapper.insert(uc);
            } catch (DuplicateKeyException e) {
                // 数据库唯一索引兜底：极端并发下拦截重复领取，随事务回滚扣减
                throw new IllegalStateException("您已领取过该优惠券，请勿重复领取");
            }
            // 写入 Redis 已领取集合
            redisUtil.sAdd(receivedSetKey, userId);
            redisUtil.expire(receivedSetKey, RECEIVED_SET_TTL_DAYS, TimeUnit.DAYS);
            // 领取后异步清理该用户优惠券缓存与可领模板缓存
            sendUserCouponCacheRefresh(userId);
            sendClaimableTemplateCacheRefresh();
            log.info("用户 {} 自助领取优惠券 {} 成功", userId, coupon.getName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("系统繁忙，请稍后再试");
        } finally {
            redisLockHelper.unlock(lockKey);
        }
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
