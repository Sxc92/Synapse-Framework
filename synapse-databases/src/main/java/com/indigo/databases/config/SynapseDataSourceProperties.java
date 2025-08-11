package com.indigo.databases.config;

import com.indigo.databases.enums.DatabaseType;
import com.indigo.databases.enums.PoolType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Synapse Framework 数据源配置属性类
 * 整合 MyBatis-Plus 和动态数据源配置
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
    private String primary = "master1";

    /**
     * 动态数据源配置
     */
    private DynamicDataSource dynamicDataSource = new DynamicDataSource();

    /**
     * 兼容标准Spring Boot配置
     */
    private SpringDatasource springDatasource = new SpringDatasource();

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
     * 动态数据源配置
     */
    @Data
    public static class DynamicDataSource {
        /**
         * 是否启用严格模式
         */
        private boolean strict = false;
        
        /**
         * 是否启用Seata分布式事务
         */
        private boolean seata = false;
        
        /**
         * 是否启用P6Spy
         */
        private boolean p6spy = false;
        
        /**
         * 数据源配置
         */
        private Map<String, DataSourceConfig> datasource = new LinkedHashMap<>();
        
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
    }
    
    /**
     * 兼容标准Spring Boot配置
     * 支持 spring.datasource.dynamic 配置格式
     */
    @Data
    public static class SpringDatasource {
        /**
         * 动态数据源配置
         */
        private Dynamic springDynamic = new Dynamic();
        
        /**
         * 动态数据源配置
         */
        @Data
        public static class Dynamic {
            /**
             * 主数据源名称
             */
            private String primary = "master1";
            
            /**
             * 是否启用严格模式
             */
            private boolean strict = false;
            
            /**
             * 数据源配置
             */
            private Map<String, SpringDataSourceConfig> datasource = new LinkedHashMap<>();
        }
        
        /**
         * Spring Boot标准数据源配置
         */
        @Data
        public static class SpringDataSourceConfig {
            /**
             * 数据库类型
             */
            private String type = "MYSQL";
            
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
             * 连接池类型
             */
            private String poolType = "HIKARI";
            
            /**
             * 连接参数
             */
            private Map<String, String> params = new LinkedHashMap<>();
            
            /**
             * HikariCP连接池配置
             */
            private SpringHikariConfig hikari = new SpringHikariConfig();
            
            /**
             * 获取JDBC URL
             */
            public String getUrl() {
                return String.format("jdbc:%s://%s:%d/%s", 
                    type.toLowerCase(), host, port, database);
            }
            
            /**
             * 获取驱动类名
             */
            public String getDriverClassName() {
                return switch (type.toUpperCase()) {
                    case "MYSQL" -> "com.mysql.cj.jdbc.Driver";
                    case "POSTGRESQL" -> "org.postgresql.Driver";
                    case "ORACLE" -> "oracle.jdbc.OracleDriver";
                    case "SQLSERVER" -> "com.microsoft.sqlserver.jdbc.SQLServerDriver";
                    case "H2" -> "org.h2.Driver";
                    default -> "com.mysql.cj.jdbc.Driver";
                };
            }
        }
        
        /**
         * Spring Boot标准HikariCP配置
         */
        @Data
        public static class SpringHikariConfig {
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
        }
    }
} 