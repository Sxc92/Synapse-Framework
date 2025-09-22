package com.indigo.core.exception;

import com.indigo.core.constants.ErrorCode;
import lombok.Getter;

/**
 * 异常基类
 * 提供统一的异常结构和错误码支持
 * 
 * @author 史偕成
 * @date 2025/01/27
 */
@Getter
public abstract class BaseException extends RuntimeException {
    
    private final String code;
    private final Object[] args;
    
    /**
     * 构造函数
     * 
     * @param errorCode 错误代码
     * @param args 消息参数
     */
    public BaseException(ErrorCode errorCode, Object... args) {
        super(errorCode.getCode()); // 使用错误码作为默认消息
        this.code = errorCode.getCode();
        this.args = args;
    }
    
    /**
     * 构造函数（带原因）
     * 
     * @param errorCode 错误代码
     * @param cause 异常原因
     * @param args 消息参数
     */
    public BaseException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode.getCode(), cause); // 使用错误码作为默认消息
        this.code = errorCode.getCode();
        this.args = args;
    }
    
    /**
     * 构造函数（自定义消息）
     * 
     * @param errorCode 错误代码
     * @param message 自定义消息
     * @param args 消息参数
     */
    public BaseException(ErrorCode errorCode, String message, Object... args) {
        super(message);
        this.code = errorCode.getCode();
        this.args = args;
    }
    
    /**
     * 构造函数（自定义消息，带原因）
     * 
     * @param errorCode 错误代码
     * @param message 自定义消息
     * @param cause 异常原因
     * @param args 消息参数
     */
    public BaseException(ErrorCode errorCode, String message, Throwable cause, Object... args) {
        super(message, cause);
        this.code = errorCode.getCode();
        this.args = args;
    }
}
