package com.indigo.databases.config;

import com.indigo.databases.enums.DatabaseType;
import com.indigo.databases.enums.FieldConversionStrategyType;
import com.indigo.databases.enums.PoolType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Synapse Framework 数据源配置属性类
 * 统一配置入口，支持读写分离、负载均衡、故障转移
 *
 * @author 史偕成
 * @date 2025/01/08
 */
@Data
@ConfigurationProperties(prefix = "synapse.datasource", ignoreUnknownFields = true)
public class SynapseDataSourceProperties {

    /**
     * MyBatis-Plus 配置
     */
    private MybatisPlus mybatisPlus = new MybatisPlus();

    /**
     * 主数据源名称
     */
    private String primary = "master";

    /**
     * 读写分离配置
     */
    private ReadWriteConfig readWrite = new ReadWriteConfig();

    /**
     * 负载均衡配置
     */
    private LoadBalanceConfig loadBalance = new LoadBalanceConfig();

    /**
     * 故障转移配置
     */
    private FailoverConfig failover = new FailoverConfig();
    
    /**
     * 健康检查配置
     */
    private HealthCheckConfig healthCheck = new HealthCheckConfig();

    /**
     * Seata分布式事务配置
     */
    private SeataConfig seata = new SeataConfig();

    /**
     * 字段转换配置
     */
    private FieldConversionConfig fieldConversion = new FieldConversionConfig();

    /**
     * 数据源配置
     */
    private Map<String, DataSourceConfig> datasources = new LinkedHashMap<>();

    /**
     * 读写分离配置
     */
    @Data
    public static class ReadWriteConfig {
        /**
         * 是否启用读写分离
         */
        private boolean enabled = false;

        /**
         * 读数据源列表
         */
        private List<String> readSources = new ArrayList<>();

        /**
         * 写数据源列表
         */
        private List<String> writeSources = new ArrayList<>();

        /**
         * 读写分离模式
         */
        private ReadWriteMode mode = ReadWriteMode.AUTO;

        /**
         * 读写分离模式枚举
         */
        public enum ReadWriteMode {
            /**
             * 自动模式：根据SQL类型自动路由
             */
            AUTO,
            /**
             * 手动模式：需要手动指定数据源
             */
            MANUAL,
            /**
             * 禁用模式：不进行读写分离
             */
            DISABLED
        }
    }

    /**
     * 负载均衡配置
     */
    @Data
    public static class LoadBalanceConfig {
        /**
         * 负载均衡策略
         */
        private LoadBalanceStrategy strategy = LoadBalanceStrategy.ROUND_ROBIN;

        /**
         * 权重配置（仅在使用WEIGHTED策略时有效）
         */
        private Map<String, Integer> weights = new LinkedHashMap<>();

        /**
         * 负载均衡策略枚举
         */
        public enum LoadBalanceStrategy {
            /**
             * 轮询策略
             */
            ROUND_ROBIN,
            /**
             * 权重策略
             */
            WEIGHTED,
            /**
             * 随机策略
             */
            RANDOM
        }
    }

    /**
     * 故障转移配置
     */
    @Data
    public static class FailoverConfig {
        /**
         * 是否启用故障转移
         */
        private boolean enabled = false;

        /**
         * 最大重试次数
         */
        private int maxRetryTimes = 3;

        /**
         * 重试间隔时间（毫秒）
         */
        private long retryInterval = 1000;

        /**
         * 故障检测间隔（毫秒）
         */
        private long detectionInterval = 5000;

        /**
         * 故障恢复检测间隔（毫秒）
         */
        private long recoveryInterval = 10000;

        /**
         * 故障转移策略
         */
        private FailoverStrategy strategy = FailoverStrategy.PRIMARY_FIRST;

        /**
         * 故障转移策略枚举
         */
        public enum FailoverStrategy {
            /**
             * 主数据源优先
             */
            PRIMARY_FIRST,
            /**
             * 健康数据源优先
             */
            HEALTHY_FIRST,
            /**
             * 轮询故障转移
             */
            ROUND_ROBIN
        }
    }

    /**
     * 数据源配置
     */
    @Data
    public static class DataSourceConfig {
        /**
         * 数据库类型
         */
        private DatabaseType type = DatabaseType.MYSQL;

        /**
         * 主机地址
         */
        private String host = "localhost";

        /**
         * 端口号
         */
        private Integer port = 3306;

        /**
         * 数据库名
         */
        private String database;

        /**
         * 用户名
         */
        private String username;

        /**
         * 密码
         */
        private String password;

        /**
         * 数据源角色
         */
//        private DataSourceRole role = DataSourceRole.READ_WRITE;

        /**
         * 权重（用于负载均衡）
         */
        private Integer weight = 100;

        /**
         * 连接池类型
         */
        private PoolType poolType = PoolType.HIKARI;

        /**
         * 连接参数
         */
        private Map<String, String> params = new LinkedHashMap<>();

        /**
         * HikariCP连接池配置
         */
        private HikariConfig hikari = new HikariConfig();

