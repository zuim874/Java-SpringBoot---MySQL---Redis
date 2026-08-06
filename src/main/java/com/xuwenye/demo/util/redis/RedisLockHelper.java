package com.xuwenye.demo.util.redis;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 分布式锁工具类
 *
 * @author ZuiM
 * @date 2026-07-20
 */
@Component
public class RedisLockHelper {
    private final RedissonClient redissonClient;
    public RedisLockHelper(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 加锁（默认锁超时 30 秒）
     *
     * @param key
     */
    public RLock lock(String key) {
        RLock lock = redissonClient.getLock(key);
        lock.lock(30, TimeUnit.SECONDS);
        return lock;
    }

    /**
     * 加锁（自定义锁超时时间）
     */
    public RLock lock(String key, long leaseTime, TimeUnit unit) {
        RLock lock = redissonClient.getLock(key);
        lock.lock(leaseTime, unit);
        return lock;
    }

    /**
     * 尝试加锁（非阻塞，立即返回）
     */
    public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException {
        RLock lock = redissonClient.getLock(key);
        return lock.tryLock(waitTime, leaseTime, unit);
    }

    /**
     * 释放锁
     */
    public void unlock(String key) {
        RLock lock = redissonClient.getLock(key);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 释放锁（通过 RLock 对象）
     */
    public void unlock(RLock lock) {
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 检查锁是否被持有
     */
    public boolean isLocked(String key) {
        RLock lock = redissonClient.getLock(key);
        return lock.isLocked();
    }
}
