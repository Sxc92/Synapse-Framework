package com.indigo.cache.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import static org.junit.jupiter.api.Assertions.*;

/**
 * RedisConnectionConfiguration 测试类
 * 
 * @author 史偕成
 * @date 2025/01/08
 */
@SpringBootTest(classes = {CacheProperties.class, RedisConnectionConfiguration.class})
@TestPropertySource(properties = {
    "synapse.cache.redis.enabled=true",
    "synapse.cache.redis.connection.host=localhost",
    "synapse.cache.redis.connection.port=6379",
    "synapse.cache.redis.connection.database=0",
    "synapse.cache.redis.pool.max-active=8",
    "synapse.cache.redis.pool.max-idle=8",
    "synapse.cache.redis.pool.min-idle=0"
})
class RedisConnectionConfigurationTest {

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Test
    void testRedisConnectionFactoryCreation() {
        assertNotNull(redisConnectionFactory, "RedisConnectionFactory should be created");
        assertTrue(redisConnectionFactory instanceof org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory, 
                  "Should be LettuceConnectionFactory");
    }

    @Test
    void testConnectionFactoryProperties() {
        assertNotNull(redisConnectionFactory, "Connection factory should not be null");
        // 测试连接工厂是否可用
        assertDoesNotThrow(() -> {
            redisConnectionFactory.getConnection().ping();
        }, "Connection should be able to ping");
    }
} 