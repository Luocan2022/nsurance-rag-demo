package com.insurance.rag.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 热缓存配置。
 * 开关：chat.history.cache=redis 才生效；默认 memory 时不装配，本地没装 Redis 也能启动。
 */
@Configuration
@ConditionalOnProperty(prefix = "chat.history", name = "cache", havingValue = "redis")
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}
