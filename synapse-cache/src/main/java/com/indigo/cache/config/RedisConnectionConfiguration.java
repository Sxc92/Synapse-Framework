package com.indigo.cache.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.time.Duration;
import java.util.Arrays;

/**
 * Redis连接工厂配置类
 * 支持单机、集群、哨兵模式
 * 
 * @author 史偕成
 * @date 2025/01/08
 */
@Slf4j
@Configuration
@ConditionalOnClass(RedisConnectionFactory.class)
public class RedisConnectionConfiguration {
    
    @Autowired
    private CacheProperties cacheProperties;
    
    /**
     * 创建Redis连接工厂
     * 根据配置自动选择单机、集群或哨兵模式
     */
    @Bean("redisConnectionFactory")
    @Primary
    @ConditionalOnMissingBean(name = "redisConnectionFactory")
    public RedisConnectionFactory redisConnectionFactory() {
        RedisConnectionFactory factory;
        
        if (cacheProperties.getRedisCache().getCluster().isEnabled()) {
            factory = createClusterConnectionFactory();
            log.info("创建Redis集群连接工厂");
        } else if (cacheProperties.getRedisCache().getSentinel().isEnabled()) {
            factory = createSentinelConnectionFactory();
            log.info("创建Redis哨兵连接工厂");
        } else {
            factory = createStandaloneConnectionFactory();
            log.info("创建Redis单机连接工厂");
        }
        
        return factory;
    }
    
    /**
     * 创建单机Redis连接工厂
     */
    private RedisConnectionFactory createStandaloneConnectionFactory() {
        CacheProperties.RedisCache.Connection conn = cacheProperties.getRedisCache().getConnection();
        CacheProperties.RedisCache.Pool pool = cacheProperties.getRedisCache().getPool();
        
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(conn.getHost());
        config.setPort(conn.getPort());
        config.setDatabase(conn.getDatabase());
        if (conn.getPassword() != null && !conn.getPassword().isEmpty()) {
            config.setPassword(conn.getPassword());
        }
        // 注意：RedisStandaloneConfiguration没有setClientName方法
        
        LettucePoolingClientConfiguration clientConfig = createLettuceClientConfiguration(pool);
        
        return new LettuceConnectionFactory(config, clientConfig);
    }
    
    /**
     * 创建哨兵Redis连接工厂
     */
    private RedisConnectionFactory createSentinelConnectionFactory() {
        CacheProperties.RedisCache.Sentinel sentinel = cacheProperties.getRedisCache().getSentinel();
        CacheProperties.RedisCache.Pool pool = cacheProperties.getRedisCache().getPool();
        
        RedisSentinelConfiguration config = new RedisSentinelConfiguration();
        config.master(sentinel.getMaster());
        // 将String数组转换为RedisNode列表
        for (String node : sentinel.getNodes()) {
            String[] parts = node.split(":");
            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 26379;
            config.sentinel(host, port);
        }
        if (sentinel.getPassword() != null && !sentinel.getPassword().isEmpty()) {
            config.setSentinelPassword(sentinel.getPassword());
        }
        
        LettucePoolingClientConfiguration clientConfig = createLettuceClientConfiguration(pool);
        
        return new LettuceConnectionFactory(config, clientConfig);
    }
    
    /**
     * 创建集群Redis连接工厂
     */
    private RedisConnectionFactory createClusterConnectionFactory() {
        CacheProperties.RedisCache.Cluster cluster = cacheProperties.getRedisCache().getCluster();
        CacheProperties.RedisCache.Pool pool = cacheProperties.getRedisCache().getPool();
        
        RedisClusterConfiguration config = new RedisClusterConfiguration();
        // 将String数组转换为RedisNode列表
        for (String node : cluster.getNodes()) {
            String[] parts = node.split(":");
            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 6379;
            config.clusterNode(host, port);
        }
        config.setMaxRedirects(cluster.getMaxRedirects());
        
        LettucePoolingClientConfiguration clientConfig = createLettuceClientConfiguration(pool);
        
        return new LettuceConnectionFactory(config, clientConfig);
    }
    
    /**
     * 创建Lettuce客户端配置
     */
    private LettucePoolingClientConfiguration createLettuceClientConfiguration(CacheProperties.RedisCache.Pool pool) {
        // 创建连接池配置
        GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(pool.getMaxActive());
        poolConfig.setMaxIdle(pool.getMaxIdle());
        poolConfig.setMinIdle(pool.getMinIdle());
        poolConfig.setMaxWaitMillis(pool.getMaxWait().toMillis());
        poolConfig.setTimeBetweenEvictionRunsMillis(pool.getTimeBetweenEvictionRuns().toMillis());
        poolConfig.setMinEvictableIdleTimeMillis(pool.getMinEvictableIdleTime().toMillis());
        // 注意：GenericObjectPoolConfig没有setMaxEvictableIdleTimeMillis方法
        poolConfig.setTestOnBorrow(pool.isTestOnBorrow());
        poolConfig.setTestOnReturn(pool.isTestOnReturn());
        poolConfig.setTestWhileIdle(pool.isTestWhileIdle());
        
        // 创建Lettuce客户端配置
        return LettucePoolingClientConfiguration.builder()
                .commandTimeout(cacheProperties.getRedisCache().getConnectionTimeout())
                .shutdownTimeout(Duration.ofSeconds(2))
                .poolConfig(poolConfig)
                .build();
    }
} 