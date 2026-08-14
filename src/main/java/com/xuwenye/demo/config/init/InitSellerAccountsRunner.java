package com.xuwenye.demo.config.init;

import com.xuwenye.demo.Entity.Seller;
import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Mapper.SellerMapper;
import com.xuwenye.demo.Service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 启动时自动为卖家创建登录账号
 * <p>
 * 背景：sys_seller 表与 sys_user 表之前未关联，卖家（商家）无法登录系统。
 * 本启动器通过「店铺名 = 用户名」的约定建立一对一关联：
 * 1.扫描 sys_seller 表全部未删除卖家
 * 2.为每个卖家在 sys_user 表创建对应登录账号（用户名=店铺名称，默认密码 Seller@123）
 * 3.已有同名账号则跳过（防止重启重复创建）
 * <p>
 * 登录打通说明：
 * - 卖家登录后，SellerOrderController.validateSeller() 用登录用户名
 *   调用 sellerService.getSellerBySellerName(username) 反查卖家ID，
 *   因此用户名必须与店铺名称一致，本启动器正是按此约定创建。
 * - 商品归属校验（orderContainsSeller）、店铺信息等接口随之自动生效。
 * <p>
 * @author ZuiM
 */
@Component      // 标记为组件，Spring 启动时会自动执行
public class InitSellerAccountsRunner implements CommandLineRunner {

    /** 卖家默认登录密码（统一初始密码，首次登录后可自行修改） */
    private static final String DEFAULT_SELLER_PASSWORD = "Seller@123";

    private final SellerMapper sellerMapper;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public InitSellerAccountsRunner(SellerMapper sellerMapper,
                                    UserService userService,
                                    PasswordEncoder passwordEncoder) {
        this.sellerMapper = sellerMapper;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 项目启动后执行初始化（CommandLineRunner 回调）
     * 1.直接查库获取全部未删除卖家（不走 Redis 缓存，保证拿到最新店铺数据）
     * 2.逐卖家创建 sys_user 登录账号
     * 3.打印创建结果
     * <p>
     * @author ZuiM
     * @param args 启动参数
     */
    @Override
    public void run(String... args) {
        System.out.println("===== 开始为卖家创建登录账号 =====");

        // 直接查库，避免启动阶段读到 Redis 中的旧缓存卖家数据
        List<Seller> sellers = sellerMapper.findAllSellers();
        if (sellers == null || sellers.isEmpty()) {
            System.out.println("⚠️ 暂无卖家数据，跳过卖家账号初始化");
            return;
        }

        int created = 0;   // 本次新建账号数
        int skipped = 0;   // 已存在账号数（防止重复创建）
        for (Seller seller : sellers) {
            String username = seller.getSellerName();
            // 用户名（店铺名）为空则跳过，避免脏数据
            if (username == null || username.trim().isEmpty()) {
                System.out.println("⚠️ 卖家ID=" + seller.getId() + " 店铺名称为空，跳过");
                continue;
            }
            username = username.trim();

            // 账号已存在（含逻辑删除）则跳过，防止唯一索引冲突与重复创建
            if (userService.findAllUser(username) != null) {
                skipped++;
                continue;
            }

            User sellerAccount = new User();
            sellerAccount.setUsername(username);
            sellerAccount.setPassword(passwordEncoder.encode(DEFAULT_SELLER_PASSWORD)); // BCrypt 加密存储
            sellerAccount.setNickname(username);
            sellerAccount.setStatus(1);
            sellerAccount.setUserRole("ROLE_SELLER");
            sellerAccount.setCreateTime(LocalDateTime.now());
            userService.saveUser(sellerAccount);
            created++;
        }

        System.out.println("✅ 卖家账号初始化完成：共 " + sellers.size() + " 个卖家，新建 " + created
                + " 个，已存在跳过 " + skipped + " 个");
        System.out.println("    默认登录：账号=店铺名称，密码=" + DEFAULT_SELLER_PASSWORD);
    }
}
