package com.xuwenye.demo.util.redis;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 分布式锁工具类（基于 Redisson）
 * 1.lock：加锁（默认 30 秒锁超时）
 * 2.tryLock：非阻塞尝试加锁
 * 3.unlock：释放锁（自动判断当前线程是否持有）
 * 4.isLocked：检查锁是否被持有
 * <p>
 * @author ZuiM
 */
@Component
public class RedisLockHelper {
    private final RedissonClient redissonClient;
    public RedisLockHelper(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 加锁（默认锁超时 30 秒）
     * <p>
     * @author ZuiM
     * @param key 锁的 key
     * @return RLock 锁对象（用于释放）
     */
    public RLock lock(String key) {
        RLock lock = redissonClient.getLock(key);
        lock.lock(30, TimeUnit.SECONDS);
        return lock;
    }

    /**
     * 加锁（自定义锁超时时间）
     * <p>
     * @author ZuiM
     * @param key 锁的 key
     * @param leaseTime 锁超时时间
     * @param unit 时间单位
     * @return RLock 锁对象（用于释放）
     */
    public RLock lock(String key, long leaseTime, TimeUnit unit) {
        RLock lock = redissonClient.getLock(key);
        lock.lock(leaseTime, unit);
        return lock;
    }

    /**
     * 尝试加锁（非阻塞，立即返回）
     * <p>
     * @author ZuiM
     * @param key 锁的 key
     * @param waitTime 等待获取锁的最长时间
     * @param leaseTime 锁超时时间
     * @param unit 时间单位
     * @return boolean true=加锁成功
     * @throws InterruptedException 线程中断异常
     */
    public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException {
        RLock lock = redissonClient.getLock(key);
        return lock.tryLock(waitTime, leaseTime, unit);
    }

    /**
     * 释放锁（按 key，自动判断当前线程是否持有）
     * <p>
     * @author ZuiM
     * @param key 锁的 key
     */
    public void unlock(String key) {
        RLock lock = redissonClient.getLock(key);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 释放锁（通过 RLock 对象）
     * <p>
     * @author ZuiM
     * @param lock 锁对象
     */
    public void unlock(RLock lock) {
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 检查锁是否被持有
     * <p>
     * @author ZuiM
     * @param key 锁的 key
     * @return boolean true=被持有
     */
    public boolean isLocked(String key) {
        RLock lock = redissonClient.getLock(key);
        return lock.isLocked();
    }
}
