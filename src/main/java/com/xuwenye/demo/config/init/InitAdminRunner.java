package com.xuwenye.demo.config.init;

import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/**
 * 启动时初始化管理员与测试账号
 * 1.读取配置开关 examStatus
 * 2.若开启：创建管理员 admin / 123456
 * 3.批量创建 1000 个测试账号 tester_1 ~ tester_1000
 * <p>
 * @author ZuiM
 */
@Component      // 标记为组件，Spring启动时会自动执行
public class InitAdminRunner implements CommandLineRunner {
    @Value("${test.username.status:false}")
    private Boolean examStatus;
    @Value("${test.initEmailName}")
    private String testInitEmailName;

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    public InitAdminRunner(UserService userService,
                           PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 项目启动后执行初始化（CommandLineRunner 回调）
     * 1.打印配置开关状态
     * 2.创建管理员账号（不存在时）
     * 3.批量创建测试账号（不存在时，防止重启重复创建）
     * <p>
     * @author ZuiM
     * @param args 启动参数
     */
    @Override
    public void run(String... args) {
        System.out.println("【调试】examStatus = " + examStatus);

        // 测试环境开启，创建管理员+批量创建1000个测试账号
        if (examStatus) {
            // 检查是否有用户
            System.out.println("===== 开始创建管理员admin =====");
            if (userService.findAllUser("admin") == null) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("123456"));  // 加密存储
                admin.setNickname("超级管理员");
                admin.setStatus(1);
                admin.setUserRole("ROLE_ADMIN");
                admin.setEmail(testInitEmailName);
                admin.setCreateTime(LocalDateTime.now());
                userService.saveUser(admin);
            }
            System.out.println("✅ 初始化管理员账号：admin / 123456");

            System.out.println("===== 开始批量创建1000个测试账号 =====");
            for (int i = 1; i <= 1000; i++) {
                // 用户名 admin_1、admin_2 ... admin_1000，避免唯一索引冲突
                String username = "tester_" + i;
                // 判断账号不存在再新增，防止重启项目重复创建
                if (userService.findAllUser(username) == null) {
                    User testUser = new User();
                    testUser.setUsername(username);
                    testUser.setPassword(passwordEncoder.encode("123456"));
                    testUser.setNickname("测试用户" + i);
                    testUser.setStatus(1);
                    testUser.setCreateTime(LocalDateTime.now());
//                        testUser.setEmail(testInitEmailName);
                    userService.saveUser(testUser);
                    if (i % 100 == 0) {
                        System.out.println("已创建：" + i + " 个测试账号");
                    }
                }
            }
            System.out.println("===== 1000个测试账号初始化完成 =====");
        }
    }
}