package com.indigo.i18n.utils;

import com.indigo.i18n.resolver.I18nMessageResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 国际化消息工具类
 * 提供静态方法访问国际化消息解析器
 * 
 * @author 史偕成
 * @date 2025/03/21
 */
@Component
public class MessageUtils {
    
    private static I18nMessageResolver i18nMessageResolver;
    
    @Autowired
    public MessageUtils(I18nMessageResolver i18nMessageResolver) {
        MessageUtils.i18nMessageResolver = i18nMessageResolver;
    }
    
    /**
     * 获取国际化消息
     *
     * @param code 消息代码
     * @return 国际化消息
     */
    public static String getMessage(String code) {
        return getMessage(code, (Object[]) null);
    }
    
    /**
     * 获取国际化消息
     *
     * @param code 消息代码
     * @param args 参数
     * @return 国际化消息
     */
    public static String getMessage(String code, Object... args) {
        if (i18nMessageResolver == null) {
            return code;
        }
        return i18nMessageResolver.resolveMessage(code, args);
    }
    
    /**
     * 获取国际化消息
     *
     * @param code 消息代码
     * @param locale 语言环境
     * @return 国际化消息
     */
    public static String getMessage(String code, Locale locale) {
        return getMessage(code, locale, (Object[]) null);
    }
    
    /**
     * 获取国际化消息
     *
     * @param code 消息代码
     * @param locale 语言环境
     * @param args 参数
     * @return 国际化消息
     */
    public static String getMessage(String code, Locale locale, Object... args) {
        if (i18nMessageResolver == null) {
            return code;
        }
        return i18nMessageResolver.resolveMessage(code, locale, args);
    }
}
