package com.indigo.i18n.adapter;

import com.indigo.core.i18n.LocaleContext;
import com.indigo.i18n.utils.LocaleContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 语言环境上下文适配器
 * 将synapse-i18n的LocaleContextHolder适配到core模块的LocaleContext接口
 * 
 * @author 史偕成
 * @date 2025/01/27
 */
@Slf4j
@Component
public class LocaleContextAdapter implements LocaleContext {
    
    @Override
    public Locale getCurrentLocale() {
        try {
            return LocaleContextHolder.getCurrentLocale();
        } catch (Exception e) {
            log.warn("Failed to get current locale", e);
            return Locale.getDefault();
        }
    }
    
    @Override
    public void setCurrentLocale(Locale locale) {
        try {
            LocaleContextHolder.setCurrentLocale(locale);
        } catch (Exception e) {
            log.warn("Failed to set current locale: {}", locale, e);
        }
    }
    
    @Override
    public void clearCurrentLocale() {
        try {
            LocaleContextHolder.clearCurrentLocale();
        } catch (Exception e) {
            log.warn("Failed to clear current locale", e);
        }
    }
}
