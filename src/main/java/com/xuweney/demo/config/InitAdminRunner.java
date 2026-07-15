package com.xuweney.demo.config;

import com.xuweney.demo.Entity.User;
import com.xuweney.demo.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component      // 标记为组件，Spring启动时会自动执行
public class InitAdminRunner implements CommandLineRunner {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 检查是否有用户
        if (userService.findUsername("admin") == null) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123456"));  // 加密存储
            admin.setNickname("超级管理员");
            admin.setStatus(1);
            admin.setCreateTime(LocalDateTime.now());
            userService.save(admin);
            System.out.println("✅ 初始化管理员账号：admin / 123456");
        }
    }
}