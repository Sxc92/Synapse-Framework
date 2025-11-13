package com.indigo.cache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 缓存配置属性类
 * 支持缓存预热、穿透防护、异常处理等配置
 *
 * @author 史偕成
 * @date 2025/01/08
 */
@Data
@ConfigurationProperties(prefix = "synapse.cache")
public class CacheProperties {

    /**
     * 是否启用缓存功能
     */
    private boolean enabled = true;

    /**
     * 默认缓存策略
     */
    private String defaultStrategy = "LOCAL_AND_REDIS";

    /**
     * 本地缓存配置
     */
    private LocalCache localCache = new LocalCache();

    /**
     * Redis缓存配置
     */
    private RedisCache redisCache = new RedisCache();

    /**
     * 缓存预热配置
     */
    private Warmup warmup = new Warmup();

    /**
     * 会话缓存预热配置
     */
    private SessionWarmup sessionWarmup = new SessionWarmup();

    /**
     * 缓存穿透防护配置
     */
    private PenetrationProtection penetrationProtection = new PenetrationProtection();

    /**
     * 异常处理配置
     */
    private ExceptionHandling exceptionHandling = new ExceptionHandling();

    /**
     * 健康检查配置
     */
    private HealthCheck healthCheck = new HealthCheck();

    /**
     * 模块特定配置
     */
    private Map<String, ModuleConfig> modules = new HashMap<>();

    /**
     * 本地缓存配置
     */
    @Data
    public static class LocalCache {
        /**
         * 最大缓存条目数
         */
        private long maximumSize = 10000;

        /**
         * 默认过期时间
         */
        private Duration expireAfterWrite = Duration.ofMinutes(30);

        /**
         * 访问后过期时间
         */
        private Duration expireAfterAccess = Duration.ofMinutes(10);

        /**
         * 刷新时间
         */
        private Duration refreshAfterWrite = Duration.ofMinutes(5);

        /**
         * 是否启用统计
         */
        private boolean enableStats = true;

        /**
         * 是否启用记录
         */
        private boolean enableRecord = false;
    }

    /**
     * Redis缓存配置
     */
    @Data
    public static class RedisCache {
        /**
         * 是否启用Redis缓存
         */
        private boolean enabled = true;
        
        /**
         * Redis键前缀
         */
        private String keyPrefix = "synapse";
        
        /**
         * 默认过期时间
         */
        private Duration defaultExpire = Duration.ofHours(1);

        /**
         * Redis连接配置
         */
        private Connection connection = new Connection();
        
        /**
         * Redis集群配置
         */
        private Cluster cluster = new Cluster();
        
        /**
         * Redis哨兵配置
         */
        private Sentinel sentinel = new Sentinel();
        
        /**
         * Redis连接池配置
         */
        private Pool pool = new Pool();

        /**
         * 连接超时时间
         */
        private Duration connectionTimeout = Duration.ofSeconds(5);

        /**
         * 读取超时时间
         */
        private Duration readTimeout = Duration.ofSeconds(3);

        /**
         * 写入超时时间
         */
        private Duration writeTimeout = Duration.ofSeconds(3);

        /**
         * 最大重试次数
         */
        private int maxRetries = 3;

        /**
         * 重试间隔
         */
        private Duration retryInterval = Duration.ofMillis(100);

        /**
         * 是否启用压缩
         */
        private boolean enableCompression = false;

        /**
         * 压缩阈值（字节）
         */
        private int compressionThreshold = 1024;
        
        /**
         * Redis连接配置
         */
        @Data
        public static class Connection {
            /**
             * Redis主机地址
             */
            private String host = "localhost";
            
            /**
             * Redis端口
             */
            private int port = 6379;
            
            /**
             * Redis数据库索引
             */
            private int database = 0;
            
            /**
             * Redis密码
             */
            private String password;
            
            /**
             * 连接超时时间
             */
            private Duration timeout = Duration.ofSeconds(2);
            
            /**
             * 是否启用SSL
             */
            private boolean ssl = false;
            
            /**
             * 连接名称
             */
            private String clientName;
        }
        
        /**
         * Redis集群配置
         */
        @Data
        public static class Cluster {
            /**
             * 是否启用集群模式
             */
            private boolean enabled = false;
            
            /**
             * 集群节点列表
             */
            private String[] nodes = {};
            
            /**
             * 集群最大重定向次数
             */
            private int maxRedirects = 5;
            
            /**
             * 集群刷新周期
             */
            private Duration refreshPeriod = Duration.ofSeconds(30);
        }
        
        /**
         * Redis哨兵配置
         */
        @Data
        public static class Sentinel {
            /**
             * 是否启用哨兵模式
             */
            private boolean enabled = false;
            
            /**
             * 哨兵节点列表
             */
            private String[] nodes = {};
            
            /**
             * 主节点名称
             */
            private String master = "mymaster";
            
            /**
             * 哨兵密码
             */
            private String password;
        }
        
        /**
         * Redis连接池配置
         */
        @Data
        public static class Pool {
            /**
             * 最大活跃连接数
             */
            private int maxActive = 8;
            
            /**
             * 最大空闲连接数
             */
            private int maxIdle = 8;
            
            /**
             * 最小空闲连接数
             */
            private int minIdle = 0;
            
            /**
             * 最大等待时间
             */
            private Duration maxWait = Duration.ofMillis(-1);
            
