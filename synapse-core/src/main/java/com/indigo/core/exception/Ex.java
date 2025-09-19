package com.indigo.core.exception;

import com.indigo.core.exception.enums.ErrorCode;

/**
 * 异常工具类
 * 提供优雅的异常抛出方法
 * 
 * @author 史偕成
 * @date 2025/01/27
 */
public class Ex {
    
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
}
