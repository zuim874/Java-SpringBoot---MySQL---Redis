package com.xuwenye.demo.util.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Redis 操作工具
 * 1.set/get/delete：通用缓存读写
 * 2.hasKey/getExpire：key 存在性与剩余过期时间
 * 3.setIfAbsent：原子防刷（不存在才设置，验证码限流用）
 * 4.incrementAndExpire：Lua 原子「自增 + 首次设置过期」（接口限流用）
 * 5.expire/increment：辅助操作
 * <p>
 * @author ZuiM
 */
@Component
public class RedisUtil {
    /** 通用缓存过期时间（分钟，默认 10） */
    @Value("${test.redisTimeOut:1}")
    private long redisTimeOut;
    /** 验证码发送间隔（分钟，默认 1） */
    @Value("${test.redisCodeSendTimeOut:1}")
    private long redisCodeSendTimeOut;
    /** 验证码过期时间（分钟，默认 10） */
    @Value("${test.redisCodeTimeOut:10}")
    private long redisCodeTimeOut;

    private final RedisTemplate<String, Object> redisTemplate;
    public RedisUtil(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 原子「自增 + 首次设置过期时间」Lua 脚本：
     * 用于限流场景，避免 INCR 与 EXPIRE 两步非原子导致 key 永久残留。
     */
    private static final DefaultRedisScript<Long> INCR_EXPIRE_SCRIPT = new DefaultRedisScript<>(
            "local c = redis.call('incr', KEYS[1]) " +
                    "if c == 1 then " +
                    "    redis.call('expire', KEYS[1], tonumber(ARGV[1])) " +
                    "end " +
                    "return c",
            Long.class
    );

    /**
     * 设置缓存（使用默认过期时间 redisTimeOut 分钟）
     * <p>
     * @author ZuiM
     * @param key 缓存 key
     * @param value 缓存值
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value, redisTimeOut + ThreadLocalRandom.current().nextInt(0, 300), TimeUnit.SECONDS);
    }

    public void setCode(String key, Object value) {
        redisTemplate.opsForValue().set(key, value, redisCodeTimeOut + ThreadLocalRandom.current().nextInt(0, 60), TimeUnit.SECONDS);
    }

    /**
     * 判断 key 是否存在
     * <p>
     * @author ZuiM
     * @param key 缓存 key
     * @return boolean true=存在
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 获取 key 的剩余过期时间
     * <p>
     * @author ZuiM
     * @param key 缓存 key
     * @param unit 时间单位
     * @return long 剩余时间（key 不存在返回 -1）
     */
    public long getExpire(String key, TimeUnit unit) {
        Long expire = redisTemplate.getExpire(key, unit);
        return expire != null ? expire : -1;
    }

    /**
     * 获取缓存值
     * <p>
     * @author ZuiM
     * @param key 缓存 key
     * @return Object 缓存值（不存在返回 null）
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除缓存
     * <p>
     * @author ZuiM
     * @param key 缓存 key
     * @return boolean true=删除成功
     */
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    /**
     * 按前缀模糊删除缓存（匹配 prefix* 的所有 key）
     * 注意：仅用于缓存清理场景；生产环境 key 数量较大时应改用 SCAN 分批删除
     * <p>
     * @author ZuiM
     * @param prefix 缓存 key 前缀（不含 *）
     * @return long 实际删除的 key 数量
     */
    public long deleteByPrefix(String prefix) {
        Set<String> matched = redisTemplate.keys(prefix + "*");
        if (matched == null || matched.isEmpty()) {
            return 0;
        }
        Long deleted = redisTemplate.delete(matched);
        return deleted == null ? 0 : deleted;
    }

    /**
     * 自增（按指定步长）
     * <p>
     * @author ZuiM
     * @param key 缓存 key
     * @param delta 步长
     * @return Long 自增后的值
     */
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 原子「自增 + 首次设置过期时间」（Lua 实现）
     * <p>
     * 场景：限流计数。第一次调用时创建 key 并设置 TTL，
     * 之后只自增。INCR 与 EXPIRE 在同一个 Lua 脚本内执行，天然原子。
     *
     * @author ZuiM
     * @param key    限流 key
     * @param timeout 过期时间（从第一次调用开始计时）
     * @param unit    时间单位
     * @return 自增后的计数值；Redis 异常时返回 null
     */
    public Long incrementAndExpire(String key, long timeout, TimeUnit unit) {
        long seconds = unit.toSeconds(timeout);
        return redisTemplate.execute(
                INCR_EXPIRE_SCRIPT,
                Collections.singletonList(key),
                seconds
        );
    }

    /**
     * 设置 key 过期时间
     * <p>
     * @author ZuiM
     * @param key 缓存 key
     * @param timeout 过期时长
     * @param unit 时间单位
     */
    public void expire(String key, long timeout, TimeUnit unit) {
        redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 原子防刷：key 不存在才设置（SET NX），使用验证码过期时间 redisCodeTimeOut
     * 1.返回 true 表示首次设置成功（放行）
     * 2.返回 false 表示 key 已存在（限流窗口内重复请求）
     * <p>
     * @author ZuiM
     * @param key 限流 key
     * @param value 值（通常为 "1"）
     * @return Boolean true=首次设置成功
     */
    public Boolean setIfAbsent(String key, String value) {
        return redisTemplate.opsForValue()
                .setIfAbsent(key, value, redisCodeSendTimeOut, TimeUnit.SECONDS);
    }
}
