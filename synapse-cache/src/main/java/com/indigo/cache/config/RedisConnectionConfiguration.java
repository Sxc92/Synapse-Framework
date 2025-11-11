package com.indigo.cache.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;

import java.time.Duration;

/**
 * Redis连接工厂配置类
 * 支持单机、集群、哨兵模式
 * 使用synapse.cache.redis前缀配置创建RedisTemplate
 * 
 * @author 史偕成
 * @date 2025/01/08
 */
@Slf4j
@Configuration
@ConditionalOnClass(RedisConnectionFactory.class)
@AutoConfigureBefore(RedisAutoConfiguration.class)
public class RedisConnectionConfiguration {
    
    @Autowired
    private CacheProperties cacheProperties;
    
    /**
     * 创建Redis连接工厂
     * 使用synapse.cache.redis配置
     * 使用 @Primary 确保此 Bean 优先于 Spring Boot 自动配置的 Bean
     * 使用 @AutoConfigureBefore 确保此配置在 RedisAutoConfiguration 之前加载
     */
    @Bean("redisConnectionFactory")
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        RedisConnectionFactory factory;
        
        if (cacheProperties.getRedisCache().getCluster().isEnabled()) {
            factory = createClusterConnectionFactory();
            log.info("使用synapse.cache.redis配置创建Redis集群连接工厂");
        } else if (cacheProperties.getRedisCache().getSentinel().isEnabled()) {
            factory = createSentinelConnectionFactory();
            log.info("使用synapse.cache.redis配置创建Redis哨兵连接工厂");
        } else {
            factory = createStandaloneConnectionFactory();
            log.info("使用synapse.cache.redis配置创建Redis单机连接工厂");
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
        CacheProperties.RedisCache.Connection conn = cacheProperties.getRedisCache().getConnection();
        
        RedisSentinelConfiguration config = new RedisSentinelConfiguration();
        config.master(sentinel.getMaster());
        
        // 设置数据库索引（重要！）
        config.setDatabase(conn.getDatabase());
        
        // 将String数组转换为RedisNode列表
        for (String node : sentinel.getNodes()) {
            String[] parts = node.split(":");
            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 26379;
            config.sentinel(host, port);
        }
        
        // 设置哨兵密码（如果有）
        if (sentinel.getPassword() != null && !sentinel.getPassword().isEmpty()) {
            config.setSentinelPassword(sentinel.getPassword());
        }
        
        // 设置主节点密码（重要！这是 Redis 主节点的密码，不是哨兵密码）
        if (conn.getPassword() != null && !conn.getPassword().isEmpty()) {
            config.setPassword(conn.getPassword());
        }
        
        LettucePoolingClientConfiguration clientConfig = createLettuceClientConfiguration(pool);
        
        log.info("创建哨兵连接工厂 - Master: {}, Database: {}, Sentinel Nodes: {}", 
                sentinel.getMaster(), conn.getDatabase(), sentinel.getNodes().length);
        
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
    @SuppressWarnings("deprecation")
    private LettucePoolingClientConfiguration createLettuceClientConfiguration(CacheProperties.RedisCache.Pool pool) {
        // 创建连接池配置
        @SuppressWarnings("rawtypes")
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxTotal(pool.getMaxActive());
        poolConfig.setMaxIdle(pool.getMaxIdle());
        poolConfig.setMinIdle(pool.getMinIdle());
        // 使用新的 Duration API（替代已废弃的 setMaxWaitMillis）
        poolConfig.setMaxWait(pool.getMaxWait());
        // 使用新的 Duration API（替代已废弃的 setTimeBetweenEvictionRunsMillis）
        poolConfig.setTimeBetweenEvictionRuns(pool.getTimeBetweenEvictionRuns());
        // 使用新的 Duration API（替代已废弃的 setMinEvictableIdleTimeMillis）
        // 注意：setMinEvictableIdleTime(Duration) 在新版本中也被标记为废弃，但暂无替代方法
        poolConfig.setMinEvictableIdleTime(pool.getMinEvictableIdleTime());
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