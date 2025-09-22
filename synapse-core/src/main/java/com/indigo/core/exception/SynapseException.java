package com.indigo.core.exception;

import com.indigo.core.constants.ErrorCode;

/**
 * Synapse异常
 * 统一的异常类，用于处理所有类型的异常情况
 * 只需要传入 ErrorCode 即可，不需要其他异常类
 * 
 * @author 史偕成
 * @date 2025/01/27
 */
public class SynapseException extends BaseException {
    
    /**
     * 构造函数
     * 
     * @param errorCode 错误代码
     * @param args 消息参数
     */
    public SynapseException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
    
    /**
     * 构造函数（带原因）
     * 
     * @param errorCode 错误代码
     * @param cause 异常原因
     * @param args 消息参数
     */
    public SynapseException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode, cause, args);
    }
    
    /**
     * 构造函数（自定义消息）
     * 
     * @param errorCode 错误代码
     * @param message 自定义消息
     * @param args 消息参数
     */
    public SynapseException(ErrorCode errorCode, String message, Object... args) {
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
    public SynapseException(ErrorCode errorCode, String message, Throwable cause, Object... args) {
        super(errorCode, message, cause, args);
    }
    
    // ==================== 静态工厂方法 ====================
    
    /**
     * 创建异常（不抛出）
     * 
     * @param errorCode 错误代码
     * @param args 消息参数
     * @return SynapseException实例
     */
    public static SynapseException of(ErrorCode errorCode, Object... args) {
        return new SynapseException(errorCode, args);
    }
    
    /**
     * 创建异常（带原因，不抛出）
     * 
     * @param errorCode 错误代码
     * @param cause 异常原因
     * @param args 消息参数
     * @return SynapseException实例
     */
    public static SynapseException of(ErrorCode errorCode, Throwable cause, Object... args) {
        return new SynapseException(errorCode, cause, args);
    }
    
    /**
     * 创建异常（自定义消息，不抛出）
     * 
     * @param errorCode 错误代码
     * @param message 自定义消息
     * @param args 消息参数
     * @return SynapseException实例
     */
    public static SynapseException of(ErrorCode errorCode, String message, Object... args) {
        return new SynapseException(errorCode, message, args);
    }
    
    // ==================== 直接抛出方法 ====================
    
    /**
     * 抛出异常
     * 
     * @param errorCode 错误代码
     * @param args 消息参数
     * @throws SynapseException
     */
    public static void throwEx(ErrorCode errorCode, Object... args) throws SynapseException {
        throw new SynapseException(errorCode, args);
    }
    
    /**
     * 抛出异常（带原因）
     * 
     * @param errorCode 错误代码
     * @param cause 异常原因
     * @param args 消息参数
     * @throws SynapseException
     */
    public static void throwEx(ErrorCode errorCode, Throwable cause, Object... args) throws SynapseException {
        throw new SynapseException(errorCode, cause, args);
    }
    
    /**
     * 抛出异常（自定义消息）
     * 
     * @param errorCode 错误代码
     * @param message 自定义消息
     * @param args 消息参数
     * @throws SynapseException
     */
    public static void throwEx(ErrorCode errorCode, String message, Object... args) throws SynapseException {
        throw new SynapseException(errorCode, message, args);
    }
}
