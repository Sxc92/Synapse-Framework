package com.indigo.core.exception.enums;

/**
 * 错误码接口
 * 定义错误码的基本规范，支持业务模块扩展自定义错误码
 * 
 * @author 史偕成
 * @date 2025/03/21
 */
public interface ErrorCode {
    
    /**
     * 获取错误码
     * 
     * @return 错误码字符串
     */
    String getCode();
    
    /**
     * 获取错误码对应的消息键
     * 用于国际化消息解析
     * 格式：error.{错误码小写}
     * 例如：error.sys000, error.iam002
     * 
     * @return 消息键
     */
    default String getMessageKey() {
        return "error." + getCode().toLowerCase();
    }
}