package com.xuwenye.demo.util.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

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
     * 设置缓存（带过期时间）
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 判断 key 是否存在
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 获取 key 的剩余过期时间
     */
    public long getExpire(String key, TimeUnit unit) {
        Long expire = redisTemplate.getExpire(key, unit);
        return expire != null ? expire : -1;
    }

    /**
     * 获取缓存值
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除缓存
     */
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 原子「自增 + 首次设置过期时间」（Lua 实现）
     * <p>
     * 场景：限流计数。第一次调用时创建 key 并设置 TTL，
     * 之后只自增。INCR 与 EXPIRE 在同一个 Lua 脚本内执行，天然原子。
     *
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

    public void expire(String key, long timeout, TimeUnit unit) {
        redisTemplate.expire(key, timeout, unit);
    }

    public Boolean setIfAbsent(String key, String value, long timeout, TimeUnit unit) {
        return redisTemplate.opsForValue()
                .setIfAbsent(key, value, timeout, unit);
    }
}
