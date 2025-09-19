package com.indigo.core.i18n;

import java.util.Locale;

/**
 * 国际化消息解析器接口
 * 定义在core模块中，避免core依赖i18n模块
 * 
 * @author 史偕成
 * @date 2025/01/27
 */
public interface MessageResolver {
    
    /**
     * 解析国际化消息
     * 
     * @param code 消息代码
     * @param locale 语言环境
     * @param args 消息参数
     * @return 国际化消息
     */
    String resolveMessage(String code, Locale locale, Object... args);
    
    /**
     * 解析国际化消息（使用当前语言环境）
     * 
     * @param code 消息代码
     * @param args 消息参数
     * @return 国际化消息
     */
    String resolveMessage(String code, Object... args);
    
    /**
     * 检查语言是否支持
     * 
     * @param locale 语言环境
     * @return 是否支持
     */
    boolean isLocaleSupported(Locale locale);
}
