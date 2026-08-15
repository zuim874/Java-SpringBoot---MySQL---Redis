package com.xuwenye.demo.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuwenye.demo.Entity.Seller;
import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Mapper.SellerMapper;
import com.xuwenye.demo.util.redis.RedisUtil;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 卖家业务层
 * 1.查询：卖家列表/详情/按用户ID查店铺，均带 Redis 缓存（Cache-Aside）
 * 2.写操作：新增/更新/删除，写库后维护缓存
 * 3.账号绑定：新增卖家时绑定已存在的 sys_user 账号（用户名 + 邮箱双重校验，不新建账号），
 *   卖家删除时移除其账号的卖家角色（账号本身保留），保证两表数据一致性
 * <p>
 * @author ZuiM
 */
@Service
public class SellerService {
    private final SellerMapper sellerMapper;
    private final RedisUtil redisUtil;
    private final MQProducer mqProducer;
    private final UserService userService;

    // Redis缓存Key前缀
    private static final String SELLER_LIST_CACHE_KEY = "demo:seller:list:all";
    private static final String SELLER_DETAIL_CACHE_PREFIX = "demo:seller:detail:";

    public SellerService(SellerMapper sellerMapper,
                         RedisUtil redisUtil,
                         MQProducer mqProducer,
                         UserService userService) {
        this.sellerMapper = sellerMapper;
        this.redisUtil = redisUtil;
        this.mqProducer = mqProducer;
        this.userService = userService;
    }

    /**
     * 分页查询卖家（含 Redis 缓存）
     * 1.先从 Redis 查（仅第一页缓存）
     * 2.未命中则查 MySQL
     * 3.查到了写入 Redis
     * <p>
     * @author ZuiM
     * @param page 页码
     * @param size 每页条数
     * @return Page<Seller> 分页卖家列表
     */
    public Page<Seller> getSellerPage(int page, int size) {
        // 使用 MyBatis-Plus 分页查询，排除逻辑删除记录
        LambdaQueryWrapper<Seller> wrapper = new LambdaQueryWrapper<Seller>()
                .eq(Seller::getIsDeleted, 0);
        Page<Seller> sellerPage = sellerMapper.selectPage(new Page<>(page, size), wrapper);
        return sellerPage;
    }

    /**
     * 获取所有卖家（含 Redis 缓存）
     * 1.先从 Redis 查
     * 2.未命中则查 MySQL
     * 3.查到了写入 Redis
     * <p>
     * @author ZuiM
     * @return List<Seller> 卖家列表
     */
    @SuppressWarnings("unchecked")
    public List<Seller> getAllSellers() {
        // 第 1 步：先从 Redis 查
        List<Seller> cached = (List<Seller>) redisUtil.get(SELLER_LIST_CACHE_KEY);
        if (cached != null) {
            return cached;
        }
        // 第 2 步：Redis 没有，查 MySQL
        List<Seller> sellers = sellerMapper.findAllSellers();
        // 第 3 步：写入 Redis
        if (sellers != null && !sellers.isEmpty()) {
            redisUtil.set(SELLER_LIST_CACHE_KEY, sellers);
        }
        return sellers;
    }

    /**
     * 按ID查询卖家（含 Redis 缓存）
     * 1.先从 Redis 查
     * 2.未命中则查 MySQL
     * 3.查到了写入 Redis
     * <p>
     * @author ZuiM
     * @param id 卖家ID
     * @return Seller 卖家（可能为null）
     */
    public Seller getSellerById(Long id) {
        String cacheKey = SELLER_DETAIL_CACHE_PREFIX + id;
        // 第 1 步：先从 Redis 查
        Seller cached = (Seller) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // 第 2 步：Redis 没有，查 MySQL
        Seller seller = sellerMapper.findSellerById(id);
        // 第 3 步：写入 Redis
        if (seller != null) {
            redisUtil.set(cacheKey, seller);
        }
        return seller;
    }

