package com.xuweney.demo.config;

import com.xuweney.demo.Entity.User;
import com.xuweney.demo.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component      // 标记为组件，Spring启动时会自动执行
public class InitAdminRunner implements CommandLineRunner {
    @Value("${test.username.status:false}")
    private Boolean examStatus;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        System.out.println("【调试】examStatus = " + examStatus);

            // 测试环境开启，创建管理员+批量创建1000个测试账号
            if (examStatus) {
                // 检查是否有用户
                System.out.println("===== 开始创建管理员admin =====");
                if (userService.findUsername("admin") == null) {
                    User admin = new User();
                    admin.setUsername("admin");
                    admin.setPassword(passwordEncoder.encode("123456"));  // 加密存储
                    admin.setNickname("超级管理员");
                    admin.setStatus(1);
                    admin.setUser_role("ROLE_ADMIN");
                    admin.setCreate_time(LocalDateTime.now());
                    userService.save(admin);
                }
                System.out.println("✅ 初始化管理员账号：admin / 123456");

                System.out.println("===== 开始批量创建1000个测试账号 =====");
                for (int i = 1; i <= 1000; i++) {
                    // 用户名 admin_1、admin_2 ... admin_1000，避免唯一索引冲突
                    String username = "tester_" + i;
                    // 判断账号不存在再新增，防止重启项目重复创建
                    if (userService.findUsername(username) == null) {
                        User testUser = new User();
                        testUser.setUsername(username);
                        testUser.setPassword(passwordEncoder.encode("123456"));
                        testUser.setNickname("测试用户" + i);
                        testUser.setStatus(1);
                        testUser.setCreate_time(LocalDateTime.now());
                        userService.save(testUser);
                        if (i % 100 == 0) {
                            System.out.println("已创建：" + i + " 个测试账号");
                        }
                    }
                }
                System.out.println("===== 1000个测试账号初始化完成 =====");
            }
    }
}