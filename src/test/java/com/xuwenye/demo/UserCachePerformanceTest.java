package com.xuwenye.demo;

import com.xuwenye.demo.Service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@SpringBootTest
public class UserCachePerformanceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final List<String> testUsernames = new ArrayList<>();

    @BeforeEach
    public void init() {
        testUsernames.clear();
        for (int i = 1; i <= 1000; i++) {
            testUsernames.add("tester_" + i);
        }
    }

    @Test
    public void testCacheEffect() {
        String username = "admin";
        String cacheKey = "demo:user:login:" + username;

        redisTemplate.delete(cacheKey);
        System.out.println("========== 第1轮：冷查询（查MySQL）==========");
        long start1 = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            userService.findUsernameforlogin(username);
        }
        long time1 = System.currentTimeMillis() - start1;
        System.out.println("1000次查询耗时: " + time1 + "ms, 平均: " + (time1 / 1000.0) + "ms");

        System.out.println("\n========== 第2轮：热查询（查Redis）==========");
        long start2 = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            userService.findUsernameforlogin(username);
        }
        long time2 = System.currentTimeMillis() - start2;
        System.out.println("1000次查询耗时: " + time2 + "ms, 平均: " + (time2 / 1000.0) + "ms");

        double boost = time1 * 1.0 / (time2 == 0 ? 1 : time2);
        System.out.println("\n🚀 性能提升: " + String.format("%.1f", boost) + "倍");
    }

    @Test
    public void testConcurrentAccess() throws InterruptedException {
        int threadCount = 50;  // 50个并发线程
        int usersPerThread = 20;  // 每个线程登录20个不同用户

        System.out.println("========== 模拟高并发登录场景 ==========");
        System.out.println("并发线程数: " + threadCount);
        System.out.println("每线程登录用户数: " + usersPerThread);
        System.out.println("总登录次数: " + (threadCount * usersPerThread));
        System.out.println("使用测试用户: tester_1 ~ tester_1000\n");

        // 第1轮：清除所有缓存，模拟冷启动
        System.out.println("===== 第1轮：无缓存（全部查MySQL）=====");
        for (int i = 1; i <= 1000; i++) {
            redisTemplate.delete("demo:user:login:tester_" + i);
        }

        long time1 = runConcurrentTest(threadCount, usersPerThread);
        System.out.println("无缓存总耗时: " + time1 + "ms");
        System.out.println("平均每次登录: " + (time1 * 1.0 / (threadCount * usersPerThread)) + "ms\n");

        // 第2轮：预热缓存（让所有用户数据进入Redis）
        System.out.println("===== 预热缓存中... =====");
        for (int i = 1; i <= 1000; i++) {
            userService.findUsernameforlogin("tester_" + i);
        }
        System.out.println("缓存预热完成\n");

        // 第3轮：有缓存，模拟高并发登录
        System.out.println("===== 第2轮：有缓存（全部查Redis）=====");
        long time2 = runConcurrentTest(threadCount, usersPerThread);
        System.out.println("有缓存总耗时: " + time2 + "ms");
        System.out.println("平均每次登录: " + (time2 * 1.0 / (threadCount * usersPerThread)) + "ms\n");

        // 输出对比结果
        double boost = time1 * 1.0 / (time2 == 0 ? 1 : time2);
        System.out.println("========== 性能对比 ==========");
        System.out.println("无缓存耗时: " + time1 + "ms");
        System.out.println("有缓存耗时: " + time2 + "ms");
        System.out.println("🚀 性能提升: " + String.format("%.1f", boost) + "倍");
    }

    private long runConcurrentTest(int threadCount, int usersPerThread) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(threadCount);
        java.util.concurrent.atomic.AtomicLong totalTime = new java.util.concurrent.atomic.AtomicLong(0);

        long overallStart = System.currentTimeMillis();

        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        long start = System.currentTimeMillis();
                        // 每个线程登录不同的用户（模拟真实场景）
                        for (int j = 0; j < usersPerThread; j++) {
                            int userId = (threadId * usersPerThread + j) % 1000 + 1;
                            String username = "tester_" + userId;
                            userService.findUsernameforlogin(username);
                        }
                        totalTime.addAndGet(System.currentTimeMillis() - start);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
        }

        return System.currentTimeMillis() - overallStart;
    }

    @Test
    public void testRedisTemplate() {
        String testKey = "demo:test:hello";
        String testValue = "Hello Redis!";

        System.out.println("========== RedisTemplate 基础测试 ==========");
        redisTemplate.opsForValue().set(testKey, testValue, 60, TimeUnit.SECONDS);
        Object result = redisTemplate.opsForValue().get(testKey);
        System.out.println("写入值: " + testValue);
        System.out.println("读取值: " + result);
        System.out.println("测试结果: " + (testValue.equals(result) ? "✅ 成功" : "❌ 失败"));
        redisTemplate.delete(testKey);
    }
}