    /**
     * 保存卖家（新增，绑定已存在的登录账号）
     * 1.校验卖家名称非空
     * 2.以表单填入的 username/email 校验并绑定已存在的 sys_user 账号（不新建账号），
     *   避免卖家店名为中文时生成非法用户名；邮箱二次校验防止绑定错账号
     * 3.将账号ID写入 seller.userId 后插入卖家，同一事务内完成「账号绑定 + 店铺」写入
     * 4.清除卖家列表缓存（异步 MQ）
     * <p>
     * @author ZuiM
     * @param seller 卖家实体（sellerName/address/sellerContact + username/email）
     * @return boolean true=保存成功
     */
    @Transactional
    public boolean saveSeller(Seller seller) {
        if (seller.getSellerName() == null || seller.getSellerName().trim().isEmpty()) {
            throw new IllegalArgumentException("卖家名称不能为空");
        }
        // 绑定已存在的登录账号（用户名 + 邮箱双重校验，避免绑定错账号）
        User account = bindExistingSellerAccount(seller);
        seller.setUserId(account.getId());

        boolean result;
        try {
            result = sellerMapper.insert(seller) > 0;
        } catch (DuplicateKeyException e) {
            // 并发兜底：uk_user_id 唯一索引拦截同一账号重复绑定
            throw new IllegalArgumentException("该账号已绑定其他卖家，请更换账号");
        }
        if (result) {
            // 异步发送缓存刷新任务
            sendSellerCacheRefreshTask();
        }
        return result;
    }

    /**
     * 绑定已存在的用户账号为卖家登录账号（替代旧版「自动新建账号」）
     * 1.用户名、邮箱必填，用于定位并二次校验账号身份，避免绑定错误
     * 2.按用户名查询可用账号（不含已禁用/已逻辑删除），不存在则报错
     * 3.校验账号邮箱与表单一致（邮箱不匹配说明账号选错，拒绝绑定）
     * 4.校验该账号未被其他卖家绑定（user_id 唯一索引兜底）
     * 5.账号若无卖家角色则升级为 ROLE_SELLER，使其具备商家后台权限
     * <p>
     * @author ZuiM
     * @param seller 卖家实体（username/email 表单字段）
     * @return User 绑定成功的用户账号
     */
    private User bindExistingSellerAccount(Seller seller) {
        String username = seller.getUsername() == null ? "" : seller.getUsername().trim();
        String email = seller.getEmail() == null ? "" : seller.getEmail().trim();
        if (username.isEmpty()) {
            throw new IllegalArgumentException("请填写要绑定的登录账号用户名");
        }
        if (email.isEmpty()) {
            throw new IllegalArgumentException("请填写要绑定的登录账号邮箱");
        }

        // 1.账号必须存在且可用（未禁用、未逻辑删除）
        User account = userService.findUserableUser(username);
        if (account == null) {
            throw new IllegalArgumentException("登录账号不存在或已被禁用：" + username);
        }
        // 2.邮箱二次校验，防止绑定错账号
        String accountEmail = account.getEmail() == null ? "" : account.getEmail().trim();
        if (!accountEmail.equalsIgnoreCase(email)) {
            throw new IllegalArgumentException("用户名与邮箱不匹配，请核对后重新填写");
        }
        // 3.同一账号不能绑定多个卖家
        if (sellerMapper.findSellerByUserId(account.getId()) != null) {
            throw new IllegalArgumentException("该账号已绑定其他卖家，请更换账号");
        }
        // 4.账号需具备卖家角色，否则升级为 ROLE_SELLER
        if (!hasSellerRole(account.getUserRole())) {
            userService.updateUserRole(account.getId(), "ROLE_SELLER", true);
        }
        return account;
    }

