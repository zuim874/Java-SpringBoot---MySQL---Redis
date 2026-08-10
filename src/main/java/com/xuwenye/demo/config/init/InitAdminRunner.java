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

//package com.xuwenye.demo.config.init;
//
//import com.xuwenye.demo.Entity.User;
//import com.xuwenye.demo.Service.UserService;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDateTime;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicInteger;
//
///**
// * 启动时初始化管理员与测试账号（多线程优化版）
// * 1. 读取配置开关 examStatus
// * 2. 若开启：创建管理员 admin / 123456
// * 3. 批量创建 100000 个测试账号 tester_1 ~ tester_100000（多线程）
// *
// * @author ZuiM
// */
//@Component
//public class InitAdminRunner implements CommandLineRunner {
//
//    @Value("${test.username.status:false}")
//    private Boolean examStatus;
//
//    @Value("${test.initEmailName}")
//    private String testInitEmailName;
//
//    private final UserService userService;
//    private final PasswordEncoder passwordEncoder;
//
//    public InitAdminRunner(UserService userService,
//                           PasswordEncoder passwordEncoder) {
//        this.userService = userService;
//        this.passwordEncoder = passwordEncoder;
//    }
//
//    @Override
//    public void run(String... args) {
//        System.out.println("【调试】examStatus = " + examStatus);
//
//        if (!examStatus) {
//            System.out.println("ℹ️ 测试账号初始化已关闭（examStatus=false）");
//            return;
//        }
//
//        long startTime = System.currentTimeMillis();
//
//        // 1. 创建管理员
//        createAdmin();
//
//        // 2. 多线程批量创建测试账号
//        int totalCount = 100000;
//        createUsersMultiThread(totalCount);
//
//        long endTime = System.currentTimeMillis();
//        System.out.println("✅ 全部初始化完成，总耗时: " + (endTime - startTime) / 1000 + "秒");
//    }
//
//    /**
//     * 创建管理员账号
//     */
//    private void createAdmin() {
//        System.out.println("===== 开始创建管理员admin =====");
//        if (userService.findAllUser("admin") == null) {
//            User admin = new User();
//            admin.setUsername("admin");
//            admin.setPassword(passwordEncoder.encode("123456"));
//            admin.setNickname("超级管理员");
//            admin.setStatus(1);
//            admin.setUserRole("ROLE_ADMIN");
//            admin.setEmail(testInitEmailName);
//            admin.setCreateTime(LocalDateTime.now());
//            userService.saveUser(admin);
//        }
//        System.out.println("✅ 初始化管理员账号：admin / 123456");
//    }
//
//    /**
//     * 多线程批量创建用户（只改了这个方法）
//     *
//     * @param totalCount 总创建数量
//     */
//    private void createUsersMultiThread(int totalCount) {
//        System.out.println("===== 开始批量创建 " + totalCount + " 个测试账号（多线程） =====");
//
//        // 线程池大小：CPU核心数 × 2，至少8个
//        int threadPoolSize = Math.max(Runtime.getRuntime().availableProcessors() * 2, 8);
//        System.out.println("📊 线程池大小: " + threadPoolSize);
//
//        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
//        AtomicInteger createdCount = new AtomicInteger(0);
//        AtomicInteger skippedCount = new AtomicInteger(0);
//
//        // 每个线程分配的任务数
//        int batchSize = totalCount / threadPoolSize;
//        if (batchSize < 1) {
//            batchSize = 1;
//        }
//
//        long startTime = System.currentTimeMillis();
//
//        for (int t = 0; t < threadPoolSize; t++) {
//            int start = t * batchSize + 1;
//            int end = (t == threadPoolSize - 1) ? totalCount : (t + 1) * batchSize;
//
//            executor.submit(() -> {
//                for (int i = start; i <= end; i++) {
//                    String username = "tester_" + i;
//                    try {
//                        // 判断账号不存在再新增
//                        if (userService.findAllUser(username) == null) {
//                            User testUser = new User();
//                            testUser.setUsername(username);
//                            testUser.setPassword(passwordEncoder.encode("123456"));
//                            testUser.setNickname("测试用户" + i);
//                            testUser.setStatus(1);
//                            testUser.setCreateTime(LocalDateTime.now());
//                            // testUser.setEmail(testInitEmailName);
//                            userService.saveUser(testUser);
//
//                            int current = createdCount.incrementAndGet();
//                            if (current % 1000 == 0) {
//                                System.out.println("✅ 已创建: " + current + " 个测试账号");
//                            }
//                        } else {
//                            skippedCount.incrementAndGet();
//                        }
//                    } catch (Exception e) {
//                        System.err.println("❌ 创建用户失败: " + username + ", 错误: " + e.getMessage());
//                    }
//                }
//            });
//        }
//
//        // 关闭线程池并等待完成
//        executor.shutdown();
//        try {
//            if (!executor.awaitTermination(30, TimeUnit.MINUTES)) {
//                executor.shutdownNow();
//            }
//        } catch (InterruptedException e) {
//            executor.shutdownNow();
//            Thread.currentThread().interrupt();
//        }
//
//        long endTime = System.currentTimeMillis();
//        System.out.println("✅ 批量创建完成！");
//        System.out.println("   ✅ 新建账号: " + createdCount.get() + " 个");
//        System.out.println("   ⏭️ 跳过已存在: " + skippedCount.get() + " 个");
//        System.out.println("   ⏱️ 创建耗时: " + (endTime - startTime) / 1000 + "秒");
//    }
//}