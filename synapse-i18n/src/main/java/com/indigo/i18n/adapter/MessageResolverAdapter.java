package com.indigo.i18n.adapter;

import com.indigo.core.i18n.MessageResolver;
import com.indigo.i18n.resolver.I18nMessageResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 消息解析器适配器
 * 将synapse-i18n的I18nMessageResolver适配到core模块的MessageResolver接口
 * 
 * @author 史偕成
 * @date 2025/01/27
 */
@Slf4j
@Component
public class MessageResolverAdapter implements MessageResolver {
    
    @Autowired
    private I18nMessageResolver i18nMessageResolver;
    
    @Override
    public String resolveMessage(String code, Locale locale, Object... args) {
        try {
            return i18nMessageResolver.resolveMessage(code, locale, args);
        } catch (Exception e) {
            log.warn("Failed to resolve message for code: {}, locale: {}", code, locale, e);
            return code;
        }
    }
    
    @Override
    public String resolveMessage(String code, Object... args) {
        try {
            return i18nMessageResolver.resolveMessage(code, args);
        } catch (Exception e) {
            log.warn("Failed to resolve message for code: {}", code, e);
            return code;
        }
    }
    
    @Override
    public boolean isLocaleSupported(Locale locale) {
        try {
            return i18nMessageResolver.isLocaleSupported(locale);
        } catch (Exception e) {
            log.warn("Failed to check locale support for: {}", locale, e);
            return false;
        }
    }
}