        /**
         * Druid连接池配置
         */
        private DruidConfig druid = new DruidConfig();

        /**
         * 数据源角色枚举
         */
//        public enum DataSourceRole {
//            /**
//             * 只读数据源
//             */
//            READ,
//            /**
//             * 只写数据源
//             */
//            WRITE,
//            /**
//             * 读写数据源
//             */
//            READ_WRITE
//        }

        /**
         * 获取JDBC URL
         */
        public String getUrl() {
            StringBuilder url = new StringBuilder(type.getUrlPrefix());
            
            // 添加主机和端口
            url.append(host);
            if (port != null) {
                url.append(":").append(port);
            }
            
            // 添加数据库名
            if (database != null && !database.isEmpty()) {
                if (type == DatabaseType.ORACLE) {
                    url.append(":").append(database);
                } else {
                    url.append("/").append(database);
                }
            }
            
            // 添加连接参数
            if (!params.isEmpty()) {
                url.append("?");
                params.forEach((key, value) -> url.append(key).append("=").append(value).append("&"));
                url.setLength(url.length() - 1); // 删除最后一个&
            }
            
            return url.toString();
        }

        /**
         * 获取驱动类名
         */
        public String getDriverClassName() {
            return type.getDriverClassName();
        }
    }

    /**
     * MyBatis-Plus 配置
     */
    @Data
    public static class MybatisPlus {
        /**
         * 配置
         */
        private Configuration configuration = new Configuration();
        
        /**
         * 全局配置
         */
        private GlobalConfig globalConfig = new GlobalConfig();
        
        /**
         * 类型别名包路径
         */
        private String typeAliasesPackage;
        
        /**
         * 映射器位置
         */
        private String[] mapperLocations = {"classpath*:mapper/**/*.xml"};
        
        /**
         * 配置位置
         */
        private String configLocation;
        
        /**
         * 配置
         */
        @Data
        public static class Configuration {
            /**
             * 日志实现类
             */
            private String logImpl = "org.apache.ibatis.logging.stdout.StdOutImpl";
            
            /**
             * 下划线转驼峰
             */
            private boolean mapUnderscoreToCamelCase = true;
            
            /**
             * 缓存启用
             */
            private boolean cacheEnabled = true;
            
            /**
             * 延迟加载启用
             */
            private boolean lazyLoadingEnabled = true;
            
            /**
             * 积极延迟加载
             */
            private boolean aggressiveLazyLoading = false;
            
            /**
             * 多结果集启用
             */
            private boolean multipleResultSetsEnabled = true;
            
            /**
             * 列标签使用
             */
            private boolean useColumnLabel = true;
            
            /**
             * 自动生成键
             */
            private boolean useGeneratedKeys = false;
            
            /**
             * 自动映射行为
             */
            private String autoMappingBehavior = "PARTIAL";
            
            /**
             * 自动映射未知列行为
             */
            private String autoMappingUnknownColumnBehavior = "WARNING";
            
            /**
             * 默认执行器类型
             */
            private String defaultExecutorType = "SIMPLE";
            
            /**
             * 默认语句超时
             */
            private Integer defaultStatementTimeout = 25;
            
            /**
             * 默认获取超时
             */
            private Integer defaultFetchSize = 100;
            
            /**
             * 安全行边界启用
             */
            private boolean safeRowBoundsEnabled = false;
            
            /**
             * 安全结果处理启用
             */
            private boolean safeResultHandlerEnabled = true;
            
            /**
             * 本地缓存作用域
             */
            private String localCacheScope = "SESSION";
            
            /**
             * 延迟加载触发方法
             */
            private String lazyLoadTriggerMethods = "equals,clone,hashCode,toString";
        }
        
        /**
         * 全局配置
         */
        @Data
        public static class GlobalConfig {
            /**
             * 是否显示Banner
             */
            private boolean banner = false;
            
            /**
             * 是否启用SQL性能分析插件
             */
            private boolean enableSqlRunner = false;
            
            /**
             * 是否启用元数据处理器
             */
            private boolean enableMetaObjectHandler = true;
            
            /**
             * 是否启用SQL注入检查
             */
            private boolean enableSqlInjector = true;
            
            /**
             * 是否启用分页插件
             */
            private boolean enablePagination = true;
            
            /**
             * 是否启用乐观锁插件
             */
            private boolean enableOptimisticLocker = true;
            
            /**
             * 是否启用防止全表更新删除插件
             */
            private boolean enableBlockAttack = true;
        }
    }

    /**
     * HikariCP配置
     */
    @Data
    public static class HikariConfig {
        /**
         * 最小空闲连接数
         */
        private Integer minimumIdle = 5;
        
        /**
         * 最大连接池大小
         */
        private Integer maximumPoolSize = 15;
        
        /**
         * 空闲超时时间(毫秒)
         */
        private Long idleTimeout = 30000L;
        
        /**
         * 最大生命周期(毫秒)
         */
        private Long maxLifetime = 1800000L;
        
