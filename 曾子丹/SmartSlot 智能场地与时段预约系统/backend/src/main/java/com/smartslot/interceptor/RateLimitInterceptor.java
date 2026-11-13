package com.smartslot.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.smartslot.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 接口高频限流与防刷拦截器
 * 满足基本要求：拦截器 (Interceptor) + Redis / 缓存
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    // 每秒单个 IP 最大允许请求数
    private static final int MAX_REQUESTS_PER_SECOND = 15;

    // Caffeine 本地计数降级缓存 (1秒自动过期)
    private final Cache<String, AtomicInteger> localCounterCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.SECONDS)
            .maximumSize(10000)
            .build();

    private volatile boolean redisAvailable = true;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String ip = getClientIp(request);
        String uri = request.getRequestURI();
        String limitKey = "rate:limit:" + ip + ":" + uri;

        long currentCount;
        if (redisAvailable && redisTemplate != null) {
            try {
                currentCount = redisTemplate.opsForValue().increment(limitKey);
                if (currentCount == 1) {
                    redisTemplate.expire(limitKey, Duration.ofSeconds(1));
                }
            } catch (Exception e) {
                redisAvailable = false;
                currentCount = incrementLocal(limitKey);
            }
        } else {
            currentCount = incrementLocal(limitKey);
        }

        if (currentCount > MAX_REQUESTS_PER_SECOND) {
            log.warn("触发接口防刷拦截: ip={}, uri={}, count={}", ip, uri, currentCount);
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            Result<Void> result = Result.error(429, "操作过于频繁，请稍后再试");
            response.getWriter().write(objectMapper.writeValueAsString(result));
            return false;
        }

        return true;
    }

    private long incrementLocal(String key) {
        AtomicInteger counter = localCounterCache.get(key, k -> new AtomicInteger(0));
        return counter.incrementAndGet();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "127.0.0.1";
    }
}
