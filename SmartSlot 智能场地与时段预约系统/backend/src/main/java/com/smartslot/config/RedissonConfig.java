package com.smartslot.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 分布式锁与延迟队列核心配置
 * 具备生产级连接超时配置与离线自愈降级保障
 */
@Slf4j
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:127.0.0.1}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = "redis://" + redisHost + ":" + redisPort;
        
        var singleServerConfig = config.useSingleServer()
                .setAddress(address)
                .setConnectionMinimumIdleSize(2)
                .setConnectionPoolSize(8)
                .setConnectTimeout(3000)
                .setTimeout(3000)
                .setRetryAttempts(2)
                .setRetryInterval(1000);

        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            singleServerConfig.setPassword(redisPassword.trim());
        }

        try {
            RedissonClient client = Redisson.create(config);
            log.info("RedissonClient 成功初始化并连接至: {}", address);
            return client;
        } catch (Exception e) {
            log.warn("Redisson 无法连接至 {}，系统将自动启用 JVM 内存级原子锁与 DelayQueue 容灾降级模式: {}", address, e.getMessage());
            return null;
        }
    }
}
