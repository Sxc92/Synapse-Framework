package com.indigo.security.config;

import com.indigo.security.interceptor.UserContextInterceptor;
import com.indigo.security.utils.TokenExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebMVC 安全配置自动装配类
 * 注册用户上下文拦截器
 * 
 * <p>此配置类通过 {@code AutoConfiguration.imports} 文件自动注册，
 * 确保拦截器在所有使用 {@code synapse-security} 模块的应用中生效。
 * 
 * <p><b>注意：</b>此配置类只在 WebMVC 环境中生效（通过 {@code @ConditionalOnWebApplication} 条件），
 * 这样可以避免在 WebFlux 环境中加载 Spring MVC 相关的类（如 {@code HandlerInterceptor}）。
 * 
 * @author 史偕成
 * @date 2025/01/10
 */
@Slf4j
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(WebMvcConfigurer.class)
public class WebMvcSecurityConfig implements WebMvcConfigurer {

    @Autowired
    private SecurityProperties securityProperties;

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 创建 Token 提取工具 Bean
     * 只在 WebMVC 环境中创建，避免在 WebFlux 环境中加载 Servlet API
     * 
     * @return Token 提取工具
     */
    @Bean
    @ConditionalOnMissingBean(TokenExtractor.class)
    public TokenExtractor tokenExtractor() {
        log.debug("初始化 Token 提取工具");
        return new TokenExtractor(securityProperties);
    }

    /**
     * 创建用户上下文拦截器 Bean
     * 只在 WebMVC 环境中创建，避免在 WebFlux 环境中加载 Spring MVC 相关的类
     * 
     * <p><b>注意：</b>不在字段中注入此 Bean，而是在 {@link #addInterceptors} 方法中
     * 通过 {@code ApplicationContext} 获取，避免循环依赖。
     * 
     * @param tokenExtractor Token 提取工具（必须）
     * @return 用户上下文拦截器
     */
    @Bean
    @ConditionalOnMissingBean(UserContextInterceptor.class)
    public UserContextInterceptor userContextInterceptor(TokenExtractor tokenExtractor) {
        log.debug("初始化用户上下文拦截器，TokenExtractor: {}",
                tokenExtractor != null ? "已注入" : "未配置");
        return new UserContextInterceptor(securityProperties, tokenExtractor);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 通过 ApplicationContext 获取 Bean，避免循环依赖
        try {
            UserContextInterceptor interceptor = applicationContext.getBean(UserContextInterceptor.class);
            if (interceptor != null) {
                // 注册用户上下文拦截器，拦截所有请求
                registry.addInterceptor(interceptor)
                        .addPathPatterns("/**")
                        .order(1); // 设置优先级，确保在其他拦截器之前执行
                log.debug("用户上下文拦截器已注册到 WebMVC，拦截路径: /**");
            } else {
                log.debug("用户上下文拦截器不存在，跳过注册");
            }
        } catch (org.springframework.beans.factory.NoSuchBeanDefinitionException e) {
            log.debug("用户上下文拦截器 Bean 不存在，跳过注册");
        }
    }
}

