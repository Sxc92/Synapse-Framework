package com.indigo.i18n.config;

import com.indigo.cache.core.CacheService;
import com.indigo.i18n.resolver.I18nMessageResolver;
import com.indigo.i18n.resolver.RedisI18nMessageResolver;
import com.indigo.i18n.cache.I18nCache;
import com.indigo.i18n.utils.MessageUtils;
import com.indigo.i18n.resolver.HeaderLocaleResolver;
import com.indigo.i18n.adapter.MessageResolverAdapter;
import com.indigo.i18n.adapter.LocaleContextAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * 国际化自动配置
 * 提供基于Redis的动态国际化支持，只支持请求头方式获取语言环境
 * 
 * @author 史偕成
 * @date 2025/01/27
 */
@Configuration
@ConditionalOnClass(com.indigo.cache.core.CacheService.class)
@ConditionalOnProperty(prefix = "synapse.i18n", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(I18nProperties.class)
public class I18nAutoConfiguration {
    
    /**
     * 国际化配置属性
     */
    @Bean
    @ConditionalOnMissingBean
    public I18nProperties i18nProperties() {
        return new I18nProperties();
    }
    
    /**
     * 国际化缓存
     */
    @Bean
    @ConditionalOnMissingBean
    public I18nCache i18nCache(CacheService cacheService, I18nProperties i18nProperties) {
        return new I18nCache(cacheService);
    }
    
    /**
     * Redis国际化消息解析器
     */
    @Bean
    @ConditionalOnMissingBean
    public I18nMessageResolver i18nMessageResolver(I18nCache i18nCache) {
        return new RedisI18nMessageResolver();
    }
    
    /**
     * 消息工具类
     */
    @Bean
    @ConditionalOnMissingBean
    public MessageUtils messageUtils(I18nMessageResolver i18nMessageResolver) {
        return new MessageUtils(i18nMessageResolver);
    }
    
    /**
     * 基于请求头的语言环境解析器
     */
    @Bean
    @ConditionalOnMissingBean
    public HeaderLocaleResolver headerLocaleResolver(I18nProperties i18nProperties) {
        HeaderLocaleResolver resolver = new HeaderLocaleResolver();
        // 设置到LocaleContextHolder中
        com.indigo.i18n.utils.LocaleContextHolder.setHeaderLocaleResolver(resolver);
        return resolver;
    }
    
//    /**
//     * Spring LocaleResolver
//     * 在Gateway架构中，语言环境通常由Gateway处理并传递
//     */
//    @Bean
//    @ConditionalOnMissingBean
//    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
//    public LocaleResolver localeResolver(I18nProperties i18nProperties) {
//        AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
//        localeResolver.setDefaultLocale(i18nProperties.getDefaultLocale());
//        return localeResolver;
//    }
    
    /**
     * 消息解析器适配器
     * 将I18nMessageResolver适配到core模块的MessageResolver接口
     */
    @Bean
    @ConditionalOnMissingBean
    public MessageResolverAdapter messageResolverAdapter(I18nMessageResolver i18nMessageResolver) {
        return new MessageResolverAdapter();
    }
    
    /**
     * 语言环境上下文适配器
     * 将LocaleContextHolder适配到core模块的LocaleContext接口
     */
    @Bean
    @ConditionalOnMissingBean
    public LocaleContextAdapter localeContextAdapter() {
        return new LocaleContextAdapter();
    }
}
