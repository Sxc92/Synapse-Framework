package com.indigo.core.entity;

import com.indigo.core.constants.ErrorCode;
import com.indigo.core.constants.StandardErrorCode;
import com.indigo.core.i18n.MessageResolver;
import com.indigo.core.i18n.LocaleContext;
import com.indigo.core.utils.SpringUtils;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Locale;


/**
 * 统一返回结果封装
 * 支持动态国际化消息处理（后端处理国际化）
 * 
 * @author 史偕成
 * @date 2025/04/24 21:55
 **/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Accessors(chain = true)
public class Result<T> implements Serializable {

    private T data;
    private String msg;
    private String code;

    // ==================== 成功方法 ====================
    
    /**
     * 成功返回（无数据）
     */
    public static <T> Result<T> success() {
        return Result.<T>builder()
                .data(null)
                .code(StandardErrorCode.BASE_SUCCESS.getCode())
                .msg(resolveMessage(StandardErrorCode.BASE_SUCCESS))
                .build();
    }

    /**
     * 成功返回（带数据）
     */
    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .data(data)
                .code(StandardErrorCode.BASE_SUCCESS.getCode())
                .msg(resolveMessage(StandardErrorCode.BASE_SUCCESS))
                .build();
    }

    /**
     * 成功返回（带数据和自定义消息）
     */
    public static <T> Result<T> success(T data, String msg) {
        return Result.<T>builder()
                .data(data)
                .code(StandardErrorCode.BASE_SUCCESS.getCode())
                .msg(msg)
                .build();
    }

    // ==================== 错误方法 ====================
    
    /**
     * 错误返回（使用默认错误码）
     */
    public static <T> Result<T> error() {
        return Result.<T>builder()
                .data(null)
                .code(StandardErrorCode.BASE_ERROR.getCode())
                .msg(resolveMessage(StandardErrorCode.BASE_ERROR))
                .build();
    }

    /**
     * 错误返回（使用错误码）
     */
    public static <T> Result<T> error(ErrorCode errorCode) {
        return Result.<T>builder()
                .data(null)
                .code(errorCode.getCode())
                .msg(resolveMessage(errorCode))
                .build();
    }

    /**
     * 错误返回（使用错误码和参数）
     */
    public static <T> Result<T> error(ErrorCode errorCode, Object... args) {
        return Result.<T>builder()
                .data(null)
                .code(errorCode.getCode())
                .msg(resolveMessage(errorCode, args))
                .build();
    }

    /**
     * 错误返回（使用错误码字符串）
     */
    public static <T> Result<T> error(String code) {
        return Result.<T>builder()
                .data(null)
                .code(code)
                .msg(resolveMessage(code))
                .build();
    }

    /**
     * 错误返回（使用错误码和自定义消息）
     */
    public static <T> Result<T> error(String code, String msg) {
        return Result.<T>builder()
                .data(null)
                .code(code)
                .msg(msg)
                .build();
    }

    // ==================== 工具方法 ====================
    
    /**
     * 解析错误码对应的国际化消息
     */
    private static String resolveMessage(ErrorCode errorCode) {
        return resolveMessage(errorCode, new Object[0]);
    }

    /**
     * 解析错误码对应的国际化消息（带参数）
     */
    private static String resolveMessage(ErrorCode errorCode, Object... args) {
        try {
            MessageResolver messageResolver = SpringUtils.getBean(MessageResolver.class);
            LocaleContext localeContext = SpringUtils.getBean(LocaleContext.class);
            
            Locale locale = localeContext.getCurrentLocale();
            return messageResolver.resolveMessage(errorCode.getMessageKey(), locale, args);
        } catch (Exception e) {
            // 如果国际化解析失败，返回错误码
            return errorCode.getCode();
        }
    }

    /**
     * 解析错误码字符串对应的国际化消息
     */
    private static String resolveMessage(String code) {
        try {
            MessageResolver messageResolver = SpringUtils.getBean(MessageResolver.class);
            LocaleContext localeContext = SpringUtils.getBean(LocaleContext.class);
            
            Locale locale = localeContext != null ? localeContext.getCurrentLocale() : Locale.getDefault();
            String messageKey = "error." + code.toLowerCase();
            return messageResolver.resolveMessage(messageKey, locale);
        } catch (Exception e) {
            return code;
        }
    }

    /**
     * 判断是否成功
     */
    public Boolean isSuccess() {
        return StandardErrorCode.BASE_SUCCESS.getCode().equals(code);
    }

    /**
     * 判断是否失败
     */
    public Boolean isError() {
        return !isSuccess();
    }
}
