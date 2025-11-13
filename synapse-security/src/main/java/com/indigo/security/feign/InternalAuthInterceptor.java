package com.indigo.security.feign;

import com.indigo.security.config.SecurityProperties;
import com.indigo.security.constants.SecurityConstants;
import com.indigo.security.utils.InternalSignatureUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Feign 内部服务调用拦截器
 * 自动为 Feign 请求添加内部服务调用签名
 * 
 * <p>此拦截器会在以下条件满足时自动生效：
 * <ul>
 *   <li>项目中引入了 Feign 依赖（spring-cloud-starter-openfeign）</li>
 *   <li>配置了 {@code synapse.security.internal-service.enabled=true}</li>
 *   <li>配置了 {@code synapse.security.internal-service.service-name} 和 {@code secret}</li>
 * </ul>
 * 
 * <p><b>工作原理：</b>
 * <ul>
 *   <li>使用 {@code @ConditionalOnClass} 确保只有在 Feign 依赖存在时才加载此类</li>
 *   <li>使用 {@code @Component} 让 Spring 自动发现并注册为 Bean</li>
 *   <li>Feign 会自动扫描所有实现了 {@code RequestInterceptor} 接口的 Bean 并注册</li>
 * </ul>
 * 
 * <p>使用方式：
 * <ol>
 *   <li>在业务模块的 pom.xml 中添加 Feign 依赖：
 *       <pre>{@code
 *       <dependency>
 *           <groupId>org.springframework.cloud</groupId>
 *           <artifactId>spring-cloud-starter-openfeign</artifactId>
 *       </dependency>
 *       }</pre>
 *   </li>
 *   <li>在配置文件中配置内部服务调用参数（见 application-security-example.yml）</li>
 *   <li>在启动类上添加 {@code @EnableFeignClients} 注解</li>
 *   <li>拦截器会自动为所有 Feign 请求添加签名</li>
 * </ol>
 * 
 * @author 史偕成
 * @date 2025/01/10
 */
@Slf4j
@Component
@ConditionalOnClass(name = "feign.RequestInterceptor")
@ConditionalOnProperty(prefix = "synapse.security.internal-service", name = "enabled", havingValue = "true", matchIfMissing = false)
public class InternalAuthInterceptor implements feign.RequestInterceptor {

    @Autowired(required = false)
    private SecurityProperties securityProperties;

    @Override
    public void apply(feign.RequestTemplate template) {
        if (securityProperties == null) {
            log.warn("SecurityProperties 未配置，跳过内部服务调用签名");
            return;
        }

        SecurityProperties.InternalServiceConfig config = securityProperties.getInternalService();
        if (config == null || !config.isEnabled()) {
            log.debug("内部服务调用签名未启用，跳过");
            return;
        }

        String serviceName = config.getServiceName();
        String secret = config.getSecret();

        if (!StringUtils.hasText(serviceName) || !StringUtils.hasText(secret)) {
            log.warn("内部服务调用配置不完整: serviceName={}, secret={}", 
                    serviceName, StringUtils.hasText(secret) ? "***" : "null");
            return;
        }

        // 生成时间戳和签名
        long timestamp = System.currentTimeMillis();
        String signature = InternalSignatureUtils.generateSignature(secret, serviceName, timestamp);

        if (!StringUtils.hasText(signature)) {
            log.warn("生成内部服务调用签名失败: serviceName={}", serviceName);
            return;
        }

        // 添加请求头
        template.header(SecurityConstants.X_INTERNAL_SERVICE_HEADER, serviceName);
        template.header(SecurityConstants.X_INTERNAL_TIMESTAMP_HEADER, String.valueOf(timestamp));
        template.header(SecurityConstants.X_INTERNAL_SIGNATURE_HEADER, signature);

        log.debug("已添加内部服务调用签名: serviceName={}, timestamp={}", serviceName, timestamp);
    }
}

