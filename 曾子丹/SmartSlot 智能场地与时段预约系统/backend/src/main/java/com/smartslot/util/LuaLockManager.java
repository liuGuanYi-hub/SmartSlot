package com.smartslot.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 企业级 Redis Lua 脚本原子时段预占锁管理器
 * 1. 使用 Lua 脚本保证单次原子执行，杜绝并发超卖
 * 2. 具备本地 Caffeine + ConcurrentHashMap 双模自愈容灾
 */
@Slf4j
@Component
public class LuaLockManager {

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private DefaultRedisScript<Long> lockScript;
    private DefaultRedisScript<Long> unlockScript;

    // 内存容灾锁 (Redis 离线或异常时自动无缝接管)
    private final Cache<String, String> localValueCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .maximumSize(50000)
            .build();

    private final ConcurrentHashMap<String, Long> localExpireMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> localValueMap = new ConcurrentHashMap<>();

    private volatile boolean redisAvailable = true;

    @PostConstruct
    public void init() {
        lockScript = new DefaultRedisScript<>();
        lockScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/slot_lock.lua")));
        lockScript.setResultType(Long.class);

        unlockScript = new DefaultRedisScript<>();
        unlockScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/slot_unlock.lua")));
        unlockScript.setResultType(Long.class);
    }

    /**
     * 原子抢占时段锁 (Lua 脚本执行)
     *
     * @param lockKey    时段锁键 (如 slot:lock:1:2026-09-06:09:00-10:00)
     * @param lockValue  占用凭据 (如 UID:2:ORD20260906001)
     * @param ttlSeconds 过期自动释放秒数 (如 900 秒，即 15 分钟)
     * @return true-成功抢占锁定, false-已被抢先锁定
     */
    public boolean tryLockAtomic(String lockKey, String lockValue, long ttlSeconds) {
        if (redisAvailable && redisTemplate != null) {
            try {
                Long result = redisTemplate.execute(
                        lockScript,
                        Collections.singletonList(lockKey),
                        lockValue,
                        String.valueOf(ttlSeconds)
                );
                return result != null && result == 1L;
            } catch (Exception e) {
                log.warn("Redis 执行 Lua 加锁异常，自动无缝降级为本地内存级原子锁: {}", e.getMessage());
                redisAvailable = false;
            }
        }

        // 本地内存原子锁降级方案
        long expireAt = System.currentTimeMillis() + (ttlSeconds * 1000);
        synchronized (this) {
            cleanExpiredLocalLocks();
            Long existingExpire = localExpireMap.get(lockKey);
            String existingVal = localValueMap.get(lockKey);

            if (existingExpire != null && existingExpire > System.currentTimeMillis()) {
                // 已被占用，检查是否为相同凭据重入
                if (lockValue.equals(existingVal)) {
                    localExpireMap.put(lockKey, expireAt);
                    return true;
                }
                return false;
            }

            localExpireMap.put(lockKey, expireAt);
            localValueMap.put(lockKey, lockValue);
            localValueCache.put(lockKey, lockValue);
            return true;
        }
    }

    /**
     * 安全原子释放时段锁 (Lua 脚本验证凭据)
     *
     * @param lockKey       时段锁键
     * @param expectedValue 预期的锁持有凭据 (如 UID:2 或 'FORCE_UNLOCK')
     * @return true-释放成功或锁已不存在, false-凭据不匹配拒绝释放
     */
    public boolean unlockAtomic(String lockKey, String expectedValue) {
        if (redisAvailable && redisTemplate != null) {
            try {
                Long result = redisTemplate.execute(
                        unlockScript,
                        Collections.singletonList(lockKey),
                        expectedValue != null ? expectedValue : "FORCE_UNLOCK"
                );
                return result != null && result == 1L;
            } catch (Exception e) {
                redisAvailable = false;
            }
        }

        synchronized (this) {
            String currentVal = localValueMap.get(lockKey);
            if (currentVal == null || currentVal.equals(expectedValue) || "FORCE_UNLOCK".equals(expectedValue)) {
                localExpireMap.remove(lockKey);
                localValueMap.remove(lockKey);
                localValueCache.invalidate(lockKey);
                return true;
            }
            return false;
        }
    }

    /**
     * 检查时段是否正处于被占用锁定状态
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
        if (expireAt == null) return false;
        if (expireAt <= System.currentTimeMillis()) {
            localExpireMap.remove(lockKey);
            localValueMap.remove(lockKey);
            return false;
        }
        return true;
    }

    private void cleanExpiredLocalLocks() {
        long now = System.currentTimeMillis();
        localExpireMap.entrySet().removeIf(entry -> {
            if (entry.getValue() <= now) {
                localValueMap.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }
}
