package com.indigo.security.config;

import com.indigo.cache.session.UserSessionService;
import com.indigo.security.aspect.PermissionAspect;
import com.indigo.security.core.AuthenticationService;
import com.indigo.security.core.PermissionService;
import com.indigo.security.core.TokenManager;
import com.indigo.security.core.TokenService;
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
 * 使用自研的 TokenService 和 PermissionService 处理认证和权限
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
    "com.indigo.security.feign"
    // 注意：
    // - com.indigo.security.interceptor 不在这里扫描，UserContextInterceptor 通过 @Bean 方法在 WebMvcSecurityConfig 中创建
    // - com.indigo.security.aspect 不在这里扫描，PermissionAspect 通过 @Bean 方法在此类中创建（避免条件加载问题）
})
public class SecurityAutoConfiguration {


    /**
     * Token服务（依赖 UserSessionService）
     * 负责 Token 的生成、验证、存储和撤销
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(UserSessionService.class)
    public TokenService tokenService(UserSessionService userSessionService) {
        log.info("初始化Token服务");
        return new TokenService(userSessionService);
    }

    /**
     * 权限检查服务
     * 提供权限和角色的检查方法
     * 
     * <p><b>注意：</b>此服务不依赖任何 Bean，只使用 ThreadLocal 中的 UserContext。
     * 因此不需要 {@code @ConditionalOnBean} 条件。
     */
    @Bean
    @ConditionalOnMissingBean
    public PermissionService permissionService() {
        log.debug("初始化权限检查服务");
        return new PermissionService();
    }

    /**
     * 权限检查切面（依赖 PermissionService 和 SecurityProperties）
     * 通过 AOP 拦截自定义权限注解
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(PermissionService.class)
    public PermissionAspect permissionAspect(PermissionService permissionService, 
                                             SecurityProperties securityProperties) {
        log.debug("初始化权限检查切面");
        return new PermissionAspect(permissionService, securityProperties);
    }

    /**
     * Token管理服务（依赖 UserSessionService 和 TokenService）
     * 为了保持向后兼容性而保留
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({UserSessionService.class, TokenService.class})
    public TokenManager tokenManager(TokenService tokenService) {
        log.debug("初始化Token管理服务");
        return new TokenManager(tokenService);
    }

    /**
     * 认证服务（默认版本）
     * 如果 UserSessionService 和 TokenService 存在，则注入；否则使用无参构造函数
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(AuthenticationService.class)
    public AuthenticationService authenticationService(
            @Autowired(required = false) UserSessionService userSessionService,
            @Autowired(required = false) TokenService tokenService,
            SecurityProperties securityProperties) {
        if (userSessionService != null && tokenService != null) {
            log.debug("初始化认证服务（默认版本），UserSessionService: 已注入, TokenService: 已注入");
            return new DefaultAuthenticationService(userSessionService, tokenService, securityProperties);
        } else {
            log.warn("初始化认证服务（默认版本），UserSessionService: {}, TokenService: {}",
                    userSessionService != null ? "已注入" : "未配置",
                    tokenService != null ? "已注入" : "未配置");
            return new DefaultAuthenticationService();
        }
    }

    /**
     * 用户上下文拦截器（WebMVC环境）
     * 
     * <p><b>注意：</b>此 Bean 的创建已移至 {@link WebMvcSecurityConfig} 中，
     * 因为那里已经有 {@code @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)}
     * 条件，可以确保只在 WebMVC 环境中创建，避免在 WebFlux 环境中加载 Spring MVC 相关的类。
     * 
     * <p>如果在这里创建，即使有 {@code @ConditionalOnClass} 条件，类加载时仍然会尝试加载
     * {@code UserContextInterceptor} 类，导致在 WebFlux 环境中抛出 {@code NoClassDefFoundError}。
     */

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