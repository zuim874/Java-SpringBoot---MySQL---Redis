package com.xuwenye.demo.service;

import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Entity.UserAddress;
import com.xuwenye.demo.Mapper.UserAddressMapper;
import com.xuwenye.demo.Service.UserAddressService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户地址业务层集成测试（需 MySQL + Redis + ActiveMQ）
 * 1.地址 CRUD：新增/查询/更新/逻辑删除
 * 2.默认地址：设置/自动取消旧默认地址
 * 3.Redis 缓存：Cache-Aside 模式的地址列表/默认地址缓存
 * 4.归属校验：非本人地址不可删除
 * <p>
 * @author ZuiM
 */
class UserAddressServiceTest extends AbstractServiceTest {

    @Autowired
    private UserAddressService addressService;

    @Autowired
    private UserAddressMapper addressMapper;

    // ========== 测试数据构造 ==========

    /**
     * 构造测试用户（用于地址归属）
     * <p>
     * @author ZuiM
     * @return User 用户
     */
    private User buildUser() {
        User user = new User();
        user.setUsername(uniqueName("addr_user"));
        user.setPassword("$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa");
        user.setNickname("地址测试用户" + SUFFIX);
        user.setEmail(uniqueName("addr") + "@test.com");
        user.setStatus(1);
        user.setUserRole("ROLE_USER");
        user.setBalance(BigDecimal.ZERO);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    /**
     * 构造测试地址
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     * @param isDefault 是否默认地址 0/1
     * @return UserAddress 地址
     */
    private UserAddress buildAddress(Long userId, int isDefault) {
        UserAddress addr = new UserAddress();
        addr.setUserId(userId);
        addr.setReceiverName("张三");
        addr.setReceiverPhone("13800000001");
        addr.setReceiverAddress("测试地址" + SUFFIX);
        addr.setIsDefault(isDefault);
        addr.setCreateTime(LocalDateTime.now());
        return addr;
    }

    /**
     * 清理测试数据
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     */
    private void cleanUser(Long userId) {
        if (userId != null) {
            trackCacheKey("demo:address:user:" + userId + ":*");
            userMapper.deleteById(userId);
        }
    }

    // ========== 1. 新增与查询 ==========

    /**
     * 新增地址后列表查询包含该地址（含 Redis 缓存写入）
     * <p>
     * @author ZuiM
     */
    @Test
    void 新增地址后列表可查() {
        User user = buildUser();
        UserAddress addr = buildAddress(user.getId(), 0);
        assertTrue(addressService.addAddress(addr));
        assertNotNull(addr.getId());

        var list = addressService.getUserAddresses(user.getId());
        assertTrue(list.stream().anyMatch(a -> a.getId().equals(addr.getId())));

        trackCacheKey("demo:address:user:" + user.getId());
        cleanUser(user.getId());
    }

    /**
     * 查询默认地址（无默认地址时返回 null）
     * <p>
     * @author ZuiM
     */
    @Test
    void 查询默认地址() {
        User user = buildUser();
        assertNull(addressService.getDefaultAddress(user.getId()));

        // 新增默认地址后可查到
        UserAddress addr = buildAddress(user.getId(), 1);
        addressService.addAddress(addr);
        assertNotNull(addressService.getDefaultAddress(user.getId()));

        trackCacheKey("demo:address:user:" + user.getId() + ":default");
        cleanUser(user.getId());
    }

    // ========== 2. 默认地址 ==========

    /**
     * 设置默认地址后旧的默认地址自动取消
     * <p>
     * @author ZuiM
     */
    @Test
    void 设置默认地址自动取消旧默认() {
        User user = buildUser();
        UserAddress addr1 = buildAddress(user.getId(), 1);
        addressService.addAddress(addr1);

        UserAddress addr2 = buildAddress(user.getId(), 0);
        addressService.addAddress(addr2);

        // 将 addr2 设为默认
        assertTrue(addressService.setDefaultAddress(addr2.getId(), user.getId()));

        // addr1 不再是默认
        UserAddress reloaded1 = addressMapper.selectById(addr1.getId());
        assertEquals(0, reloaded1.getIsDefault());
        // addr2 是默认
        UserAddress reloaded2 = addressMapper.selectById(addr2.getId());
        assertEquals(1, reloaded2.getIsDefault());

        trackCacheKey("demo:address:user:" + user.getId());
        trackCacheKey("demo:address:user:" + user.getId() + ":default");
        cleanUser(user.getId());
    }

    // ========== 3. 更新与删除 ==========

    /**
     * 更新地址信息（收件人/电话变更）
     * <p>
     * @author ZuiM
     */
    @Test
    void 更新地址信息() {
        User user = buildUser();
        UserAddress addr = buildAddress(user.getId(), 0);
        addressService.addAddress(addr);

        addr.setReceiverName("李四");
        addr.setReceiverPhone("13900000001");
        assertTrue(addressService.updateAddress(addr));
        UserAddress updated = addressMapper.selectById(addr.getId());
        assertEquals("李四", updated.getReceiverName());
        assertEquals("13900000001", updated.getReceiverPhone());

        trackCacheKey("demo:address:user:" + user.getId());
        cleanUser(user.getId());
    }

    /**
     * 逻辑删除地址后列表不可见
     * <p>
     * @author ZuiM
     */
    @Test
    void 逻辑删除地址() {
        User user = buildUser();
        UserAddress addr = buildAddress(user.getId(), 0);
        addressService.addAddress(addr);

        assertTrue(addressService.deleteAddress(addr.getId(), user.getId()));
        assertNull(addressMapper.selectById(addr.getId())); // @TableLogic 过滤

        trackCacheKey("demo:address:user:" + user.getId());
        cleanUser(user.getId());
    }

    /**
     * 非本人地址不可删除（归属校验）
     * <p>
     * @author ZuiM
     */
    @Test
    void 非本人地址不可删除() {
        User user1 = buildUser();
        User user2 = buildUser();
        UserAddress addr = buildAddress(user1.getId(), 0);
        addressService.addAddress(addr);

        // user2 删除 user1 的地址应抛出异常
        assertThrows(IllegalArgumentException.class,
                () -> addressService.deleteAddress(addr.getId(), user2.getId()));

        trackCacheKey("demo:address:user:" + user1.getId());
        trackCacheKey("demo:address:user:" + user2.getId());
        cleanUser(user1.getId());
        cleanUser(user2.getId());
    }
}