            /**
             * 空闲连接检测间隔
             */
            private Duration timeBetweenEvictionRuns = Duration.ofSeconds(30);
            
            /**
             * 空闲连接最小空闲时间
             */
            private Duration minEvictableIdleTime = Duration.ofMinutes(10);
            
            /**
             * 空闲连接最大空闲时间
             */
            private Duration maxEvictableIdleTime = Duration.ofMinutes(30);
            
            /**
             * 连接测试查询
             */
            private String testQuery = "SELECT 1";
            
            /**
             * 空闲时是否测试连接
             */
            private boolean testWhileIdle = true;
            
            /**
             * 借用时是否测试连接
             */
            private boolean testOnBorrow = false;
            
            /**
             * 归还时是否测试连接
             */
            private boolean testOnReturn = false;
        }
    }

    /**
     * 缓存预热配置
     */
    @Data
    public static class Warmup {
        /**
         * 是否启用预热
         */
        private boolean enabled = false;

        /**
         * 预热线程池大小
         */
        private int threadPoolSize = 5;

        /**
         * 预热超时时间
         */
        private Duration timeout = Duration.ofMinutes(5);

        /**
         * 预热批次大小
         */
        private int batchSize = 100;

        /**
         * 预热间隔
         */
        private Duration interval = Duration.ofHours(1);

        /**
         * 预热数据源配置
         */
        private Map<String, String> dataSources = new HashMap<>();
    }

    /**
     * 缓存穿透防护配置
     */
    @Data
    public static class PenetrationProtection {
        /**
         * 是否启用穿透防护
         */
        private boolean enabled = true;

        /**
         * 空值缓存过期时间
         */
        private Duration nullValueExpire = Duration.ofMinutes(5);

        /**
         * 布隆过滤器大小
         */
        private long bloomFilterSize = 1000000;

        /**
         * 布隆过滤器误判率
         */
        private double bloomFilterFalsePositiveRate = 0.01;

        /**
         * 是否启用布隆过滤器
         */
        private boolean enableBloomFilter = false;

        /**
         * 限流配置
         */
        private RateLimit rateLimit = new RateLimit();
    }

    /**
     * 限流配置
     */
    @Data
    public static class RateLimit {
        /**
         * 是否启用限流
         */
        private boolean enabled = true;

        /**
         * 每秒请求数限制
         */
        private int requestsPerSecond = 100;

        /**
         * 突发请求数限制
         */
        private int burstRequests = 200;

        /**
         * 限流窗口大小
         */
        private Duration windowSize = Duration.ofSeconds(1);
    }

    /**
     * 异常处理配置
     */
    @Data
    public static class ExceptionHandling {
        /**
         * 是否启用异常处理
         */
        private boolean enabled = true;

        /**
         * 是否记录异常日志
         */
        private boolean logExceptions = true;

        /**
         * 是否抛出异常
         */
        private boolean throwExceptions = false;

        /**
         * 异常重试次数
         */
        private int retryCount = 3;

        /**
         * 异常重试间隔
         */
        private Duration retryInterval = Duration.ofMillis(100);

        /**
         * 降级策略
         */
        private String fallbackStrategy = "RETURN_NULL";
    }

    /**
     * 健康检查配置
     */
    @Data
    public static class HealthCheck {
        /**
         * 是否启用健康检查
         */
        private boolean enabled = true;

        /**
         * 健康检查间隔
         */
        private Duration interval = Duration.ofSeconds(30);

        /**
         * 健康检查超时时间
         */
        private Duration timeout = Duration.ofSeconds(5);

        /**
         * 失败阈值
         */
        private int failureThreshold = 3;

        /**
         * 恢复阈值
         */
        private int recoveryThreshold = 2;

        /**
         * 是否启用详细检查
         */
        private boolean detailedCheck = false;
    }

    /**
     * 模块特定配置
     */
    @Data
    public static class ModuleConfig {
        /**
         * 缓存策略
         */
        private String strategy = "LOCAL_AND_REDIS";

        /**
         * 过期时间
         */
        private Duration expire = Duration.ofMinutes(30);

        /**
         * 最大大小
         */
        private long maximumSize = 1000;

        /**
         * 是否启用预热
         */
        private boolean enableWarmup = false;

        /**
         * 是否启用穿透防护
         */
        private boolean enablePenetrationProtection = true;

        /**
         * 是否启用异常处理
         */
        private boolean enableExceptionHandling = true;
    }

    /**
     * 获取模块配置
     *
     * @param moduleName 模块名
     * @return 模块配置
     */
    public ModuleConfig getModuleConfig(String moduleName) {
        return modules.getOrDefault(moduleName, new ModuleConfig());
    }

    /**
     * 设置模块配置
     *
     * @param moduleName 模块名
     * @param config     配置
     */
    public void setModuleConfig(String moduleName, ModuleConfig config) {
        modules.put(moduleName, config);
    }

    /**
     * 会话缓存预热配置
     */
    @Data
    public static class SessionWarmup {
        /**
         * 是否启用会话缓存预热
         */
        private boolean enabled = true;

        /**
         * 最多预热的会话数量
         */
        private int maxCount = 1000;

        /**
         * 最小 TTL（秒），只预热剩余时间大于此值的会话
         */
        private int minTtlSeconds = 300; // 5分钟

        /**
         * 批次大小
         */
        private int batchSize = 50;

        /**
         * 预热线程池大小
         */
        private int threadPoolSize = 4;
    }
} 