package com.smartslot.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁与时段库存占用管理器
 * 满足基本要求：缓存 (Redis / 本地双模降级保障)
 */
@Slf4j
@Component
public class LockManager {

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    // 本地 Caffeine 缓存与内存锁 (作为 Redis 离线时的平滑降级)
    private final Cache<String, Long> localLockCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .maximumSize(50000)
            .build();

    private final ConcurrentHashMap<String, Long> localExpireMap = new ConcurrentHashMap<>();

    private volatile boolean redisAvailable = true;

    /**
     * 尝试锁定某个时段 (SETNX 语义)
     *
     * @param lockKey    锁键 (如: slot:lock:venue:1:2026-09-05:09:00-10:00)
     * @param lockValue  锁值 (如: 订单号或用户ID)
     * @param ttlSeconds 超时自动释放秒数 (如: 900 秒即 15 分钟)
     * @return true-加锁成功, false-已被其他人锁定
     */
    public boolean tryLock(String lockKey, String lockValue, long ttlSeconds) {
        if (redisAvailable && redisTemplate != null) {
            try {
                Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, Duration.ofSeconds(ttlSeconds));
                return Boolean.TRUE.equals(success);
            } catch (Exception e) {
                log.warn("Redis 连接异常，平滑降级为本地内存锁机制: {}", e.getMessage());
                redisAvailable = false;
            }
        }

        // 本地内存锁降级方案 (线程安全原子操作)
        long expireAt = System.currentTimeMillis() + (ttlSeconds * 1000);
        synchronized (this) {
            cleanExpiredLocalLocks();
            Long existing = localExpireMap.get(lockKey);
            if (existing != null && existing > System.currentTimeMillis()) {
                return false; // 已被占用
            }
            localExpireMap.put(lockKey, expireAt);
            localLockCache.put(lockKey, expireAt);
            return true;
        }
    }

    /**
     * 检查某个时段是否正处于被锁状态
     */
    public boolean isLocked(String lockKey) {
        if (redisAvailable && redisTemplate != null) {
            try {
                Boolean hasKey = redisTemplate.hasKey(lockKey);
                return Boolean.TRUE.equals(hasKey);
            } catch (Exception e) {
                redisAvailable = false;
            }
        }

        Long expireAt = localExpireMap.get(lockKey);
        if (expireAt == null) {
            return false;
        }
        if (expireAt <= System.currentTimeMillis()) {
            localExpireMap.remove(lockKey);
            return false;
        }
        return true;
    }

    /**
     * 释放锁
     */
    public void unlock(String lockKey) {
        if (redisAvailable && redisTemplate != null) {
            try {
                redisTemplate.delete(lockKey);
            } catch (Exception e) {
                redisAvailable = false;
            }
        }
        localExpireMap.remove(lockKey);
        localLockCache.invalidate(lockKey);
    }

    private void cleanExpiredLocalLocks() {
        long now = System.currentTimeMillis();
        localExpireMap.entrySet().removeIf(entry -> entry.getValue() <= now);
    }
}
