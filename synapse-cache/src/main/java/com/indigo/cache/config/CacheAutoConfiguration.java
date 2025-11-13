package com.indigo.cache.config;

import com.indigo.cache.aspect.CacheAspect;
import com.indigo.cache.core.*;
import com.indigo.cache.extension.ratelimit.RateLimitService;
import com.indigo.cache.infrastructure.CaffeineCacheManager;
import com.indigo.cache.infrastructure.RedisService;
import com.indigo.cache.manager.CacheKeyGenerator;
import com.indigo.cache.session.*;
import com.indigo.cache.session.impl.DefaultCachePermissionManager;
import com.indigo.cache.session.impl.DefaultSessionManager;
import com.indigo.core.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 缓存自动配置类，用于自动注册缓存服务
 * 使用自定义的 Redis 配置，排除 Spring Boot 的默认 Redis 自动配置
 * <p>
 * 注意：此配置类排除了 Spring Boot 的 RedisAutoConfiguration，使用我们自定义的 RedisConnectionConfiguration
 *
 * @author 史偕成
 * @date 2025/05/16 10:30
 */
@Slf4j
@AutoConfiguration
@Import({RedisConnectionConfiguration.class, RedisConfiguration.class})
@EnableConfigurationProperties(CacheProperties.class)
@ComponentScan(basePackages = {"com.indigo.cache.aspect", "com.indigo.cache.core", "com.indigo.core"})
public class CacheAutoConfiguration {

    /**
     * 注册缓存键生成器
     */
    @Bean("synapseCacheKeyGenerator")
    @ConditionalOnMissingBean
    public CacheKeyGenerator synapseCacheKeyGenerator() {
        return new CacheKeyGenerator();
    }

    /**
     * 注册Caffeine缓存管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public CaffeineCacheManager caffeineCacheManager() {
        return new CaffeineCacheManager();
    }

    /**
     * 注册Redis缓存服务
     * 使用@Qualifier指定具体的RedisTemplate bean
     * 注入 JsonUtils 用于 JSON 序列化/反序列化（复用 core 模块的工具类）
     */
    @Bean
    @ConditionalOnMissingBean
    @SuppressWarnings("unchecked")
    public RedisService redisService(@Qualifier("redisTemplate") @SuppressWarnings("rawtypes") RedisTemplate redisTemplate,
                                     @Qualifier("stringRedisTemplate") StringRedisTemplate stringRedisTemplate,
                                     @Autowired(required = false) JsonUtils jsonUtils) {
        log.debug("创建RedisService Bean，RedisTemplate: {}, StringRedisTemplate: {}, JsonUtils: {}",
                redisTemplate != null ? redisTemplate.getClass().getSimpleName() : "null",
                stringRedisTemplate != null ? stringRedisTemplate.getClass().getSimpleName() : "null",
                jsonUtils != null ? "已注入" : "未注入（将使用静态方法）");

        // 类型转换为我们需要的泛型类型
        RedisTemplate<String, Object> typedRedisTemplate = (RedisTemplate<String, Object>) redisTemplate;
        return new RedisService(typedRedisTemplate, stringRedisTemplate, jsonUtils);
    }

    /**
     * 注册缓存服务
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheService cacheService(RedisService redisService) {
        return new CacheService(redisService);
    }

    /**
     * 注册二级缓存服务
     */
    @Bean
    @ConditionalOnMissingBean
    public TwoLevelCacheService twoLevelCacheService(
            CaffeineCacheManager caffeineCacheManager,
            CacheService cacheService,
            @Qualifier("synapseCacheKeyGenerator") CacheKeyGenerator cacheKeyGenerator) {
        return new TwoLevelCacheService(caffeineCacheManager, cacheService, cacheKeyGenerator);
    }

    /**
     * 注册缓存切面
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheAspect cacheAspect(TwoLevelCacheService twoLevelCacheService) {
        return new CacheAspect(twoLevelCacheService);
    }

    /**
     * 注册用户会话服务
     * 使用工厂模式创建，完全依赖接口
     * 注意：Caffeine 二级缓存由底层的 SessionManager 和 CachePermissionManager 处理
     */
    @Bean
    @ConditionalOnMissingBean
    public UserSessionService userSessionService(
            SessionManager sessionManager,
            CachePermissionManager cachePermissionManager,
            StatisticsManager statisticsManager) {
        log.debug("创建UserSessionService Bean - 使用新的session包架构");
        return new UserSessionService(sessionManager, cachePermissionManager, statisticsManager);
    }

    /**
     * 注册缓存失效追踪器
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheInvalidationTracker cacheInvalidationTracker() {
        log.debug("创建CacheInvalidationTracker Bean - 缓存失效追踪器");
        return new CacheInvalidationTracker();
    }

    /**
     * 注册缓存失效通知服务
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheInvalidationService cacheInvalidationService(
            RedisService redisService,
            RedisConnectionFactory connectionFactory) {
        log.debug("创建CacheInvalidationService Bean - 缓存失效通知服务");
        return new CacheInvalidationService(redisService, connectionFactory);
    }

    /**
     * 注册本地缓存失效监听器
     */
    @Bean
    @ConditionalOnMissingBean
    public LocalCacheInvalidationListener localCacheInvalidationListener(
            @Autowired(required = false) CaffeineCacheManager caffeineCacheManager,
            CacheInvalidationService cacheInvalidationService,
            CacheInvalidationTracker invalidationTracker) {
        if (caffeineCacheManager == null) {
            log.debug("CaffeineCacheManager 未启用，跳过 LocalCacheInvalidationListener 注册");
            return null;
        }
        log.debug("创建LocalCacheInvalidationListener Bean - 本地缓存失效监听器");
        LocalCacheInvalidationListener listener = new LocalCacheInvalidationListener(
                caffeineCacheManager, invalidationTracker);
        cacheInvalidationService.registerListener(listener);
        return listener;
    }

