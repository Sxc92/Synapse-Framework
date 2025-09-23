package com.indigo.security.config;

import cn.dev33.satoken.oauth2.config.SaOAuth2Config;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Template;
import cn.dev33.satoken.stp.StpInterface;
import com.indigo.cache.session.UserSessionService;
import com.indigo.security.core.*;
import com.indigo.security.service.DefaultAuthenticationService;
import com.indigo.security.interceptor.UserContextWebFluxFilter;
import com.indigo.security.view.OAuth2ViewHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import jakarta.annotation.PostConstruct;

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
    "com.indigo.security.core"
})
public class SecurityAutoConfiguration {

    /**
     * 初始化安全配置
     */
    @PostConstruct
    public void init() {
        // 关闭Sa-Token的banner图
        System.setProperty("sa-token.is-print", "false");
    }

    /**
     * Token管理服务（依赖 UserSessionService）
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
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(AuthenticationService.class)
    public AuthenticationService authenticationService() {
        log.debug("初始化认证服务（默认版本）");
        return new DefaultAuthenticationService();
    }

    /**
     * 配置OAuth2
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "synapse.oauth2.enabled", havingValue = "true", matchIfMissing = false)
    public SaOAuth2Config oAuth2Config() {
        log.debug("初始化OAuth2.0配置");
        // 基础的OAuth2配置，具体的视图处理可以在后续版本中完善
        return new SaOAuth2Config();
    }

    /**
     * OAuth2.0模板
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
     */
    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @ConditionalOnClass(WebFluxConfigurer.class)
    public static class WebFluxConfiguration implements WebFluxConfigurer {

        private final UserContextWebFluxFilter userContextFilter;

        public WebFluxConfiguration(UserContextWebFluxFilter userContextFilter) {
            this.userContextFilter = userContextFilter;
        }

        @Override
        public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
            // WebFlux特定的配置
        }
    }

    /**
     * OAuth2.0视图处理器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(SaOAuth2Config.class)
    public OAuth2ViewHandler oAuth2ViewHandler() {
        log.info("初始化OAuth2.0视图处理器");
        return new OAuth2ViewHandler();
    }
} 