        /**
         * 连接超时时间(毫秒)
         */
        private Long connectionTimeout = 30000L;
        
        /**
         * 连接测试查询
         */
        private String connectionTestQuery = "SELECT 1";
        
        /**
         * 连接初始化SQL
         */
        private String connectionInitSql;
        
        /**
         * 验证超时时间(毫秒)
         */
        private Long validationTimeout = 5000L;
        
        /**
         * 泄漏检测阈值(毫秒)
         */
        private Long leakDetectionThreshold = 0L;
        
        /**
         * 是否启用JMX
         */
        private boolean registerMbeans = false;
    }
    
    /**
     * Druid配置
     */
    @Data
    public static class DruidConfig {
        /**
         * 初始连接数
         */
        private Integer initialSize = 5;
        
        /**
         * 最小空闲连接数
         */
        private Integer minIdle = 5;
        
        /**
         * 最大活跃连接数
         */
        private Integer maxActive = 20;
        
        /**
         * 最大等待时间(毫秒)
         */
        private Long maxWait = 60000L;
        
        /**
         * 空闲连接检测间隔(毫秒)
         */
        private Long timeBetweenEvictionRunsMillis = 60000L;
        
        /**
         * 最小可驱逐空闲时间(毫秒)
         */
        private Long minEvictableIdleTimeMillis = 300000L;
        
        /**
         * 最大可驱逐空闲时间(毫秒)
         */
        private Long maxEvictableIdleTimeMillis = 900000L;
        
        /**
         * 验证查询
         */
        private String validationQuery = "SELECT 1";
        
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
        
        /**
         * 是否缓存预处理语句
         */
        private boolean poolPreparedStatements = true;
        
        /**
         * 每个连接最大预处理语句数
         */
        private Integer maxPoolPreparedStatementPerConnectionSize = 20;
        
        /**
         * 过滤器
         */
        private String filters = "stat,wall";
        
        // 手动添加 getter 方法，因为 Lombok 可能没有正确工作
        public boolean getTestWhileIdle() {
            return testWhileIdle;
        }
        
        public boolean getTestOnBorrow() {
            return testOnBorrow;
        }
        
        public boolean getTestOnReturn() {
            return testOnReturn;
        }
        
        public boolean getPoolPreparedStatements() {
            return poolPreparedStatements;
        }
    }

    /**
     * Seata分布式事务配置
     */
    @Data
    public static class SeataConfig {
        /**
         * 是否启用Seata
         */
        private boolean enabled = false;
        
        /**
         * 扫描包路径
         */
        private List<String> scanPackages = new ArrayList<>();
        
        /**
         * 排除扫描的包路径
         */
        private List<String> excludePackages = new ArrayList<>();
        
        /**
         * 延迟初始化时间（毫秒）
         */
        private int delayInitMs = 1000;
    }

    /**
     * 字段转换配置
     */
    @Data
    public static class FieldConversionConfig {
        
        /**
         * 字段转换策略类型
         * 可选值：CAMEL_TO_UNDERLINE, CAMEL_TO_KEBAB_CASE, NO_CONVERSION, CUSTOM
         * 默认：CAMEL_TO_UNDERLINE
         */
        private FieldConversionStrategyType strategy = FieldConversionStrategyType.CAMEL_TO_UNDERLINE;
        
        /**
         * 自定义转换模式（当strategy为CUSTOM时使用）
         */
        private CustomConversionPattern customPattern = new CustomConversionPattern();
        
        /**
         * 是否启用字段转换
         * 默认：true
         */
        private boolean enabled = true;
        
        /**
         * 自定义转换模式配置
         */
        @Data
        public static class CustomConversionPattern {
            
            /**
             * 字段名转列名的正则表达式
             * 例如：([A-Z]) -> _$1
             */
            private String fieldToColumnPattern;
            
            /**
             * 列名转字段名的正则表达式
             * 例如：_([a-z]) -> $1
             */
            private String columnToFieldPattern;
            
            /**
             * 字段名转列名的替换字符串
             * 例如：_$1
             */
            private String fieldToColumnReplacement;
            
            /**
             * 列名转字段名的替换字符串
             * 例如：$1
             */
            private String columnToFieldReplacement;
        }
    }

    /**
     * 健康检查配置
     */
    @Data
    public static class HealthCheckConfig {
        
        /**
         * 是否启用健康检查
         */
        private boolean enabled = true;
        
        /**
         * 健康检查间隔时间（毫秒）
         */
        private long interval = 30000;
        
        /**
         * 健康检查超时时间（毫秒）
         */
        private long timeout = 5000;
        
        /**
         * 是否在启动时执行健康检查
         */
        private boolean checkOnStartup = true;
        
        /**
         * 最大重试次数（连续失败时的重试次数）
         */
        private int maxRetries = 3;
        
        /**
         * 恢复检测间隔（数据源从不可用状态恢复检测的间隔，毫秒）
         */
        private long recoveryInterval = 10000;
    }

} 