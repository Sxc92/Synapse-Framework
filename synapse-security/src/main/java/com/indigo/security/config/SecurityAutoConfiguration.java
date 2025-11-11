package com.indigo.security.config;

import cn.dev33.satoken.oauth2.config.SaOAuth2Config;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Template;
import cn.dev33.satoken.stp.StpInterface;
import com.indigo.cache.session.UserSessionService;
import com.indigo.security.core.*;
import com.indigo.security.service.DefaultAuthenticationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;



/**
 * 安全配置自动装配类
 * 支持WebMVC和WebFlux环境
 * 直接使用Sa-Token框架处理所有认证
 *
 * @author 史偕成
 * @date 2025/01/08
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "synapse.security", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(SecurityProperties.class)
@ComponentScan(basePackages = {
    "com.indigo.security.service",
    "com.indigo.security.core",
    "com.indigo.security.interceptor"
})
public class SecurityAutoConfiguration {

    @Autowired
    private SecurityProperties securityProperties;

    /**
     * Sa-Token系统属性常量
     * 使用JDK17的现代特性
     */
    private static final String SA_TOKEN_IS_PRINT = "sa-token.is-print";
    private static final String SA_TOKEN_TOKEN_NAME = "sa-token.token-name";
    private static final String SA_TOKEN_TIMEOUT = "sa-token.timeout";
    private static final String SA_TOKEN_ACTIVITY_TIMEOUT = "sa-token.activity-timeout";
    private static final String SA_TOKEN_IS_CONCURRENT = "sa-token.is-concurrent";
    private static final String SA_TOKEN_IS_SHARE = "sa-token.is-share";
    private static final String SA_TOKEN_IS_LOG = "sa-token.is-log";
    private static final String SA_TOKEN_IS_READ_COOKIE = "sa-token.is-read-cookie";
    private static final String SA_TOKEN_IS_READ_HEADER = "sa-token.is-read-header";
    private static final String SA_TOKEN_IS_READ_BODY = "sa-token.is-read-body";
    private static final String SA_TOKEN_IS_WRITE_HEADER = "sa-token.is-write-header";
    private static final String SA_TOKEN_IS_WRITE_BODY = "sa-token.is-write-body";
    private static final String SA_TOKEN_IS_WRITE_COOKIE = "sa-token.is-write-cookie";
    private static final String SA_TOKEN_TOKEN_PREFIX = "sa-token.token-prefix";
    private static final String SA_TOKEN_TOKEN_STYLE = "sa-token.token-style";

    /**
     * 静态初始化块 - 在类加载时就设置Sa-Token属性
     * 这确保在Sa-Token初始化之前就设置了系统属性
     * 必须在JVM启动时设置，而不是在Spring容器初始化时设置
     */
    static {
        // 默认关闭Sa-Token的banner和版本信息
        System.setProperty(SA_TOKEN_IS_PRINT, "false");
    }

    /**
     * 使用 @Bean 方法在 Bean 创建时初始化配置
     * 这比 @PostConstruct 更早执行，确保在 Sa-Token 初始化前完成配置
     * 使用 @DependsOn 确保在其他 Bean 之前创建
     */
    @Bean
    @ConditionalOnProperty(prefix = "synapse.security", name = "enabled", havingValue = "true", matchIfMissing = true)
    public Object securityConfigurationInitializer() {
        // 根据配置动态设置Sa-Token属性
        configureSaTokenProperties();
        
        // 使用文本块格式化日志
//        log.info("""
//            ==========================================
//            Synapse Security 模块初始化完成
//            - Sa-Token banner已在静态初始化块中关闭
//            - 配置属性已动态应用
//            ==========================================
//            """);
        
        // 返回一个占位对象，这个 Bean 的唯一目的是触发初始化
        return new Object();
    }

    /**
     * 配置Sa-Token属性
     * 使用JDK17的现代特性：record、switch表达式、文本块等
     */
    private void configureSaTokenProperties() {
        var satokenConfig = securityProperties.getSatoken();
        
        if (!satokenConfig.isEnabled()) {
            log.info("Sa-Token已禁用");
            return;
        }

        // 使用record定义配置项，提高代码可读性
        record SaTokenProperty(String key, String value) {}
        
        // 使用Stream API和switch表达式配置属性
        var properties = java.util.stream.Stream.of(
            new SaTokenProperty(SA_TOKEN_TOKEN_NAME, satokenConfig.getTokenName()),
            new SaTokenProperty(SA_TOKEN_TIMEOUT, String.valueOf(satokenConfig.getTimeout())),
            new SaTokenProperty(SA_TOKEN_ACTIVITY_TIMEOUT, String.valueOf(satokenConfig.getActivityTimeout())),
            new SaTokenProperty(SA_TOKEN_IS_CONCURRENT, String.valueOf(satokenConfig.isConcurrent())),
            new SaTokenProperty(SA_TOKEN_IS_SHARE, String.valueOf(satokenConfig.isShare())),
            new SaTokenProperty(SA_TOKEN_IS_LOG, String.valueOf(satokenConfig.isLog())),
            new SaTokenProperty(SA_TOKEN_IS_READ_COOKIE, String.valueOf(satokenConfig.isReadCookie())),
            new SaTokenProperty(SA_TOKEN_IS_READ_HEADER, String.valueOf(satokenConfig.isReadHeader())),
            new SaTokenProperty(SA_TOKEN_IS_READ_BODY, String.valueOf(satokenConfig.isReadBody())),
            new SaTokenProperty(SA_TOKEN_IS_WRITE_HEADER, String.valueOf(satokenConfig.isWriteHeader())),
            new SaTokenProperty(SA_TOKEN_IS_WRITE_BODY, String.valueOf(satokenConfig.isWriteBody())),
            new SaTokenProperty(SA_TOKEN_IS_WRITE_COOKIE, String.valueOf(satokenConfig.isWriteCookie())),
            new SaTokenProperty(SA_TOKEN_TOKEN_PREFIX, satokenConfig.getTokenPrefix()),
            new SaTokenProperty(SA_TOKEN_IS_PRINT, String.valueOf(satokenConfig.isPrint())),
            new SaTokenProperty(SA_TOKEN_TOKEN_STYLE, satokenConfig.getTokenStyle())
        ).toList();

        // 批量设置系统属性
        properties.forEach(prop -> System.setProperty(prop.key(), prop.value()));
        
        // 使用文本块格式化日志信息
//        log.info("""
//            Sa-Token配置已应用:
//            - token-name: {}
//            - timeout: {}
//            - is-print: {}
//            """,
//            satokenConfig.getTokenName(),
//            satokenConfig.getTimeout(),
//            satokenConfig.isPrint()
//        );
    }

    /**
     * Token管理服务（依赖 UserSessionService）
     * 使用JDK17的现代特性
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(UserSessionService.class)
    public TokenManager tokenManager(UserSessionService userSessionService) {
        log.debug("初始化Token管理服务");
        return new TokenManager(userSessionService);
    }

    /**
     * 权限管理服务（依赖 UserSessionService）
     * 使用JDK17的现代特性
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(StpInterface.class)
    @ConditionalOnBean(UserSessionService.class)
    public PermissionManager permissionManager(UserSessionService userSessionService) {
        log.debug("初始化权限管理服务");
        return new PermissionManager(userSessionService);
    }

    /**
     * 认证服务（默认版本）
     * 使用JDK17的现代特性
     * 如果 UserSessionService 存在，则注入；否则使用无参构造函数
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(AuthenticationService.class)
    public AuthenticationService authenticationService(@Autowired(required = false) UserSessionService userSessionService) {
        if (userSessionService != null) {
            log.debug("初始化认证服务（默认版本），UserSessionService: 已注入");
            return new DefaultAuthenticationService(userSessionService);
        } else {
            log.warn("初始化认证服务（默认版本），UserSessionService: 未配置，将使用无参构造函数");
            return new DefaultAuthenticationService();
        }
    }

    /**
     * 配置OAuth2（用于第三方登录，如微信、QQ、GitHub等）
     * 使用JDK17的现代特性
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "synapse.security.oauth2.enabled", havingValue = "true", matchIfMissing = false)
    public SaOAuth2Config oAuth2Config() {
        log.debug("初始化OAuth2.0配置");
        return new SaOAuth2Config();
    }

    /**
     * OAuth2.0模板
     * 使用JDK17的现代特性
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(SaOAuth2Config.class)
    public SaOAuth2Template saOAuth2Template() {
        log.info("初始化OAuth2.0模板");
        return new SaOAuth2Template();
    }

    /**
     * WebFlux环境配置
     * 使用JDK17的现代特性
     */
    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @ConditionalOnClass(WebFluxConfigurer.class)
    public static class WebFluxConfiguration implements WebFluxConfigurer {

        @Override
        public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
            // WebFlux特定的配置
            log.debug("WebFlux环境配置已初始化");
        }
    }

} 