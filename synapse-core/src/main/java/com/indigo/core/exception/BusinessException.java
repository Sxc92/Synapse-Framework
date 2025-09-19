package com.indigo.core.exception;

import com.indigo.core.exception.enums.ErrorCode;

/**
 * 业务异常
 * 用于处理业务逻辑相关的异常情况
 * 
 * @author 史偕成
 * @date 2025/01/27
 */
public class BusinessException extends BaseException {
    
    /**
     * 构造函数
     * 
     * @param errorCode 错误代码
     * @param args 消息参数
     */
    public BusinessException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
    
    /**
     * 构造函数（带原因）
     * 
     * @param errorCode 错误代码
     * @param cause 异常原因
     * @param args 消息参数
     */
    public BusinessException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode, cause, args);
    }
    
    /**
     * 构造函数（自定义消息）
     * 
     * @param errorCode 错误代码
     * @param message 自定义消息
     * @param args 消息参数
     */
    public BusinessException(ErrorCode errorCode, String message, Object... args) {
        super(errorCode, message, args);
    }
    
    /**
     * 构造函数（自定义消息，带原因）
     * 
     * @param errorCode 错误代码
     * @param message 自定义消息
     * @param cause 异常原因
     * @param args 消息参数
     */
    public BusinessException(ErrorCode errorCode, String message, Throwable cause, Object... args) {
        super(errorCode, message, cause, args);
    }
} 