package com.xuwenye.demo.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Time;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.xuwenye.demo.util.redis.RedisLockHelper;
import com.xuwenye.demo.util.redis.RedisUtil;

/**
 * 用户业务层
 * 1.查询：按用户名/昵称/状态/ID/邮箱，均带 Redis 缓存（Cache-Aside）
 * 2.写操作：新增/逻辑删除/恢复/更新头像/更新资料，写库后维护缓存
 * 3.权限：isAdmin 校验管理员角色
 * <p>
 * @author ZuiM
 */
@Service
@Slf4j
public class UserService {
    private final UserMapper userMapper;
    private final RedisUtil redisUtil;
    private final RedisLockHelper redisLockHelper;
    private final MQProducer mqProducer;
    public UserService(UserMapper userMapper,
                       RedisUtil redisUtil,
                       RedisLockHelper redisLockHelper,
                       MQProducer mqProducer) {
        this.userMapper = userMapper;
        this.redisUtil = redisUtil;
        this.redisLockHelper = redisLockHelper;
        this.mqProducer = mqProducer;
    }

    /**
     * 根据用户名查询用户（登录用，含 Redis 缓存）
     * 1.先从 Redis 查（demo:user:active:{username}）
     * 2.未命中则查 MySQL
     * 3.查到了写入 Redis（过期 redisTimeOut 分钟）
     * <p>
     * @author ZuiM
     * @param username 用户名
     * @return User 用户（可能为 null）
     */
    //根据用户名查询用户（登录用，含 Redis 缓存）
    public User findUserableUser(String username) {
        // Redis key 命名规则：项目名:模块:业务标识
        String cacheKey = "demo:user:active:" + username;
        // 第 1 步：先从 Redis 查，有则直接返回
        User cached = (User) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // 第 2 步：Redis 没有，查 MySQL
        User user = userMapper.findUseableUserByUsername(username);
        // 第 3 步：查到了就写入 Redis（10 分钟过期，避免数据长期不一致）
        if (user != null) {
            redisUtil.set(cacheKey, user);
        }
        return user;
    }

    /**
     * 根据用户名查询用户（包含已逻辑删除的，含 Redis 缓存）
     * 1.先从 Redis 查（demo:user:all:{username}）
     * 2.未命中则查 MySQL（findAllUserByUsername）
     * 3.查到了写入 Redis
     * <p>
     * @author ZuiM
     * @param username 用户名
     * @return User 用户（可能为 null）
     */
    //根据用户名查询用户（包含已逻辑删除的）
    public User findAllUser(String username) {
        // Redis key 命名规则：项目名:模块:业务标识
        String cacheKey = "demo:user:all:" + username;
        // redis查询
        User cached = (User) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // redis没有，查mysql
        User user = userMapper.findAllUserByUsername(username);
        // 写入redis(10分钟过期)
        if (user != null) {
            redisUtil.set(cacheKey, user);
        }
        return user;
    }

    /**
     * 查询已逻辑删除的用户（用于账号恢复，含 Redis 缓存）
     * 1.先从 Redis 查（demo:user:recover:{username}）
     * 2.未命中则查 MySQL（findDeletedUserByUsername）
     * 3.查到了写入 Redis
     * <p>
     * @author ZuiM
     * @param username 用户名
     * @return User 已删除用户（可能为 null）
     */
    //查询删除用户
    public User findDeletedUserByUsername(String username) {
        String cacheKey = "demo:user:recover:" + username;
        // redis查询
        User cached = (User) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // redis没有，查mysql
        User user = userMapper.findDeletedUserByUsername(username);
        // 写入redis(10分钟过期)
        if (user != null) {
            redisUtil.set(cacheKey, user);
        }
        return user;
    }

    /**
     * 根据昵称查询用户列表（含 Redis 缓存）
     * 1.先从 Redis 查（demo:user:nickname:{nickname}）
     * 2.未命中则查 MySQL
     * 3.非空列表写入 Redis（列表缓存允许短暂不一致）
     * <p>
     * @author ZuiM
     * @param nickname 昵称
     * @return List&lt;User&gt; 用户列表
     */
    //根据昵称查询用户（含 Redis 缓存）
    @SuppressWarnings("unchecked")
    public List<User> findNickname(String nickname) {
        String cacheKey = "demo:user:nickname:" + nickname;
        // 第 1 步：先从 Redis 查
        List<User> cached = (List<User>) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // 第 2 步：Redis 没有，查 MySQL
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("nickname", nickname);
        List<User> list = userMapper.selectList(wrapper);
        // 第 3 步：写入 Redis（5 分钟过期，列表缓存允许短暂不一致）
        if (list != null && !list.isEmpty()) {
            redisUtil.set(cacheKey, list);
        }
        return list;
    }

