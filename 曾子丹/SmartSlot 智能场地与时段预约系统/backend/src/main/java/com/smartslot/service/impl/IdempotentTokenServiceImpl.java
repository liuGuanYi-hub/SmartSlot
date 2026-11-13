package com.smartslot.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.smartslot.service.IdempotentTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 幂等 Token 服务实现类 (Redis Lua 原子消费 + Caffeine 本地容灾双模)
 */
@Slf4j
@Service
public class IdempotentTokenServiceImpl implements IdempotentTokenService {

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private static final String TOKEN_PREFIX = "idemp:token:";
    private static final long DEFAULT_EXPIRE_SECONDS = 300; // 5分钟有效

    // 本地内存容灾降级缓存
    private final Cache<String, String> localTokenCache = Caffeine.newBuilder()
            .expireAfterWrite(DEFAULT_EXPIRE_SECONDS, TimeUnit.SECONDS)
            .maximumSize(50000)
            .build();

    private final ConcurrentHashMap<String, Long> localExpireMap = new ConcurrentHashMap<>();

    private volatile boolean redisAvailable = true;

    private static final String CONSUME_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";

    private final DefaultRedisScript<Long> consumeScript;

    public IdempotentTokenServiceImpl() {
        this.consumeScript = new DefaultRedisScript<>();
        this.consumeScript.setScriptText(CONSUME_LUA);
        this.consumeScript.setResultType(Long.class);
    }

    @Override
    public String generateToken() {
        String token = "IDEMP_" + UUID.randomUUID().toString().replace("-", "") + System.currentTimeMillis();
        String redisKey = TOKEN_PREFIX + token;

        if (redisAvailable && redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(redisKey, token, Duration.ofSeconds(DEFAULT_EXPIRE_SECONDS));
                return token;
            } catch (Exception e) {
                log.warn("Redis 存储幂等 Token 异常，平滑降级至本地内存容灾: {}", e.getMessage());
                redisAvailable = false;
            }
        }

        // 本地降级
        synchronized (this) {
            localTokenCache.put(redisKey, token);
            localExpireMap.put(redisKey, System.currentTimeMillis() + DEFAULT_EXPIRE_SECONDS * 1000);
        }
        return token;
    }

    @Override
    public boolean verifyAndConsume(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }

        String redisKey = TOKEN_PREFIX + token;

        if (redisAvailable && redisTemplate != null) {
            try {
                Long result = redisTemplate.execute(
                        consumeScript,
                        Collections.singletonList(redisKey),
                        token
                );
                return result != null && result == 1L;
            } catch (Exception e) {
                log.warn("Redis 执行原子消费 Token 失败，转入本地内存核验: {}", e.getMessage());
                redisAvailable = false;
            }
        }

        // 本地容灾原子核验
        synchronized (this) {
            Long expireTime = localExpireMap.get(redisKey);
            String storedToken = localTokenCache.getIfPresent(redisKey);

            if (storedToken != null && storedToken.equals(token)) {
                if (expireTime != null && expireTime > System.currentTimeMillis()) {
                    localTokenCache.invalidate(redisKey);
                    localExpireMap.remove(redisKey);
                    return true;
                }
            }
            localTokenCache.invalidate(redisKey);
            localExpireMap.remove(redisKey);
            return false;
        }
    }
}
