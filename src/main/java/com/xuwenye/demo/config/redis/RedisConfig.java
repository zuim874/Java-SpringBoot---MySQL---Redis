package com.xuwenye.demo.config.redis;

import com.xuwenye.demo.util.redis.RedisLockHelper;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 和 Redisson 配置类
 * 1.配置 Redisson 客户端（分布式锁核心）
 * 2.配置 RedisTemplate（String key + Jackson value 序列化）
 * 3.注入分布式锁工具 RedisLockHelper
 * <p>
 * @author ZuiM
 */
@Configuration
public class RedisConfig {

    // ========== Redis 连接配置（从 application.properties 读取） ==========
    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;
    @Value("${spring.data.redis.port:6379}")
    private int redisPort;
    @Value("${spring.data.redis.password:}")
    private String redisPassword;
    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    // ========== 1. Redisson 客户端（分布式锁核心） ==========
    /**
     * Redisson 客户端（分布式锁核心）
     * 1.构建单节点模式配置
     * 2.设置连接池与超时参数
     * 3.有密码则设置密码
     * <p>
     * @author ZuiM
     * @return RedissonClient 分布式锁客户端
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        // 单节点模式配置
        String address = "redis://" + redisHost + ":" + redisPort;
        config.useSingleServer()
                .setAddress(address)
                .setDatabase(redisDatabase)
                .setConnectionPoolSize(10)
                .setConnectionMinimumIdleSize(5)
                .setIdleConnectionTimeout(10000)
                .setConnectTimeout(10000)
                .setTimeout(3000)
                .setRetryAttempts(3)
                .setRetryInterval(1500);

        // 如果有密码，设置密码
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.useSingleServer().setPassword(redisPassword);
        }

        return Redisson.create(config);
    }

    // ========== 2. RedisTemplate（通用 Redis 操作） ==========
    /**
     * RedisTemplate（通用 Redis 操作）
     * 1.key 使用 String 序列化器
     * 2.value 使用 Jackson 序列化器（支持对象存储与 LocalDateTime 时间类型）
     * 3.启用默认类型信息（写入 @class 字段），保证反序列化为正确类型
     * <p>
     * @author ZuiM
     * @param connectionFactory Redis 连接工厂
     * @return RedisTemplate&lt;String, Object&gt; 通用 Redis 操作模板
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 使用 String 序列化器处理 key
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // 使用 Jackson 序列化器处理 value（支持对象存储，含 LocalDateTime 等 Java8 时间类型）
        ObjectMapper objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
        // 启用默认类型信息，序列化时写入 @class 字段，确保反序列化为正确类型
        // 使用 As.PROPERTY 格式（与 GenericJackson2JsonRedisSerializer 默认格式一致）
        objectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    // ========== 3. 分布式锁工具类（可选，封装常用操作） ==========
    /**
     * 分布式锁工具类（封装常用操作）
     * <p>
     * @author ZuiM
     * @param redissonClient Redisson 客户端
     * @return RedisLockHelper 分布式锁工具
     */
    @Bean
    public RedisLockHelper redisLockHelper(RedissonClient redissonClient) {
        return new RedisLockHelper(redissonClient);
    }
}