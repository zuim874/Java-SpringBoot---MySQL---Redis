package com.xuwenye.demo.util.redis;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 分布式锁工具类（基于 Redisson）
 * 1.lock：加锁（默认 30 秒锁超时）
 * 2.tryLock：非阻塞尝试加锁（可指定租约时间，或使用看门狗自动续期）
 * 3.unlock：释放锁（自动判断当前线程是否持有）
 * 4.isLocked：检查锁是否被持有
 * <p>
 * 看门狗（watchdog）机制：
 * 使用不带 leaseTime 的 tryLock(key, waitTime, unit) 时，Redisson 会启动看门狗自动续期——
 * 默认锁超时 30 秒、每 10 秒续期一次，业务执行多久锁就持有多久，unlock 后停止续期；
 * 若业务异常未释放锁，30 秒后锁自动过期兜底，避免死锁。
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
     * 尝试加锁（看门狗模式，推荐用于业务执行时长不确定的场景）
     * <p>
     * 与带 leaseTime 的版本不同，本方法不指定锁租约时间，Redisson 会启动看门狗自动续期：
     * 1.默认锁超时 30 秒，每 10 秒自动续期一次（锁超时的 1/3）
     * 2.业务执行多久，锁就持有多久——不会因租约到期被提前释放导致并发请求进入临界区
     * 3.调用 unlock 释放锁后，看门狗停止续期
     * 4.若业务异常且未 unlock，30 秒后锁自动过期兜底，避免死锁
     * <p>
     * @author ZuiM
     * @param key 锁的 key
     * @param waitTime 等待获取锁的最长时间
     * @param unit 时间单位
     * @return boolean true=加锁成功
     * @throws InterruptedException 线程中断异常
     */
    public boolean tryLock(String key, long waitTime, TimeUnit unit) throws InterruptedException {
        RLock lock = redissonClient.getLock(key);
        return lock.tryLock(waitTime, unit);
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
