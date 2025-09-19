package com.indigo.core.config;

import com.indigo.i18n.resolver.I18nMessageResolver;
import com.indigo.i18n.utils.MessageUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Test configuration for core module
 * 
 * @author 史偕成
 * @date 2025/03/21
 **/
@Configuration
public class TestConfig {
    
    @Bean
    public ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }

    @Bean
    public MessageUtils messageUtils(I18nMessageResolver i18nMessageResolver) {
        return new MessageUtils(i18nMessageResolver);
    }
} 