package com.xuwenye.demo.service;

import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Mapper.UserMapper;
import com.xuwenye.demo.Service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户业务层集成测试（需 MySQL + Redis + ActiveMQ）
 * 1.用户 CRUD：保存/查询/更新/删除/恢复
 * 2.Redis 缓存：Cache-Aside 模式的缓存写入与命中
 * 3.余额操作：增加/扣减（分布式锁 + 原子 SQL），余额不足拦截
 * 4.角色管理：增删角色并保证保留基础角色
 * <p>
 * @author ZuiM
 */
class UserServiceTest extends AbstractServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper mapper;

    // ========== 测试数据构造 ==========

    /**
     * 构造测试用户（用户名唯一）
     * <p>
     * @author ZuiM
     * @param balance 初始余额
     * @return User 用户
     */
    private User buildUser(BigDecimal balance) {
        User user = new User();
        user.setUsername(uniqueName("unittest_user"));
        user.setPassword("$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa"); // 预置 BCrypt 密文（明文 123456）
        user.setNickname("测试昵称" + SUFFIX);
        user.setEmail(uniqueName("ut") + "@test.com");
        user.setStatus(1);
        user.setUserRole("ROLE_USER");
        user.setBalance(balance);
        user.setCreateTime(LocalDateTime.now());
        return user;
    }

    /**
     * 清理测试用户（逻辑删除 + 清理缓存）
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     * @param username 用户名
     */
    private void cleanUser(Long userId, String username) {
        if (userId != null) {
            mapper.deleteById(userId);
        }
        trackCacheKey("demo:user:active:" + username);
        trackCacheKey("demo:user:all:" + username);
        trackCacheKey("demo:user:recover:" + username);
        trackCacheKey("demo:user:id:" + userId);
    }

    // ========== 1. 用户保存与查询 ==========

    /**
     * 保存用户成功，且能通过用户名查询到
     * <p>
     * @author ZuiM
     */
    @Test
    void 保存用户并可按用户名查询() {
        User user = buildUser(BigDecimal.ZERO);
        assertTrue(userService.saveUser(user));
        assertNotNull(user.getId());

        User found = userService.findUserableUser(user.getUsername());
        assertNotNull(found);
        assertEquals(user.getUsername(), found.getUsername());

        cleanUser(user.getId(), user.getUsername());
    }

    /**
     * 保存后命中 Redis 缓存（Cache-Aside：第二次查询不再查库）
     * <p>
     * @author ZuiM
     */
    @Test
    void 查询命中redis缓存() {
        User user = buildUser(BigDecimal.ZERO);
        userService.saveUser(user);

        // 第一次查询写入缓存
        userService.findUserableUser(user.getUsername());
        trackCacheKey("demo:user:active:" + user.getUsername());
        // 第二次查询应命中缓存
        assertTrue(redisUtil.hasKey("demo:user:active:" + user.getUsername()));

        cleanUser(user.getId(), user.getUsername());
    }

    /**
     * 按 ID / 邮箱查询
     * <p>
     * @author ZuiM
     */
    @Test
    void 按id和邮箱查询() {
        User user = buildUser(BigDecimal.ZERO);
        userService.saveUser(user);

        assertNotNull(userService.getUserById(user.getId()));
        assertTrue(userService.isEmailExist(user.getEmail()));

        cleanUser(user.getId(), user.getUsername());
    }

    // ========== 2. 用户更新 ==========

    /**
     * 更新资料：修改昵称后能查询到新昵称
     * <p>
     * @author ZuiM
     */
    @Test
    void 更新用户资料() {
        User user = buildUser(BigDecimal.ZERO);
        userService.saveUser(user);

        String newNickname = "新昵称" + SUFFIX;
        assertTrue(userService.updateProfile(user.getId(), newNickname, null));
        User updated = userService.getUserById(user.getId());
        assertEquals(newNickname, updated.getNickname());

        cleanUser(user.getId(), user.getUsername());
    }

    /**
     * 更新密码后数据库中密码已变更
     * <p>
     * @author ZuiM
     */
    @Test
    void 更新用户密码() {
        User user = buildUser(BigDecimal.ZERO);
        userService.saveUser(user);

        String newPassword = "$2a$10$ABCDEFGHIJKLMNOPQRSTUV";
        assertTrue(userService.updatePassword(user.getId(), newPassword));
        User updated = mapper.selectById(user.getId());
        assertEquals(newPassword, updated.getPassword());

        cleanUser(user.getId(), user.getUsername());
    }

    /**
     * 更新头像
     * <p>
     * @author ZuiM
     */
    @Test
    void 更新用户头像() {
        User user = buildUser(BigDecimal.ZERO);
        userService.saveUser(user);

        String avatarUrl = "http://test.com/avatar_" + SUFFIX + ".png";
        assertTrue(userService.updateAvatar(user.getId(), avatarUrl));
        User updated = mapper.selectById(user.getId());
        assertEquals(avatarUrl, updated.getAvatar());

        cleanUser(user.getId(), user.getUsername());
    }

    // ========== 3. 余额操作 ==========

    /**
     * 余额扣减与增加
     * <p>
     * @author ZuiM
     */
    @Test
    void 余额扣减与增加() {
        User user = buildUser(new BigDecimal("100.00"));
        userService.saveUser(user);

        // 扣减 30 → 70
        assertTrue(userService.deductBalance(user.getId(), new BigDecimal("30.00")));
        assertEquals(0, new BigDecimal("70.00").compareTo(mapper.selectById(user.getId()).getBalance()));

        // 增加 50 → 120
        assertTrue(userService.chargeBalance(user.getId(), new BigDecimal("50.00")));
        assertEquals(0, new BigDecimal("120.00").compareTo(mapper.selectById(user.getId()).getBalance()));

        cleanUser(user.getId(), user.getUsername());
    }

    /**
     * 余额不足时扣减失败（原子 SQL 防超扣）
     * <p>
     * @author ZuiM
     */
    @Test
    void 余额不足扣减失败() {
        User user = buildUser(new BigDecimal("50.00"));
        userService.saveUser(user);

        assertFalse(userService.deductBalance(user.getId(), new BigDecimal("100.00")));
        // 余额未被扣减
        assertEquals(0, new BigDecimal("50.00").compareTo(mapper.selectById(user.getId()).getBalance()));

        cleanUser(user.getId(), user.getUsername());
    }

    /**
     * 非法金额（负数/零）直接拒绝
     * <p>
     * @author ZuiM
     */
    @Test
    void 非法金额被拒绝() {
        User user = buildUser(new BigDecimal("50.00"));
        userService.saveUser(user);

        assertFalse(userService.deductBalance(user.getId(), BigDecimal.ZERO));
        assertFalse(userService.chargeBalance(user.getId(), new BigDecimal("-10.00")));

        cleanUser(user.getId(), user.getUsername());
    }

    // ========== 4. 角色管理 ==========

    /**
     * 新增/移除角色，并保证至少保留基础买家角色
     * <p>
     * @author ZuiM
     */
    @Test
    void 新增与移除角色() {
        User user = buildUser(BigDecimal.ZERO);
        userService.saveUser(user);

        // 新增会员角色
        assertTrue(userService.updateUserRole(user.getId(), "ROLE_VIP_USER", true));
        String afterAdd = mapper.selectById(user.getId()).getUserRole();
        assertTrue(afterAdd.contains("ROLE_VIP_USER"));
        assertTrue(afterAdd.contains("ROLE_USER"));

        // 移除会员角色
        assertTrue(userService.updateUserRole(user.getId(), "ROLE_VIP_USER", false));
        assertFalse(mapper.selectById(user.getId()).getUserRole().contains("ROLE_VIP_USER"));

        cleanUser(user.getId(), user.getUsername());
    }

    // ========== 5. 删除与恢复 ==========

    /**
     * 逻辑删除用户后不可用，恢复后重新可用
     * <p>
     * @author ZuiM
     */
    @Test
    void 逻辑删除与恢复用户() {
        User user = buildUser(BigDecimal.ZERO);
        userService.saveUser(user);

        // 删除（逻辑删除）
        assertTrue(userService.deleteUserById(user.getId()));
        // 已删除用户不可登录查询（is_deleted=1 被过滤）
        assertNull(mapper.findUseableUserByUsername(user.getUsername()));

        // 恢复
        assertTrue(userService.recoverUserById(user.getId()));
        assertNotNull(mapper.findUseableUserByUsername(user.getUsername()));

        cleanUser(user.getId(), user.getUsername());
    }

    // ========== 6. 分页查询 ==========

    /**
     * 分页查询用户列表（按创建时间倒序）
     * <p>
     * @author ZuiM
     */
    @Test
    void 分页查询用户列表() {
        var page = userService.getUserList(1, 10);
        assertNotNull(page);
        assertTrue(page.getRecords().size() <= 10);
    }
}
