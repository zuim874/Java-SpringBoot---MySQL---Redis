package com.xuwenye.demo;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
public class CacheAvalancheTest {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // ============ 配置参数 ============
    private static final int TOTAL_USERS = 100000;      // 总用户数
    private static final int EXPIRE_SECONDS = 5;        // 缓存过期时间（秒）
    private static final int CACHE_BATCH_SIZE = 1000;   // 缓存写入批次大小
    private static final int THREAD_COUNT = 200;        // 并发线程数
    private static final int QUERY_PER_THREAD = 500;    // 每线程查询数

    // ============ 准备缓存（优化：分批+进度显示） ============
    @Test
    public void prepareCache() throws InterruptedException {
        System.out.println("⏰ 开始存入缓存（" + EXPIRE_SECONDS + "秒过期）...");
        System.out.println("📊 总用户数: " + TOTAL_USERS);

        long startTime = System.currentTimeMillis();
        int count = 0;

        // 分批处理，每批 CACHE_BATCH_SIZE 条，显示进度
        for (long i = 1; i <= TOTAL_USERS; i++) {
            String cacheKey = "demo:test:user:id" + i;
            QueryWrapper<User> wrapper = new QueryWrapper<>();
            wrapper.eq("id", i);
            User user = userMapper.selectOne(wrapper);
            if (user != null) {
                redisTemplate.opsForValue().set(cacheKey, user, EXPIRE_SECONDS, TimeUnit.SECONDS);
                count++;
            }

            // 每1000条显示一次进度
            if (i % CACHE_BATCH_SIZE == 0) {
                System.out.println("  已处理: " + i + "/" + TOTAL_USERS + " (已缓存: " + count + ")");
            }
        }

        long endTime = System.currentTimeMillis();
        System.out.println("✅ 已存入 " + count + " 个缓存，" + EXPIRE_SECONDS + " 秒后同时过期");
        System.out.println("⏱️ 缓存准备耗时: " + (endTime - startTime) / 1000 + "秒");
        System.out.println("⏰ 请等待 " + (EXPIRE_SECONDS + 2) + " 秒后运行 testCacheAvalanche()");
    }

    // ============ 准备缓存（多线程加速版） ============
    @Test
    public void prepareCacheMultiThread() throws InterruptedException {
        System.out.println("⏰ 开始多线程存入缓存（" + EXPIRE_SECONDS + "秒过期）...");
        System.out.println("📊 总用户数: " + TOTAL_USERS);

        long startTime = System.currentTimeMillis();

        int threadPoolSize = Math.max(Runtime.getRuntime().availableProcessors() * 2, 8);
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        CountDownLatch latch = new CountDownLatch(threadPoolSize);
        AtomicInteger totalCached = new AtomicInteger(0);
        AtomicInteger processedCount = new AtomicInteger(0);

        int batchSize = TOTAL_USERS / threadPoolSize;

        for (int t = 0; t < threadPoolSize; t++) {
            int startId = t * batchSize + 1;
            int endId = (t == threadPoolSize - 1) ? TOTAL_USERS : (t + 1) * batchSize;

            executor.submit(() -> {
                int cached = 0;
                for (long i = startId; i <= endId; i++) {
                    String cacheKey = "demo:test:user:id" + i;
                    QueryWrapper<User> wrapper = new QueryWrapper<>();
                    wrapper.eq("id", i);
                    User user = userMapper.selectOne(wrapper);
                    if (user != null) {
                        redisTemplate.opsForValue().set(cacheKey, user, EXPIRE_SECONDS, TimeUnit.SECONDS);
                        cached++;
                    }

                    int processed = processedCount.incrementAndGet();
                    if (processed % 5000 == 0) {
                        System.out.println("  已处理: " + processed + "/" + TOTAL_USERS);
                    }
                }
                totalCached.addAndGet(cached);
                latch.countDown();
            });
        }

        latch.await(10, TimeUnit.MINUTES);
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        System.out.println("✅ 已存入 " + totalCached.get() + " 个缓存，" + EXPIRE_SECONDS + " 秒后同时过期");
        System.out.println("⏱️ 缓存准备耗时: " + (endTime - startTime) / 1000 + "秒");
        System.out.println("⏰ 请等待 " + (EXPIRE_SECONDS + 2) + " 秒后运行 testCacheAvalanche()");
    }

