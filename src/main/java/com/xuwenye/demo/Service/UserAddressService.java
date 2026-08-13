package com.xuwenye.demo.Service;

import com.xuwenye.demo.Entity.UserAddress;
import com.xuwenye.demo.Mapper.UserAddressMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户收货地址业务层
 * 1.每个用户可维护多个收货地址
 * 2.支持设置默认地址（自动取消旧的默认地址）
 * 3.逻辑删除地址
 * <p>
 * @author ZuiM
 */
@Service
public class UserAddressService {

    private final UserAddressMapper userAddressMapper;

    public UserAddressService(UserAddressMapper userAddressMapper) {
        this.userAddressMapper = userAddressMapper;
    }

    /**
     * 获取用户的所有地址
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     * @return List<UserAddress> 地址列表
     */
    public List<UserAddress> getUserAddresses(Long userId) {
        return userAddressMapper.findAddressesByUserId(userId);
    }

    /**
     * 获取用户的默认地址
     * <p>
     * @author ZuiM
     * @param userId 用户ID
     * @return UserAddress 默认地址（可能为null）
     */
    public UserAddress getDefaultAddress(Long userId) {
        return userAddressMapper.findDefaultAddress(userId);
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
        return userAddressMapper.insert(address) > 0;
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
        return userAddressMapper.updateById(address) > 0;
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
        return userAddressMapper.deleteById(id) > 0;
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
        return userAddressMapper.updateById(address) > 0;
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
}