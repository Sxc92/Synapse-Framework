package com.indigo.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 安全模块配置属性
 * 使用自研的 TokenService 和 PermissionService 处理认证和权限
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
     * 白名单路径配置
     * 这些路径不需要认证即可访问
     */
    private WhiteListConfig whiteList = new WhiteListConfig();

    /**
     * Gateway 签名配置
     * 用于在 Gateway 和微服务之间传递签名，防止请求被篡改
     */
    private GatewaySignatureConfig gatewaySignature = new GatewaySignatureConfig();

    /**
     * Token 配置
     * 配置 Token 的前缀、查询参数名、请求头名称等
     */
    private TokenConfig token = new TokenConfig();

    /**
     * 内部服务调用配置
     * 用于服务间调用的签名验证
     */
    private InternalServiceConfig internalService = new InternalServiceConfig();

    /**
     * 白名单配置类
     */
    @Data
    public static class WhiteListConfig {
        /**
         * 是否启用白名单
         */
        private boolean enabled = true;

        /**
         * 白名单路径列表
         * 支持 Ant 风格路径匹配，如 /api/iam/auth/**
         */
        private List<String> paths = new ArrayList<>();

        /**
         * 获取默认白名单路径
         * 包含常见的公开路径和 IAM 认证接口
         */
        public List<String> getDefaultPaths() {
            return List.of(
                // IAM 认证接口
                "/auth/login",
                // 监控和文档接口
                "/actuator/**",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/webjars/**",
                "/favicon.ico",
                "/error"
            );
        }

        /**
         * 获取所有白名单路径（默认路径 + 配置路径）
         */
        public List<String> getAllPaths() {
            List<String> allPaths = new ArrayList<>();
            if (enabled) {
                allPaths.addAll(getDefaultPaths());
            }
            if (paths != null && !paths.isEmpty()) {
                allPaths.addAll(paths);
            }
            return allPaths;
        }
    }

    /**
     * Gateway 签名配置类
     */
    @Data
    public static class GatewaySignatureConfig {
        /**
         * 是否启用 Gateway 签名验证
         */
        private boolean enabled = true;

        /**
         * Gateway 签名密钥
         * 生产环境必须修改为强密钥
         */
        private String secret = "synapse-gateway-secret-key-change-in-production";

        /**
         * 签名有效期窗口（毫秒）
         * 默认 5 分钟，防止重放攻击
         */
        private long validityWindow = 5 * 60 * 1000L;

        /**
         * 是否启用用户上下文传递
         * 启用后，Gateway 会将用户上下文编码到请求头，减少微服务的 Redis 查询
         */
        private boolean enableContextPassing = true;
    }

    /**
     * Token 配置类
     */
    @Data
    public static class TokenConfig {
        /**
         * Token 前缀（用于 Authorization 请求头）
         * 默认 "Bearer "，可根据需要修改
         */
        private String prefix = "Bearer ";

        /**
         * Token 查询参数名
         * 默认 "token"，可根据需要修改
         */
        private String queryParam = "token";

        /**
         * Authorization 请求头名称
         * 默认 "Authorization"
         */
        private String headerName = "Authorization";

        /**
         * X-Auth-Token 请求头名称（备用 token 传递方式）
         * 默认 "X-Auth-Token"
         */
        private String xAuthTokenHeader = "X-Auth-Token";

        /**
         * 是否启用滑动过期（自动刷新）
         * 启用后，每次用户请求时，如果 token 剩余时间少于刷新阈值，会自动刷新 token 过期时间
         * 默认 true
         */
        private boolean enableSlidingExpiration = true;

        /**
         * 刷新阈值（秒）
         * 当 token 剩余时间少于此值时，自动刷新 token
         * 默认 30 分钟（1800 秒）
         */
        private long refreshThreshold = 30 * 60L;

        /**
         * 续期时长（秒）
         * 刷新 token 时，将过期时间延长到此值
         * 默认 2 小时（7200 秒）
         */
        private long renewalDuration = 2 * 60 * 60L;

        /**
         * Token 过期时间（秒）
         * 登录时生成的 token 的过期时间
         * 默认 2 小时（7200 秒）
         */
        private long timeout = 2 * 60 * 60L;

        /**
         * 获取 Token 前缀长度
         * 
         * @return 前缀长度
         */
        public int getPrefixLength() {
            return prefix != null ? prefix.length() : 0;
        }
    }

    /**
     * 内部服务调用配置类
     */
    @Data
    public static class InternalServiceConfig {
        /**
         * 是否启用内部服务调用签名验证
         */
        private boolean enabled = true;

        /**
         * 当前服务名称（用于标识调用来源）
         */
        private String serviceName;

        /**
         * 当前服务密钥（用于生成签名）
         * 生产环境必须修改为强密钥
         */
        private String secret;

        /**
         * 签名有效期窗口（毫秒）
         * 默认 5 分钟，防止重放攻击
         */
        private long validityWindow = 5 * 60 * 1000L;

        /**
         * 允许调用的服务白名单
         * key: 服务名称, value: 服务密钥
         * 如果为空，则允许所有服务调用（不推荐）
         */
        private Map<String, String> allowedServices = new HashMap<>();
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