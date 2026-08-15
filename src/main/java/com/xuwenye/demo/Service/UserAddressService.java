package com.xuwenye.demo.Service;

import com.xuwenye.demo.Entity.UserAddress;
import com.xuwenye.demo.Mapper.UserAddressMapper;
import com.xuwenye.demo.util.redis.RedisUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户收货地址业务层
 * 1.每个用户可维护多个收货地址
 * 2.支持设置默认地址（自动取消旧的默认地址）
 * 3.逻辑删除地址
 * 查询均带 Redis 缓存（Cache-Aside），数据变更后通过 MQ 异步清理关联缓存
 * <p>
 * @author ZuiM
 */
@Service
public class UserAddressService {

    private final UserAddressMapper userAddressMapper;
    private final RedisUtil redisUtil;
    private final MQProducer mqProducer;

    // Redis缓存Key前缀
    private static final String ADDRESS_CACHE_PREFIX = "demo:address:user:";

    public UserAddressService(UserAddressMapper userAddressMapper,
                              RedisUtil redisUtil,
                              MQProducer mqProducer) {
        this.userAddressMapper = userAddressMapper;
        this.redisUtil = redisUtil;
        this.mqProducer = mqProducer;
    }

    /**
     * 获取用户的所有地址（Cache-Aside 缓存模式）
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     * @return List<UserAddress> 地址列表
     */
    @SuppressWarnings("unchecked")
    public List<UserAddress> getUserAddresses(Long userId) {
        String cacheKey = ADDRESS_CACHE_PREFIX + userId;
        // 第 1 步：先从 Redis 查
        List<UserAddress> cached = (List<UserAddress>) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // 第 2 步：Redis 没有，查 MySQL
        List<UserAddress> addresses = userAddressMapper.findAddressesByUserId(userId);
        // 第 3 步：写入 Redis
        if (addresses != null && !addresses.isEmpty()) {
            redisUtil.set(cacheKey, addresses);
        }
        return addresses;
    }

    /**
     * 获取用户的默认地址（Cache-Aside 缓存模式）
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     * @return UserAddress 默认地址（可能为null）
     */
    public UserAddress getDefaultAddress(Long userId) {
        String cacheKey = ADDRESS_CACHE_PREFIX + userId + ":default";
        // 第 1 步：先从 Redis 查
        UserAddress cached = (UserAddress) redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // 第 2 步：Redis 没有，查 MySQL
        UserAddress address = userAddressMapper.findDefaultAddress(userId);
        // 第 3 步：查到了就写入 Redis
        if (address != null) {
            redisUtil.set(cacheKey, address);
        }
        return address;
    }

    /**
     * 新增地址
     * 如果新增的地址是默认地址，则自动取消其他默认地址
     * <p>
     * @author ZuiM
     * @param address 地址实体
     * @return boolean true=新增成功
     */
    @Transactional
    public boolean addAddress(UserAddress address) {
        if (address.getIsDefault() == 1) {
            // 取消该用户其他默认地址
            cancelDefaultAddress(address.getUserId());
        }
        boolean result = userAddressMapper.insert(address) > 0;
        if (result) {
            // 新增后异步清理该用户地址缓存
            sendAddressCacheRefresh(address.getUserId());
        }
        return result;
    }

    /**
     * 更新地址
     * 如果更新的地址是默认地址，则自动取消其他默认地址
     * <p>
     * @author ZuiM
     * @param address 地址实体（必须包含id）
     * @return boolean true=更新成功
     */
    @Transactional
    public boolean updateAddress(UserAddress address) {
        if (address.getIsDefault() == 1) {
            // 取消该用户其他默认地址
            cancelDefaultAddress(address.getUserId());
        }
        boolean result = userAddressMapper.updateById(address) > 0;
        if (result) {
            // 更新后异步清理该用户地址缓存
            sendAddressCacheRefresh(address.getUserId());
        }
        return result;
    }

    /**
     * 删除地址（逻辑删除）
     * <p>
     * @author ZuiM
     * @param id 地址ID
     * @param userId 用户ID（归属校验）
     * @return boolean true=删除成功
     */
    @Transactional
    public boolean deleteAddress(Long id, Long userId) {
        UserAddress address = userAddressMapper.selectById(id);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new IllegalArgumentException("地址不存在或无权限删除");
        }
        boolean result = userAddressMapper.deleteById(id) > 0;
        if (result) {
            // 删除后异步清理该用户地址缓存
            sendAddressCacheRefresh(userId);
        }
        return result;
    }

    /**
     * 设置默认地址
     * <p>
     * @author ZuiM
     * @param id 地址ID
     * @param userId 用户ID（归属校验）
     * @return boolean true=设置成功
     */
    @Transactional
    public boolean setDefaultAddress(Long id, Long userId) {
        UserAddress address = userAddressMapper.selectById(id);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new IllegalArgumentException("地址不存在或无权限操作");
        }
        // 取消该用户所有默认地址
        cancelDefaultAddress(userId);
        // 设置新的默认地址
        address.setIsDefault(1);
        boolean result = userAddressMapper.updateById(address) > 0;
        if (result) {
            // 设置后异步清理该用户地址缓存
            sendAddressCacheRefresh(userId);
        }
        return result;
    }

    /**
     * 取消用户的所有默认地址标记
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     */
    private void cancelDefaultAddress(Long userId) {
        List<UserAddress> addresses = userAddressMapper.findAddressesByUserId(userId);
        for (UserAddress addr : addresses) {
            if (addr.getIsDefault() == 1) {
                addr.setIsDefault(0);
                userAddressMapper.updateById(addr);
            }
        }
    }

    // ======================== 缓存刷新工具方法 ========================

    /**
     * 发送「该用户地址」缓存刷新任务（增/改/删/设默认时调用）
     * 同时清理该用户的地址列表缓存与默认地址缓存
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     */
    private void sendAddressCacheRefresh(Long userId) {
        List<String> keys = new ArrayList<>();
        keys.add(ADDRESS_CACHE_PREFIX + userId + ":*");
        mqProducer.sendCacheRefreshTask("address", "clear", keys);
    }
}
