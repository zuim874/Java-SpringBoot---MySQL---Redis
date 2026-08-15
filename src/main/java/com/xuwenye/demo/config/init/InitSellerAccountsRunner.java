package com.xuwenye.demo.config.init;

import com.xuwenye.demo.Entity.Seller;
import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Mapper.SellerMapper;
import com.xuwenye.demo.Service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动时为存量卖家补填登录账号绑定（user_id）
 * <p>
 * 背景：sys_seller 表通过 user_id 与 sys_user 表建立一对一绑定。
 * 新策略下「新增卖家」不再自动新建账号，而是由管理员绑定已存在的用户账号
 * （SellerService.saveSeller 处理）。本启动器仅兜底存量数据：
 * 1.扫描 sys_seller 表全部未删除、且未绑定账号（user_id IS NULL）的卖家
 * 2.若存在「用户名 = 店铺名称」的可用账号（兼容旧版「店铺名 = 用户名」自动创建的账号），
 *   直接回填 user_id 完成绑定，不重复新建
 * 3.无匹配账号则打印提示，等待管理员在后台用「用户名 + 邮箱」手动绑定
 * <p>
 * 说明：不再自动新建账号 —— 注册用户名已限定字母/数字（店铺名可为中文），
 * 按店铺名新建账号会生成非法用户名，故一律改为绑定已有账号。
 * <p>
 * @author ZuiM
 */
@Component      // 标记为组件，Spring 启动时会自动执行
public class InitSellerAccountsRunner implements CommandLineRunner {

    private final SellerMapper sellerMapper;
    private final UserService userService;

    public InitSellerAccountsRunner(SellerMapper sellerMapper,
                                    UserService userService) {
        this.sellerMapper = sellerMapper;
        this.userService = userService;
    }

    /**
     * 项目启动后执行初始化（CommandLineRunner 回调）
     * 1.直接查库获取全部未删除卖家（不走 Redis 缓存，保证拿到最新店铺数据）
     * 2.对未绑定账号的卖家，按「店铺名 = 用户名」匹配已有账号并回填 user_id
     * 3.无匹配账号则提示手动绑定
     * <p>
     * @author ZuiM
     * @param args 启动参数
     */
    @Override
    public void run(String... args) {
        System.out.println("===== 开始为存量卖家补填登录账号绑定 =====");

        // 直接查库，避免启动阶段读到 Redis 中的旧缓存卖家数据
        List<Seller> sellers = sellerMapper.findAllSellers();
        if (sellers == null || sellers.isEmpty()) {
            System.out.println("⚠️ 暂无卖家数据，跳过卖家账号初始化");
            return;
        }

        int bound = 0;   // 本次完成绑定（补填 user_id）的卖家数
        int skipped = 0; // 已绑定/名称缺失跳过的卖家数
        int unmatched = 0; // 无同名账号、需人工绑定的卖家数
        for (Seller seller : sellers) {
            // 已绑定账号的卖家直接跳过（幂等，防止重复处理）
            if (seller.getUserId() != null) {
                skipped++;
                continue;
            }
            String username = seller.getSellerName();
            // 用户名（店铺名）为空则跳过，避免脏数据
            if (username == null || username.trim().isEmpty()) {
                System.out.println("⚠️ 卖家ID=" + seller.getId() + " 店铺名称为空，跳过");
                skipped++;
                continue;
            }
            username = username.trim();

            // 兼容旧版「店铺名 = 用户名」约定：若存在该可用账号则直接绑定，不新建账号
            User account = userService.findUserableUser(username);
            if (account != null) {
                // 回填 user_id，完成「卖家 ↔ 用户」一对一绑定
                Seller update = new Seller();
                update.setId(seller.getId());
                update.setUserId(account.getId());
                sellerMapper.updateById(update);
                bound++;
            } else {
                // 无可用同名账号：记录，等待管理员用「用户名 + 邮箱」手动绑定
                unmatched++;
                System.out.println("⚠️ 卖家ID=" + seller.getId() + "（" + seller.getSellerName()
                        + "）未绑定登录账号，请在管理后台通过「用户名 + 邮箱」绑定已有账号");
            }
        }

        System.out.println("✅ 卖家账号初始化完成：共 " + sellers.size() + " 个卖家，本次补填绑定 " + bound
                + " 个，已绑定跳过 " + skipped + " 个，待人工绑定 " + unmatched + " 个");
        System.out.println("    备注：新增卖家请在管理后台填写「用户名 + 邮箱」绑定已有账号，不再自动新建账号");
    }
}
