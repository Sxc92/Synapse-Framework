package com.indigo.cache.config;

import com.indigo.cache.aspect.CacheAspect;
import com.indigo.cache.manager.CacheKeyGenerator;
import com.indigo.cache.core.CacheService;
import com.indigo.cache.infrastructure.CaffeineCacheManager;
import com.indigo.cache.infrastructure.RedisService;
import com.indigo.cache.core.TwoLevelCacheService;
import com.indigo.cache.extension.ratelimit.RateLimitService;
import com.indigo.cache.session.UserSessionService;
import com.indigo.cache.session.UserSessionServiceFactory;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.indigo.cache.session.SessionManager;
import com.indigo.cache.session.CachePermissionManager;
import com.indigo.cache.session.StatisticsManager;

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
        log.info("创建RedisService Bean，RedisTemplate: {}, StringRedisTemplate: {}, JsonUtils: {}",
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
     */
    @Bean
    @ConditionalOnMissingBean
    public UserSessionService userSessionService(
            CacheService cacheService,
            @Qualifier("synapseCacheKeyGenerator") CacheKeyGenerator cacheKeyGenerator,
            RedisService redisService) {
        log.info("创建UserSessionService Bean - 使用新的session包架构");
        return UserSessionServiceFactory.createUserSessionService(cacheService, cacheKeyGenerator, redisService);
    }

    /**
     * 注册会话管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public SessionManager sessionManager(
            CacheService cacheService,
            @Qualifier("synapseCacheKeyGenerator") CacheKeyGenerator cacheKeyGenerator) {
        log.info("创建SessionManager Bean");
        return UserSessionServiceFactory.createSessionManager(cacheService, cacheKeyGenerator);
    }

    /**
     * 注册缓存权限管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public CachePermissionManager cachePermissionManager(
            CacheService cacheService,
            @Qualifier("synapseCacheKeyGenerator") CacheKeyGenerator cacheKeyGenerator) {
        log.info("创建CachePermissionManager Bean");
        return UserSessionServiceFactory.createPermissionManager(cacheService, cacheKeyGenerator);
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
        log.info("创建StatisticsManager Bean");
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
        log.info("创建RateLimitService Bean");
        return new RateLimitService(redisService, cacheKeyGenerator);
    }


} 