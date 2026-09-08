package com.xuwenye.demo.service;

import com.xuwenye.demo.Mapper.UserMapper;
import com.xuwenye.demo.util.redis.RedisUtil;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service 集成测试基类
 * 1.依赖真实环境（MySQL + Redis + ActiveMQ，运行前需先启动）
 * 2.提供唯一数据后缀：保证测试数据与开发数据互不冲突
 * 3.提供缓存清理钩子：测试结束后删除本次产生的 Redis 缓存
 * <p>
 * 说明：激活 test profile 加载 application-dev.properties（自包含测试配置），
 * 避免测试依赖开发环境 application-dev.properties 的配置。
 * <p>
 * @author ZuiM
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractServiceTest {

    /** 本次 JVM 启动唯一后缀（每次启动随机，避免历史残留数据冲突） */
    protected static final String SUFFIX = String.valueOf(System.nanoTime() % 100000);

    /** 本次测试产生的 Redis 缓存 key（测试结束后统一清理） */
    protected final List<String> createdCacheKeys = new ArrayList<>();

    @Autowired
    protected RedisUtil redisUtil;

    @Autowired
    protected UserMapper userMapper;

    /**
     * 生成带唯一后缀的测试名（用户名/昵称/邮箱等）
     * <p>
     * @author ZuiM
     * @param prefix 名称前缀
     * @return String 唯一名称
     */
    protected String uniqueName(String prefix) {
        return prefix + SUFFIX + "_" + ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    /**
     * 记录待清理的缓存 key（测试结束后统一删除）
     * <p>
     * @author ZuiM
     * @param key Redis 缓存 key
     */
    protected void trackCacheKey(String key) {
        createdCacheKeys.add(key);
    }

    /**
     * 每个测试方法结束后清理本次产生的 Redis 缓存
     * <p>
     * @author ZuiM
     */
    @AfterEach
    public void cleanUpCache() {
        for (String key : createdCacheKeys) {
            redisUtil.delete(key);
        }
        createdCacheKeys.clear();
    }
}