    /**
     * 根据用户状态查询用户列表（含 Redis 缓存）
     * 1.先从 Redis 查（demo:user:status:{status}）
     * 2.未命中则查 MySQL
     * 3.非空列表写入 Redis
     * <p>
     * @author ZuiM
     * @param status 用户状态（0 禁用 / 1 启用）
     * @return List&lt;User&gt; 用户列表
     */
    //根据用户状态查询用户（含 Redis 缓存）
    @SuppressWarnings("unchecked")
    public List<User> findStatus(String status) {
        String cacheKey = "demo:user:status:" + status;
        // 第 1 步：先从 Redis 查
        List<User> cached = (List<User>) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // 第 2 步：Redis 没有，查 MySQL
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("status", status);
        List<User> list = userMapper.selectList(wrapper);
        // 第 3 步：写入 Redis（5 分钟过期）
        if (list != null && !list.isEmpty()) {
            redisUtil.set(cacheKey, list);
        }
        return list;
    }

    /**
     * 根据用户 ID 查询用户（含 Redis 缓存）
     * 1.先从 Redis 查（demo:user:id:{id}）
     * 2.未命中则查 MySQL
     * 3.查到了写入 Redis
     * <p>
     * @author ZuiM
     * @param id 用户 ID
     * @return User 用户（可能为 null）
     */
    //根据用户ID查询用户（含 Redis 缓存）
    public User getUserById(Long id) {
        // Redis key 命名规则：项目名:模块:业务标识
        String cacheKey = "demo:user:id:" + id;
        // 第 1 步：先从 Redis 查
        User cached = (User) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // 第 2 步：Redis 没有，查 MySQL
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id);
        User user = userMapper.selectOne(wrapper);
        // 第 3 步：查到了就写入 Redis（10 分钟过期）
        if (user != null) {
            redisUtil.set(cacheKey, user);
        }
        return user;
    }

    /**
     * 检查邮箱是否已存在（含 Redis 缓存）
     * 1.先从 Redis 查（demo:user:email:{email}）
     * 2.未命中则查 MySQL
     * 3.存在则写入 Redis 并返回 true
     * <p>
     * @author ZuiM
     * @param email 邮箱
     * @return boolean true=邮箱已被占用
     */
    //检查邮箱是否已存在（含 Redis 缓存）
    public boolean isEmailExist(String email) {
        String cacheKey = "demo:user:email:" + email;
        // 第 1 步：先从 Redis 查
        User cached = (User) redisUtil.get(cacheKey);
        if (cached != null) {
            return true;
        }
        // 第 2 步：Redis 没有，查 MySQL
        User user = userMapper.findUserByUserEmail(email);
        // 第 3 步：存在则写入 Redis（10 分钟过期）
        if (user != null) {
            redisUtil.set(cacheKey, user);
            return true;
        }
        return false;
    }

    /**
     * 保存用户（插入成功后才写缓存）
     * 1.插入数据库
     * 2.插入成功才写入 Redis（demo:user:active:{username}），避免无效数据进缓存
     * <p>
     * @author ZuiM
     * @param user 用户实体
     * @return boolean true=保存成功
     */
    //保存用户（插入成功后才写入缓存）
    public boolean saveUser(User user) {
        String username = user.getUsername();
        // Redis key 命名规则：项目名:模块:业务标识
        String cacheKey = "demo:user:active:" + username;
        boolean result = userMapper.insert(user) > 0;
        // 插入成功才写缓存，避免无效数据进入 Redis
        if (result) {
            redisUtil.set(cacheKey, user);
        }
        return result;
    }

    /**
     * 逻辑删除用户（同时清理所有关联缓存）
     * 1.先查出用户信息（用于删除缓存）
     * 2.执行逻辑删除
     * 3.清理该用户所有维度缓存（active/all/recover/id/email）
     * <p>
     * @author ZuiM
     * @param id 用户 ID
     * @return boolean true=删除成功
     */
    //删除用户(逻辑删除，同时清理所有关联缓存)
    public boolean deleteUserById(Long id) {
        // 先查出用户信息，用于删除缓存
        User user = userMapper.selectById(id);
        boolean result = userMapper.deleteById(id) > 0;
        if (result && user != null) {
            // 异步发送缓存刷新任务
            sendUserCacheRefreshTask(user);
        }
        return result;
    }

    /**
     * 恢复逻辑删除的用户（根据 ID，恢复后刷新缓存）
     * 1.执行恢复 SQL（is_deleted 置 0）
     * 2.清除旧的关联缓存（recover/all/id 中的数据已过时）
     * 3.重新写入 active 缓存（用户已可登录）
     * <p>
     * @author ZuiM
     * @param id 用户 ID
     * @return boolean true=恢复成功
     */
    //恢复用户(根据ID，恢复后刷新缓存)
    public boolean recoverUserById(Long id) {
        boolean result = userMapper.recoverDeletedUserByUserId(id) > 0;
        if (result) {
            User user = userMapper.selectById(id);
            if (user != null) {
                // 异步发送缓存刷新任务
                sendUserCacheRefreshTask(user);
                // 恢复后重新写入 active 缓存（用户已可登录，同步写入避免延迟）
                String cacheKey = "demo:user:active:" + user.getUsername();
                redisUtil.set(cacheKey, user);
            }
        }
        return result;
    }

    /**
     * 更新用户头像（更新数据库并清理关联缓存，下次查询重新加载）
     * 1.组装更新对象并执行 updateById
     * 2.更新成功后清理 active / all / id 缓存
     * <p>
     * @author ZuiM
     * @param id 用户 ID
     * @param avatarUrl 新头像 URL
     * @return boolean true=更新成功
     */
    //更新用户头像（更新数据库并清理关联缓存，下次查询重新加载）
    public boolean updateAvatar(Long id, String avatarUrl) {
        User update = new User();
        update.setId(id);
        update.setAvatar(avatarUrl);
        boolean result = userMapper.updateById(update) > 0;
        if (result) {
            // 清理该用户所有维度缓存，保证下次查询拿到最新头像
            User user = userMapper.selectById(id);
            if (user != null) {
                // 异步发送缓存刷新任务
                sendUserCacheRefreshTask(user);
            }
        }
        return result;
    }

    /**
     * 更新用户资料（昵称/换绑邮箱，更新数据库并清理关联缓存）
     * 1.更新前先查旧数据（用于清理旧邮箱维度缓存）
     * 2.组装更新对象（MyBatis-Plus 默认忽略 null 字段，不会误清已有值）
     * 3.执行 updateById 并清理 active / all / id / 新旧邮箱缓存
     * <p>
     * @author ZuiM
     * @param id 用户 ID
     * @param nickname 新昵称（为 null 时不更新）
     * @param newEmail 新邮箱（为 null 时不更新）
     * @return boolean true=更新成功
     */
    //更新用户资料（昵称/换绑邮箱，更新数据库并清理关联缓存）
    public boolean updateProfile(Long id, String nickname, String newEmail) {
        // 更新前先查旧数据（用于清理旧邮箱维度缓存）
        User old = userMapper.selectById(id);
        if (old == null) {
            return false;
        }
        // 组装更新对象（MyBatis-Plus 默认忽略 null 字段，不会误清已有值）
        User update = new User();
        update.setId(id);
        if (nickname != null) {
            update.setNickname(nickname);
        }
        if (newEmail != null) {
            update.setEmail(newEmail);
        }
        boolean result = userMapper.updateById(update) > 0;
        if (result) {
            // 异步发送缓存刷新任务，保证下次查询拿到最新资料
            // 注意：sendUserCacheRefreshTask 会自动清理 active/all/id/email 维度缓存
            sendUserCacheRefreshTask(old);
        }
        return result;
    }

    /**
     * 更新用户密码（updateById 更新，成功后清理关联缓存）
     * 1.组装更新对象（仅密码字段）并执行 updateById
     * 2.更新成功后清理 active / all / id 缓存
     * <p>
     * @author ZuiM
     * @param id 用户 ID
     * @param newPassword BCrypt 加密后的新密码
     * @return boolean true=更新成功
     */
    //更新用户密码（更新数据库并清理关联缓存，下次登录重新加载）
    public boolean updatePassword(Long id, String newPassword) {
        User update = new User();
        update.setId(id);
        update.setPassword(newPassword);
        boolean result = userMapper.updateById(update) > 0;
        if (result) {
            User user = userMapper.selectById(id);
            if (user != null) {
                // 异步发送缓存刷新任务
                sendUserCacheRefreshTask(user);
            }
        }
        return result;
    }

    public boolean updateLastLoginTime(Long id, LocalDateTime time) {
        User update = new User();
        update.setId(id);
        update.setLastLoginTime(time);
        boolean result = userMapper.updateById(update) > 0;
        if (result) {
            User user = userMapper.selectById(id);
            if (user != null) {
                // 异步发送缓存刷新任务
                sendUserCacheRefreshTask(user);
            }
        }
        return result;
    }

    /**
     * 检查权限（复用 findUser 的 Redis 缓存，避免额外数据库查询）
     * 1.查询用户（含缓存）
     * 2.判空、角色判空
     * 3.比较角色是否为 ROLE_ADMIN
     * <p>
     * @author ZuiM
     * @param username 用户名
     * @return boolean true=管理员
     */
    //检查权限（复用 findUsername 的 Redis 缓存，避免额外数据库查询）
    public boolean isAdmin(String username) {
        // 1. 查询用户（含 Redis 缓存）
        User user = userMapper.findUseableUserByUsername(username);
        // 2. 判空
        if (user == null) {
            return false;
        }
        // 3. 获取角色并判空
        String userRole = user.getUserRole();
        if (userRole == null) {
            return false;
        }
        // 4. 比较
        return "ROLE_ADMIN".equals(userRole);
    }

    /**
     * 分页查询用户列表
     * <p>
     * @author ZuiM
     * @param page 页码
     * @param size 每页条数
     * @return Page<User> 分页用户列表
     */
    public Page<User> getUserList(int page, int size) {
        Page<User> pageObj = new Page<>(page, size);
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time");
        return userMapper.selectPage(pageObj, wrapper);
    }

    /**
     * 更新用户状态（启用/禁用）
     * 1.更新数据库
     * 2.清除关联缓存
     * <p>
     * @author ZuiM
     * @param id 用户ID
     * @param status 0禁用 1启用
     * @return boolean true=更新成功
     */
    public boolean updateUserStatus(Long id, int status) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return false;
        }
        User update = new User();
        update.setId(id);
        update.setStatus(status);
        boolean result = userMapper.updateById(update) > 0;
        if (result) {
            sendUserCacheRefreshTask(user);
        }
        return result;
    }

    /**
     * 发送用户缓存刷新任务（异步，通过 MQ）
     * 1.清除 active/all/recover/id/email 维度缓存
     * <p>
     * @author ZuiM
     * @param user 用户实体
     */
    private void sendUserCacheRefreshTask(User user) {
        if (user == null) return;
        List<String> keys = new ArrayList<>();
        keys.add("demo:user:active:" + user.getUsername());
        keys.add("demo:user:all:" + user.getUsername());
        keys.add("demo:user:recover:" + user.getUsername());
        keys.add("demo:user:id:" + user.getId());
        if (user.getEmail() != null) {
            keys.add("demo:user:email:" + user.getEmail());
        }
        mqProducer.sendCacheRefreshTask("user", "clear", keys);
    }

    /**
     * 增加用户余额（管理员充值/退款回补，分布式锁 + 原子 SQL 双重保护）
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     * @param amount 增加金额
     * @return boolean true=成功
     */
    public boolean chargeBalance(Long userId, BigDecimal amount) {
        return changeBalance(userId, amount, false);
    }

    /**
     * 扣减用户余额（支付扣款，分布式锁 + 原子 SQL 防止超扣）
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     * @param amount 扣减金额
     * @return boolean true=成功（余额充足）
     */
    public boolean deductBalance(Long userId, BigDecimal amount) {
        return changeBalance(userId, amount, true);
    }

    /**
     * 变更余额（内部方法）
     * 1.分布式锁：同一用户余额操作串行，避免并发读写
     * 2.原子 SQL：扣减时余额充足才成功，防止超扣
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     * @param amount 变更金额（正数）
     * @param deduct true=扣减 false=增加
     * @return boolean true=成功
     */
    private boolean changeBalance(Long userId, BigDecimal amount, boolean deduct) {
        if (userId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        String lockKey = "demo:user:balance:" + userId;
        try {
            boolean locked = redisLockHelper.tryLock(lockKey, 3, TimeUnit.SECONDS);
            if (!locked) {
                log.error("获取余额锁失败，userId={}", userId);
                return false;
            }
            int rows = deduct
                    ? userMapper.deductBalance(userId, amount)
                    : userMapper.addBalance(userId, amount);
            if (rows > 0) {
                // 清缓存，下次查询读到最新余额
                User user = userMapper.selectById(userId);
                if (user != null) {
                    sendUserCacheRefreshTask(user);
                }
            }
            return rows > 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("余额操作被中断，userId={}", userId, e);
            return false;
        } catch (Exception e) {
            log.error("余额操作异常，userId={}", userId, e);
            return false;
        } finally {
            redisLockHelper.unlock(lockKey);
        }
    }
}
