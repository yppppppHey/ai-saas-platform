package com.aisaas.common.util;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
public class DistributedLock {

    @Resource
    private RedissonClient redissonClient;

    /**
     * 获取锁（阻塞式）
     */
    public void lock(String lockKey, Runnable task) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            lock.lock();
            log.debug("获取锁成功: {}", lockKey);
            task.run();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("释放锁成功: {}", lockKey);
            }
        }
    }

    /**
     * 尝试获取锁（非阻塞式）
     */
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit, Runnable task) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(waitTime, leaseTime, unit);
            if (locked) {
                log.debug("尝试获取锁成功: {}", lockKey);
                task.run();
                return true;
            } else {
                log.warn("尝试获取锁失败: {}", lockKey);
                return false;
            }
        } catch (InterruptedException e) {
            log.error("尝试获取锁被中断: {}", lockKey, e);
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("释放锁成功: {}", lockKey);
            }
        }
    }

    /**
     * 带返回值的可重入锁
     */
    public <T> T lockWithResult(String lockKey, Supplier<T> supplier) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            lock.lock();
            log.debug("获取锁成功: {}", lockKey);
            return supplier.get();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("释放锁成功: {}", lockKey);
            }
        }
    }

    /**
     * 获取公平锁
     */
    public void fairLock(String lockKey, Runnable task) {
        RLock lock = redissonClient.getFairLock(lockKey);
        try {
            lock.lock();
            log.debug("获取公平锁成功: {}", lockKey);
            task.run();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("释放公平锁成功: {}", lockKey);
            }
        }
    }

    /**
     * 获取读写锁的读锁
     */
    public void readLock(String lockKey, Runnable task) {
        var rwLock = redissonClient.getReadWriteLock(lockKey);
        RLock lock = rwLock.readLock();
        try {
            lock.lock();
            log.debug("获取读锁成功: {}", lockKey);
            task.run();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("释放读锁成功: {}", lockKey);
            }
        }
    }

    /**
     * 获取读写锁的写锁
     */
    public void writeLock(String lockKey, Runnable task) {
        var rwLock = redissonClient.getReadWriteLock(lockKey);
        RLock lock = rwLock.writeLock();
        try {
            lock.lock();
            log.debug("获取写锁成功: {}", lockKey);
            task.run();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("释放写锁成功: {}", lockKey);
            }
        }
    }

    /**
     * 续期锁（看门狗机制）
     * Redisson默认会自动续期，此方法用于特殊情况下的手动续期
     */
    public boolean renewLock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            // Redisson会自动续期，这里可以添加自定义逻辑
            log.debug("续期锁: {}", lockKey);
            return true;
        }
        return false;
    }
}