    // ============ 雪崩测试（优化：分页查询，避免一次查太多） ============
    @Test
    public void testCacheAvalanche() throws InterruptedException {
        // 1. 先等待缓存过期
        System.out.println("⏰ 等待缓存过期...");
        Thread.sleep((EXPIRE_SECONDS + 2) * 1000);

        // 2. 验证缓存是否真的失效了
        String testKey = "demo:test:user:id1";
        User testUser = (User) redisTemplate.opsForValue().get(testKey);
        if (testUser != null) {
            System.out.println("⚠️ 缓存还未过期，请检查 Redis 配置或先运行 prepareCache()");
            return;
        }
        System.out.println("✅ 缓存已失效，开始模拟雪崩...");

        // 3. 模拟高并发读取
        int threadCount = THREAD_COUNT;
        int queryCount = QUERY_PER_THREAD;

        System.out.println("🔥 " + threadCount + " 个线程，每线程查询 " + queryCount + " 个用户");
        System.out.println("📊 总查询次数: " + (threadCount * queryCount));

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger dbQueryCount = new AtomicInteger(0);
        AtomicInteger cacheHitCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            // 每个线程分配不同的起始ID，分散查询范围
            final long offset = (i % 10) * 1000 + 1; // 分散到不同的ID段

            executor.submit(() -> {
                try {
                    startLatch.await();

                    for (int j = 0; j < queryCount; j++) {
                        try {
                            // 随机查询，避免所有线程查同样的ID
                            long id = (j % TOTAL_USERS) + 1;
                            String cacheKey = "demo:test:user:id" + id;
                            User user = (User) redisTemplate.opsForValue().get(cacheKey);

                            if (user == null) {
                                dbQueryCount.incrementAndGet();
                                QueryWrapper<User> wrapper = new QueryWrapper<>();
                                wrapper.eq("id", id);
                                user = userMapper.selectOne(wrapper);
                                if (user != null) {
                                    redisTemplate.opsForValue().set(cacheKey, user, 60, TimeUnit.SECONDS);
                                }
                            } else {
                                cacheHitCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();

        boolean completed = endLatch.await(120, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        int totalQueries = threadCount * queryCount;
        int dbQueries = dbQueryCount.get();
        int hitCount = cacheHitCount.get();
        long duration = endTime - startTime;

        System.out.println("\n========== 雪崩测试结果 ==========");
        System.out.println("是否完成: " + (completed ? "✅ 是" : "❌ 否（超时）"));
        System.out.println("总耗时: " + duration + "ms");
        System.out.println("总查询次数: " + totalQueries);
        System.out.println("缓存命中: " + hitCount);
        System.out.println("数据库查询: " + dbQueries);
        System.out.println("错误次数: " + errorCount.get());
        System.out.println("缓存命中率: " + String.format("%.2f%%", hitCount * 100.0 / totalQueries));

        if (dbQueries > totalQueries * 0.3) {
            System.out.println("\n⚠️⚠️⚠️ 缓存雪崩发生！");
            System.out.println("   数据库查询次数: " + dbQueries + "/" + totalQueries);
            System.out.println("   建议：使用互斥锁、永不过期等方案防护");
        } else {
            System.out.println("\n✅ 缓存命中率较高，雪崩未发生或已防护");
        }

        executor.shutdown();
        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }
    }

    // ============ 清理缓存 ============
    @Test
    public void clearCache() {
        System.out.println("⏰ 开始清理缓存...");
        Set<String> keys = redisTemplate.keys("demo:test:user:*");
        if (keys != null && !keys.isEmpty()) {
            Long deleted = redisTemplate.delete(keys);
            System.out.println("✅ 已清理 " + deleted + " 个缓存");
        } else {
            System.out.println("ℹ️ 没有缓存需要清理");
        }
    }

//    // ============ 快速测试（小数据量） ============
//    @Test
//    public void quickTest() throws InterruptedException {
//        // 只测试前1000条数据
//        System.out.println("🚀 快速测试模式（1000条数据）");
//
//        // 修改参数
//        int testSize = 1000;
//        for (long i = 1; i <= testSize; i++) {
//            String cacheKey = "demo:test:user:id" + i;
//            QueryWrapper<User> wrapper = new QueryWrapper<>();
//            wrapper.eq("id", i);
//            User user = userMapper.selectOne(wrapper);
//            if (user != null) {
//                redisTemplate.opsForValue().set(cacheKey, user, 3, TimeUnit.SECONDS);
//            }
//        }
//
//        Thread.sleep(4000);
//
//        int threadCount = 20;
//        int queryCount = 100;
//        // ... 后续测试逻辑
//        System.out.println("✅ 快速测试完成");
//    }
}