    /**
     * 判断角色串是否已具备卖家角色（ROLE_SELLER / ROLE_VIP_SELLER）
     * <p>
     * @author ZuiM
     * @param userRole 用户角色串（逗号分隔，可能为 null）
     * @return true=已具备卖家角色
     */
    private boolean hasSellerRole(String userRole) {
        if (userRole == null || userRole.trim().isEmpty()) {
            return false;
        }
        for (String role : userRole.split(",")) {
            String r = role.trim();
            if ("ROLE_SELLER".equals(r) || "ROLE_VIP_SELLER".equals(r)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 按用户ID查询卖家（含 Redis 缓存）
     * 1.先从 Redis 查
     * 2.未命中则查 MySQL
     * 3.查到了写入 Redis
     * <p>
     * @author ZuiM
     * @param userId 关联的用户ID（sys_user.id）
     * @return Seller 卖家（可能为null）
     */
    public Seller getSellerByUserId(Long userId) {
        String cacheKey = SELLER_DETAIL_CACHE_PREFIX + "userId:" + userId;
        // 第 1 步：先从 Redis 查
        Seller cached = (Seller) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // 第 2 步：Redis 没有，查 MySQL
        Seller seller = sellerMapper.findSellerByUserId(userId);
        // 第 3 步：写入 Redis
        if (seller != null) {
            redisUtil.set(cacheKey, seller);
        }
        return seller;
    }

    /**
     * 获取卖家绑定的登录账号（店铺信息展示用，未绑定返回 null）
     * <p>
     * @author ZuiM
     * @param seller 卖家实体（需含 userId）
     * @return User 登录账号（可能为null）
     */
    public User getUserForSeller(Seller seller) {
        if (seller == null || seller.getUserId() == null) {
            return null;
        }
        return userService.getUserById(seller.getUserId());
    }

    /**
     * 更新卖家信息
     * 1.执行 updateById
     * 2.清除所有相关缓存（异步 MQ）
     * <p>
     * @author ZuiM
     * @param seller 卖家实体（必须包含id）
     * @return boolean true=更新成功
     */
    @Transactional
    public boolean updateSeller(Seller seller) {
        boolean result = sellerMapper.updateById(seller) > 0;
        if (result) {
            // 异步发送缓存刷新任务
            sendSellerCacheRefreshTask();
            // 附带清除详情缓存
            List<String> keys = new ArrayList<>();
            keys.add(SELLER_DETAIL_CACHE_PREFIX + seller.getId());
            if (seller.getUserId() != null) {
                keys.add(SELLER_DETAIL_CACHE_PREFIX + "userId:" + seller.getUserId());
            }
            mqProducer.sendCacheRefreshTask("seller", "delete", keys);
        }
        return result;
    }

    /**
     * 逻辑删除卖家（移除绑定账号的卖家角色，账号本身保留）
     * 1.查出卖家（取绑定 userId）
     * 2.执行逻辑删除
     * 3.移除绑定账号的卖家角色（ROLE_SELLER/ROLE_VIP_SELLER），使其失去商家权限；
     *   账号本身保留（它是真实用户账号，不应随店铺删除被禁用）
     * 4.清除所有相关缓存（异步 MQ）
     * <p>
     * @author ZuiM
     * @param id 卖家ID
     * @return boolean true=删除成功
     */
    @Transactional
    public boolean deleteSeller(Long id) {
        Seller seller = sellerMapper.findSellerById(id);
        boolean result = sellerMapper.deleteById(id) > 0;
        if (result) {
            // 移除绑定账号的卖家角色（账号本身保留，避免误伤真实用户）
            if (seller != null && seller.getUserId() != null) {
                User account = userService.getUserById(seller.getUserId());
                if (account != null && hasSellerRole(account.getUserRole())) {
                    userService.updateUserRole(account.getId(), "ROLE_SELLER", false);
                    userService.updateUserRole(account.getId(), "ROLE_VIP_SELLER", false);
                }
            }
            // 异步发送缓存刷新任务
            sendSellerCacheRefreshTask();
            List<String> keys = new ArrayList<>();
            keys.add(SELLER_DETAIL_CACHE_PREFIX + id);
            if (seller != null && seller.getUserId() != null) {
                keys.add(SELLER_DETAIL_CACHE_PREFIX + "userId:" + seller.getUserId());
            }
            mqProducer.sendCacheRefreshTask("seller", "delete", keys);
        }
        return result;
    }

    /**
     * 发送卖家缓存刷新任务（异步，通过 MQ）
     * 1.清除卖家列表缓存
     * <p>
     * @author ZuiM
     */
    private void sendSellerCacheRefreshTask() {
        List<String> keys = new ArrayList<>();
        keys.add(SELLER_LIST_CACHE_KEY);
        mqProducer.sendCacheRefreshTask("seller", "clear", keys);
    }

    /**
     * 重新加载卖家详情缓存
     * <p>
     * @author ZuiM
     * @param id 卖家ID
     */
    private void reloadSellerDetail(Long id) {
        Seller seller = sellerMapper.findSellerById(id);
        if (seller != null) {
            redisUtil.set(SELLER_DETAIL_CACHE_PREFIX + id, seller);
        }
    }
}
