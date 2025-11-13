package com.indigo.cache.core;

import com.indigo.cache.core.constants.SessionCacheConstants;
import com.indigo.cache.infrastructure.CaffeineCacheManager;
import com.indigo.cache.infrastructure.RedisService;
import com.indigo.cache.manager.CacheKeyGenerator;
import com.indigo.cache.session.SessionManager;
import com.indigo.cache.session.CachePermissionManager;
import com.indigo.core.context.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 会话缓存预热服务
 * 在应用启动后，从 Redis 加载活跃的用户会话和权限数据到 Caffeine 本地缓存
 * 
 * <p>预热策略：
 * <ul>
 *   <li>只预热活跃的会话（TTL > 阈值）</li>
 *   <li>异步预热，不阻塞应用启动</li>
 *   <li>可配置预热数量限制，避免内存溢出</li>
 *   <li>支持分批预热，控制 Redis 压力</li>
 * </ul>
 *
 * @author 史偕成
 * @date 2025/01/15
 */
@Slf4j
public class SessionCacheWarmupService {

    /**
     * 会话缓存名称
     * 注意：所有缓存名称常量已迁移到 SessionCacheConstants
     */

    /**
     * 默认配置
     */
    private static final int DEFAULT_MAX_WARMUP_COUNT = 1000; // 最多预热 1000 个会话
    private static final int DEFAULT_MIN_TTL_SECONDS = 300; // 只预热剩余 TTL > 5 分钟的会话
    private static final int DEFAULT_BATCH_SIZE = 50; // 每批预热 50 个
    private static final int DEFAULT_THREAD_POOL_SIZE = 4; // 预热线程池大小

    private final CacheService cacheService;
    private final CacheKeyGenerator keyGenerator;
    private final CaffeineCacheManager caffeineCacheManager;
    private final RedisService redisService;

    /**
     * 预热配置
     */
    private final int maxWarmupCount;
    private final int minTtlSeconds;
    private final int batchSize;
    private final boolean enabled;

    @SuppressWarnings("unused")
    public SessionCacheWarmupService(
            CacheService cacheService,
            CacheKeyGenerator keyGenerator,
            SessionManager sessionManager,
            CachePermissionManager permissionManager,
            CaffeineCacheManager caffeineCacheManager,
            RedisService redisService) {
        this(cacheService, keyGenerator, caffeineCacheManager, redisService, 
             true, DEFAULT_MAX_WARMUP_COUNT, DEFAULT_MIN_TTL_SECONDS, DEFAULT_BATCH_SIZE);
    }

    public SessionCacheWarmupService(
            CacheService cacheService,
            CacheKeyGenerator keyGenerator,
            CaffeineCacheManager caffeineCacheManager,
            RedisService redisService,
            boolean enabled,
            int maxWarmupCount,
            int minTtlSeconds,
            int batchSize) {
        this.cacheService = cacheService;
        this.keyGenerator = keyGenerator;
        this.caffeineCacheManager = caffeineCacheManager;
        this.redisService = redisService;
        this.enabled = enabled;
        this.maxWarmupCount = maxWarmupCount;
        this.minTtlSeconds = minTtlSeconds;
        this.batchSize = batchSize;
    }

