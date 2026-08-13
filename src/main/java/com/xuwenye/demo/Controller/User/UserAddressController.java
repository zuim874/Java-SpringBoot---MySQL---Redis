package com.xuwenye.demo.Controller.User;

import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Entity.UserAddress;
import com.xuwenye.demo.Service.UserAddressService;
import com.xuwenye.demo.annotation.OperationLog;
import com.xuwenye.demo.annotation.RateLimit;
import com.xuwenye.demo.annotation.UserCheck;
import com.xuwenye.demo.common.Result;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户收货地址管理接口
 * 1.用户可管理自己的多个收货地址
 * 2.支持设置默认地址，下单时自动选中
 * 3.所有接口需登录
 * <p>
 * @author ZuiM
 */
@RestController
@RequestMapping("/api/user/address")
@Validated
public class UserAddressController {

    private final UserAddressService userAddressService;

    public UserAddressController(UserAddressService userAddressService) {
        this.userAddressService = userAddressService;
    }

    /**
     * 获取当前用户的所有地址
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @return Result<List<UserAddress>> 地址列表
     */
    @UserCheck
    @GetMapping("/list")
    @RateLimit(window = 60, maxRequests = 20)
    public Result<List<UserAddress>> getAddresses(User currentUser) {
        List<UserAddress> list = userAddressService.getUserAddresses(currentUser.getId());
        return Result.ok(list);
    }

    /**
     * 获取默认地址
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @return Result<UserAddress> 默认地址（可能为null）
     */
    @UserCheck
    @GetMapping("/default")
    @RateLimit(window = 60, maxRequests = 20)
    public Result<UserAddress> getDefaultAddress(User currentUser) {
        UserAddress addr = userAddressService.getDefaultAddress(currentUser.getId());
        return Result.ok(addr);
    }

    /**
     * 新增地址
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @param address 地址信息（除userId、id外）
     * @return Result<?>
     */
    @OperationLog("新增收货地址")
    @UserCheck
    @PostMapping("/add")
    @RateLimit(window = 60, maxRequests = 10, message = "操作过于频繁，请稍后再试")
    public Result<?> addAddress(User currentUser, @RequestBody UserAddress address) {
        if (address.getReceiverName() == null || address.getReceiverName().trim().isEmpty()) {
            return Result.error(400, "收货人姓名不能为空");
        }
        if (address.getReceiverPhone() == null || address.getReceiverPhone().trim().isEmpty()) {
            return Result.error(400, "收货人电话不能为空");
        }
        if (address.getReceiverAddress() == null || address.getReceiverAddress().trim().isEmpty()) {
            return Result.error(400, "收货地址不能为空");
        }

        address.setId(null);
        address.setUserId(currentUser.getId());
        boolean success = userAddressService.addAddress(address);
        return success ? Result.ok("地址添加成功") : Result.error(500, "添加失败");
    }

    /**
     * 更新地址
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @param address 地址信息（必须包含id）
     * @return Result<?>
     */
    @OperationLog("更新收货地址")
    @UserCheck
    @PutMapping("/update")
    @RateLimit(window = 60, maxRequests = 10, message = "操作过于频繁，请稍后再试")
    public Result<?> updateAddress(User currentUser, @RequestBody UserAddress address) {
        if (address.getId() == null) {
            return Result.error(400, "地址ID不能为空");
        }
        address.setUserId(currentUser.getId());
        try {
            boolean success = userAddressService.updateAddress(address);
            return success ? Result.ok("地址更新成功") : Result.error(500, "更新失败");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 删除地址
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @param id 地址ID
     * @return Result<?>
     */
    @OperationLog("删除收货地址")
    @UserCheck
    @DeleteMapping("/delete/{id}")
    @RateLimit(window = 60, maxRequests = 10, message = "操作过于频繁，请稍后再试")
    public Result<?> deleteAddress(User currentUser, @PathVariable Long id) {
        try {
            boolean success = userAddressService.deleteAddress(id, currentUser.getId());
            return success ? Result.ok("地址删除成功") : Result.error(500, "删除失败");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 设置默认地址
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @param id 地址ID
     * @return Result<?>
     */
    @OperationLog("设置默认地址")
    @UserCheck
    @PutMapping("/default/{id}")
    @RateLimit(window = 60, maxRequests = 10, message = "操作过于频繁，请稍后再试")
    public Result<?> setDefaultAddress(User currentUser, @PathVariable Long id) {
        try {
            boolean success = userAddressService.setDefaultAddress(id, currentUser.getId());
            return success ? Result.ok("默认地址设置成功") : Result.error(500, "设置失败");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }
}