    /**
     * 注册会话管理器
     * 支持 Caffeine 二级缓存和缓存失效通知（如果可用）
     */
    @Bean
    @ConditionalOnMissingBean
    public SessionManager sessionManager(
            CacheService cacheService,
            @Qualifier("synapseCacheKeyGenerator") CacheKeyGenerator cacheKeyGenerator,
            @Autowired(required = false) CaffeineCacheManager caffeineCacheManager,
            @Autowired(required = false) CacheInvalidationService cacheInvalidationService,
            @Autowired(required = false) CacheInvalidationTracker invalidationTracker) {
        log.debug("创建SessionManager Bean，Caffeine缓存: {}, 失效通知: {}, 失效追踪: {}", 
                caffeineCacheManager != null ? "启用" : "未启用",
                cacheInvalidationService != null ? "启用" : "未启用",
                invalidationTracker != null ? "启用" : "未启用");
        return new DefaultSessionManager(
                cacheService, cacheKeyGenerator, caffeineCacheManager, cacheInvalidationService, invalidationTracker);
    }

    /**
     * 注册缓存权限管理器
     * 支持 Caffeine 二级缓存和缓存失效通知（如果可用）
     */
    @Bean
    @ConditionalOnMissingBean
    public CachePermissionManager cachePermissionManager(
            CacheService cacheService,
            @Qualifier("synapseCacheKeyGenerator") CacheKeyGenerator cacheKeyGenerator,
            @Autowired(required = false) CaffeineCacheManager caffeineCacheManager,
            @Autowired(required = false) CacheInvalidationService cacheInvalidationService,
            @Autowired(required = false) CacheInvalidationTracker invalidationTracker) {
        log.debug("创建CachePermissionManager Bean，Caffeine缓存: {}, 失效通知: {}, 失效追踪: {}", 
                caffeineCacheManager != null ? "启用" : "未启用",
                cacheInvalidationService != null ? "启用" : "未启用",
                invalidationTracker != null ? "启用" : "未启用");
        return new DefaultCachePermissionManager(
                cacheService, cacheKeyGenerator, caffeineCacheManager, cacheInvalidationService, invalidationTracker);
    }

    /**
     * 注册统计管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public StatisticsManager statisticsManager(
            CacheService cacheService,
            @Qualifier("synapseCacheKeyGenerator") CacheKeyGenerator cacheKeyGenerator,
            RedisService redisService,
            SessionManager sessionManager,
            CachePermissionManager permissionManager) {
        log.debug("创建StatisticsManager Bean");
        return UserSessionServiceFactory.createStatisticsManager(
                cacheService, cacheKeyGenerator, redisService, sessionManager, permissionManager);
    }

    /**
     * 注册限流服务
     */
    @Bean
    @ConditionalOnMissingBean
    public RateLimitService rateLimitService(
            RedisService redisService,
            @Qualifier("synapseCacheKeyGenerator") CacheKeyGenerator cacheKeyGenerator) {
        log.debug("创建RateLimitService Bean");
        return new RateLimitService(redisService, cacheKeyGenerator);
    }

    /**
     * 注册会话缓存预热服务
     * 在应用启动后，从 Redis 加载活跃的用户会话和权限数据到 Caffeine 本地缓存
     */
    @Bean
    @ConditionalOnMissingBean
    public SessionCacheWarmupService sessionCacheWarmupService(
            CacheService cacheService,
            @Qualifier("synapseCacheKeyGenerator") CacheKeyGenerator cacheKeyGenerator,
            SessionManager sessionManager,
            CachePermissionManager permissionManager,
            @Autowired(required = false) CaffeineCacheManager caffeineCacheManager,
            RedisService redisService,
            CacheProperties cacheProperties) {
        if (caffeineCacheManager == null) {
            log.debug("CaffeineCacheManager 未启用，跳过 SessionCacheWarmupService 注册");
            return null;
        }
        
        CacheProperties.SessionWarmup warmupConfig = cacheProperties.getSessionWarmup();
        log.debug("创建SessionCacheWarmupService Bean，预热配置: enabled={}, maxCount={}, minTtlSeconds={}, batchSize={}",
                warmupConfig.isEnabled(), warmupConfig.getMaxCount(), 
                warmupConfig.getMinTtlSeconds(), warmupConfig.getBatchSize());
        
        return new SessionCacheWarmupService(
                cacheService,
                cacheKeyGenerator,
                caffeineCacheManager,
                redisService,
                warmupConfig.isEnabled(),
                warmupConfig.getMaxCount(),
                warmupConfig.getMinTtlSeconds(),
                warmupConfig.getBatchSize()
        );
    }

} 