    /**
     * 应用启动后预热缓存
     * 使用 @Order 确保在其他启动任务之后执行
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(1000) // 在主要服务启动后执行
    public void warmupCache() {
        if (!enabled) {
            log.info("会话缓存预热已禁用");
            return;
        }

        if (caffeineCacheManager == null) {
            log.debug("CaffeineCacheManager 未启用，跳过会话缓存预热");
            return;
        }

        log.debug("开始预热会话缓存...");
        
        // 异步预热，不阻塞应用启动
        CompletableFuture.runAsync(() -> {
            try {
                doWarmup();
                log.debug("会话缓存预热完成");
            } catch (Exception e) {
                log.error("会话缓存预热失败", e);
            }
        });
    }

    /**
     * 执行预热逻辑
     */
    private void doWarmup() {
        try {
            // 1. 扫描所有会话 key
            String sessionPattern = keyGenerator.generate(CacheKeyGenerator.Module.USER, "session", "*");
            Set<String> sessionKeys = redisService.scan(sessionPattern);
            
            if (sessionKeys.isEmpty()) {
                log.debug("未找到需要预热的会话数据");
                return;
            }

            log.debug("找到 {} 个会话，开始预热（最多预热 {} 个，TTL > {} 秒）",
                    sessionKeys.size(), maxWarmupCount, minTtlSeconds);

            // 2. 过滤活跃会话（TTL > 阈值）
            List<WarmupItem> warmupItems = sessionKeys.stream()
                    .map(key -> {
                        String token = extractTokenFromKey(key);
                        if (token == null) {
                            return null;
                        }
                        long ttl = cacheService.getTimeToLive(key);
                        return new WarmupItem(token, key, ttl);
                    })
                    .filter(item -> item != null && item.ttl > minTtlSeconds)
                    .sorted((a, b) -> Long.compare(b.ttl, a.ttl)) // 按 TTL 降序排序，优先预热剩余时间长的
                    .limit(maxWarmupCount)
                    .toList();

            if (warmupItems.isEmpty()) {
                log.info("未找到符合条件的活跃会话（TTL > {} 秒）", minTtlSeconds);
                return;
            }

            log.info("准备预热 {} 个活跃会话", warmupItems.size());

            // 3. 分批预热
            ExecutorService executor = Executors.newFixedThreadPool(DEFAULT_THREAD_POOL_SIZE);
            try {
                int totalCount = warmupItems.size();
                int successCount = 0;
                int failCount = 0;

                for (int i = 0; i < warmupItems.size(); i += batchSize) {
                    int end = Math.min(i + batchSize, warmupItems.size());
                    List<WarmupItem> batch = warmupItems.subList(i, end);

                    // 提交批次任务
                    CompletableFuture<Integer> batchFuture = CompletableFuture.supplyAsync(() -> {
                        int batchSuccess = 0;
                        for (WarmupItem item : batch) {
                            try {
                                warmupSession(item.token, item.sessionKey, item.ttl);
                                batchSuccess++;
                            } catch (Exception e) {
                                log.warn("预热会话失败: token={}", item.token, e);
                            }
                        }
                        return batchSuccess;
                    }, executor);

                    // 等待批次完成（带超时）
                    try {
                        int batchSuccess = batchFuture.get(30, TimeUnit.SECONDS);
                        successCount += batchSuccess;
                        failCount += (batch.size() - batchSuccess);
                        log.debug("批次预热完成: {}/{}, 成功: {}, 失败: {}", 
                                end, totalCount, batchSuccess, batch.size() - batchSuccess);
                    } catch (Exception e) {
                        log.warn("批次预热超时或失败", e);
                        failCount += batch.size();
                    }
                }

                log.info("会话缓存预热完成: 总计={}, 成功={}, 失败={}", 
                        totalCount, successCount, failCount);
            } finally {
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        } catch (Exception e) {
            log.error("预热过程发生异常", e);
        }
    }

    /**
     * 预热单个会话
     */
    private void warmupSession(String token, String sessionKey, long ttl) {
        try {
            // 1. 预热用户会话
            UserContext userContext = cacheService.getObject(sessionKey, UserContext.class);
            if (userContext != null) {
                int localExpireSeconds = calculateLocalCacheExpire(ttl);
                caffeineCacheManager.put(SessionCacheConstants.CACHE_NAME_USER_SESSION, token, userContext, localExpireSeconds);
            }

            // 2. 预热用户权限
            String permissionsKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "permissions", token);
            long permissionsTtl = cacheService.getTimeToLive(permissionsKey);
            if (permissionsTtl > 0) {
                @SuppressWarnings("unchecked")
                List<String> permissions = cacheService.getObject(permissionsKey, List.class);
                if (permissions != null) {
                    int localExpireSeconds = calculateLocalCacheExpire(permissionsTtl);
                    caffeineCacheManager.put(SessionCacheConstants.CACHE_NAME_USER_PERMISSIONS, token, permissions, localExpireSeconds);
                }
            }

            // 3. 预热用户角色
            String rolesKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "roles", token);
            long rolesTtl = cacheService.getTimeToLive(rolesKey);
            if (rolesTtl > 0) {
                @SuppressWarnings("unchecked")
                List<String> roles = cacheService.getObject(rolesKey, List.class);
                if (roles != null) {
                    int localExpireSeconds = calculateLocalCacheExpire(rolesTtl);
                    caffeineCacheManager.put(SessionCacheConstants.CACHE_NAME_USER_ROLES, token, roles, localExpireSeconds);
                }
            }

            log.debug("预热会话成功: token={}, ttl={}s", token, ttl);
        } catch (Exception e) {
            log.warn("预热会话失败: token={}", token, e);
            throw e;
        }
    }

    /**
     * 从 session key 中提取 token
     */
    private String extractTokenFromKey(String key) {
        try {
            String prefix = keyGenerator.generate(CacheKeyGenerator.Module.USER, "session", "");
            if (key.startsWith(prefix)) {
                return key.substring(prefix.length());
            }
            // 如果格式不匹配，尝试从最后一个冒号后提取
            int lastIndex = key.lastIndexOf(":");
            if (lastIndex > 0 && lastIndex < key.length() - 1) {
                return key.substring(lastIndex + 1);
            }
            return null;
        } catch (Exception e) {
            log.warn("从 key 中提取 token 失败: key={}", key, e);
            return null;
        }
    }

    /**
     * 计算本地缓存过期时间
     */
    private int calculateLocalCacheExpire(long redisTtl) {
        // 本地缓存过期时间 = min(Redis过期时间的1/10, 5分钟)
        long localExpire = Math.min(redisTtl / 10, 300);
        // 确保至少 1 分钟
        return (int) Math.max(localExpire, 60);
    }

    /**
     * 预热项
     */
    private static class WarmupItem {
        final String token;
        final String sessionKey;
        final long ttl;

        WarmupItem(String token, String sessionKey, long ttl) {
            this.token = token;
            this.sessionKey = sessionKey;
            this.ttl = ttl;
        }
    }
}

