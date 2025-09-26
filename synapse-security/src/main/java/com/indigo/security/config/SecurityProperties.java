package com.indigo.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 安全模块配置属性
 * 直接使用Sa-Token框架处理所有认证
 *
 * @author 史偕成
 * @date 2025/01/07
 */
@Data
@ConfigurationProperties(prefix = "synapse.security")
public class SecurityProperties {

    /**
     * 是否启用安全模块
     */
    private boolean enabled = true;

    /**
     * 安全模式
     */
    private SecurityMode mode = SecurityMode.STRICT;

    /**
     * 是否启用安全日志
     */
    private boolean securityLogging = true;

    /**
     * 安全日志级别
     */
    private String securityLogLevel = "INFO";

    /**
     * Sa-Token 配置
     */
    private SaTokenConfig satoken = new SaTokenConfig();

    /**
     * JWT 配置
     */
    private JwtConfig jwt = new JwtConfig();

    /**
     * OAuth2 配置
     */
    private OAuth2Config oauth2 = new OAuth2Config();

    /**
     * 登录安全配置
     */
    private LoginConfig login = new LoginConfig();

    /**
     * 数据权限配置
     */
    private DataPermissionConfig dataPermission = new DataPermissionConfig();

    /**
     * Sa-Token 配置类
     */
    @Data
    public static class SaTokenConfig {
        /**
         * 是否启用Sa-Token
         */
        private boolean enabled = true;

        /**
         * Token名称
         */
        private String tokenName = "satoken";

        /**
         * Token有效期（秒）
         */
        private long timeout = 2592000;

        /**
         * Token活跃有效期（秒）
         */
        private long activityTimeout = 1800;

        /**
         * 是否允许同一账号并发登录
         */
        private boolean isConcurrent = true;

        /**
         * 是否在多个项目中共享Token
         */
        private boolean isShare = false;

        /**
         * 是否输出操作日志
         */
        private boolean isLog = true;

        /**
         * 是否从Cookie中读取Token
         */
        private boolean isReadCookie = false;

        /**
         * 是否从请求头中读取Token
         */
        private boolean isReadHeader = true;

        /**
         * 是否从请求体中读取Token
         */
        private boolean isReadBody = false;

        /**
         * 是否在响应头中写入Token
         */
        private boolean isWriteHeader = true;

        /**
         * 是否在响应体中写入Token
         */
        private boolean isWriteBody = false;

        /**
         * 是否在Cookie中写入Token
         */
        private boolean isWriteCookie = false;

        /**
         * Token前缀
         */
        private String tokenPrefix = "Bearer";

        /**
         * 是否打印Sa-Token版本信息和banner
         */
        private boolean isPrint = false;

        /**
         * Token风格
         */
        private String tokenStyle = "uuid";
    }

    /**
     * JWT 配置类
     */
    @Data
    public static class JwtConfig {
        /**
         * 是否启用JWT认证
         */
        private boolean enabled = true;

        /**
         * JWT签名密钥
         */
        private String secret = "your-super-secret-jwt-signing-key-here";

        /**
         * JWT过期时间（秒）
         */
        private long expiration = 86400;

        /**
         * JWT请求头名称
         */
        private String headerName = "Authorization";

        /**
         * JWT令牌前缀
         */
        private String prefix = "Bearer ";
    }

    /**
     * OAuth2 配置类
     */
    @Data
    public static class OAuth2Config {
        /**
         * 是否启用OAuth2.0
         */
        private boolean enabled = false;

        /**
         * OAuth2客户端ID
         */
        private String clientId;

        /**
         * OAuth2客户端密钥
         */
        private String clientSecret;

        /**
         * OAuth2授权服务器URL
         */
        private String authorizationServerUrl;

        /**
         * OAuth2令牌服务器URL
         */
        private String tokenServerUrl;

        /**
         * OAuth2用户信息URL
         */
        private String userInfoUrl;

        /**
         * OAuth2重定向URI
         */
        private String redirectUri;

        /**
         * OAuth2授权范围
         */
        private String scope = "read";
    }

    /**
     * 登录安全配置类
     */
    @Data
    public static class LoginConfig {
        /**
         * 最大登录失败次数
         */
        private int maxFailCount = 5;

        /**
         * 账户锁定持续时间（秒）
         */
        private long lockDuration = 1800;

        /**
         * 失败计数窗口时间（秒）
         */
        private long failWindow = 300;
    }

    /**
     * 数据权限配置类
     */
    @Data
    public static class DataPermissionConfig {
        /**
         * 是否启用数据权限
         */
        private boolean enabled = true;

        /**
         * 数据权限规则缓存时间（秒）
         */
        private long cacheTimeout = 3600;

        /**
         * 是否启用SQL注入防护
         */
        private boolean sqlInjectionProtection = true;
    }

    /**
     * 安全模式枚举
     */
    public enum SecurityMode {
        /**
         * 严格模式：所有请求都需要认证
         */
        STRICT,
        
        /**
         * 宽松模式：部分请求可以匿名访问
         */
        PERMISSIVE,
        
        /**
         * 关闭模式：不进行安全控制
         */
        DISABLED
    }
} 