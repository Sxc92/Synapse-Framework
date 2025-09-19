package com.indigo.core.exception;

import com.indigo.core.exception.enums.ErrorCode;
import com.indigo.i18n.utils.MessageUtils;

import java.util.Locale;

/**
 * 国际化异常
 * 支持多语言的异常消息
 * 
 * @author 史偕成
 * @date 2025/01/27
 */
public class I18nException extends BaseException {
    
    private final Locale locale;
    
    /**
     * 构造函数
     * 
     * @param errorCode 错误代码
     * @param locale 语言环境
     * @param args 消息参数
     */
    public I18nException(ErrorCode errorCode, Locale locale, Object... args) {
        super(errorCode, MessageUtils.getMessage(errorCode.getMessage(), locale, args), args);
        this.locale = locale;
    }
    
    /**
     * 构造函数（带原因）
     * 
     * @param errorCode 错误代码
     * @param locale 语言环境
     * @param cause 异常原因
     * @param args 消息参数
     */
    public I18nException(ErrorCode errorCode, Locale locale, Throwable cause, Object... args) {
        super(errorCode, MessageUtils.getMessage(errorCode.getMessage(), locale, args), cause, args);
        this.locale = locale;
    }
    
    /**
     * 构造函数（自定义消息）
     * 
     * @param errorCode 错误代码
     * @param locale 语言环境
     * @param message 自定义消息
     * @param args 消息参数
     */
    public I18nException(ErrorCode errorCode, Locale locale, String message, Object... args) {
        super(errorCode, message, args);
        this.locale = locale;
    }
    
    /**
     * 构造函数（自定义消息，带原因）
     * 
     * @param errorCode 错误代码
     * @param locale 语言环境
     * @param message 自定义消息
     * @param cause 异常原因
     * @param args 消息参数
     */
    public I18nException(ErrorCode errorCode, Locale locale, String message, Throwable cause, Object... args) {
        super(errorCode, message, cause, args);
        this.locale = locale;
    }
    
    /**
     * 获取语言环境
     * 
     * @return 语言环境
     */
    public Locale getLocale() {
        return locale;
    }
    
    /**
     * 使用默认语言环境创建I18nException
     * 
     * @param errorCode 错误代码
     * @param args 消息参数
     * @return I18nException实例
     */
    public static I18nException of(ErrorCode errorCode, Object... args) {
        return new I18nException(errorCode, Locale.getDefault(), args);
    }
    
    /**
     * 使用默认语言环境创建I18nException（带原因）
     * 
     * @param errorCode 错误代码
     * @param cause 异常原因
     * @param args 消息参数
     * @return I18nException实例
     */
    public static I18nException of(ErrorCode errorCode, Throwable cause, Object... args) {
        return new I18nException(errorCode, Locale.getDefault(), cause, args);
    }
}
