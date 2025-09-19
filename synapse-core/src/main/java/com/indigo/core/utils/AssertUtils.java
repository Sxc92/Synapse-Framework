package com.indigo.core.utils;

import com.indigo.core.exception.SynapseException;
import com.indigo.core.exception.enums.ErrorCode;
import com.indigo.core.exception.enums.StandardErrorCode;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * 断言工具类，用于参数校验和业务断言
 * 
 * @author 史偕成
 * @date 2025/03/21
 **/
public class AssertUtils {
    
    private AssertUtils() {
        throw new IllegalStateException("Utility class");
    }
    
    /**
     * 断言对象不为空
     *
     * @param object 要检查的对象
     * @param message 错误消息
     * @throws SynapseException 如果对象为空
     */
    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new SynapseException(StandardErrorCode.PARAM_ERROR, message);
        }
    }
    
    /**
     * 断言对象不为空
     *
     * @param object 要检查的对象
     * @param errorCode 错误码
     * @param message 错误消息
     * @throws SynapseException 如果对象为空
     */
    public static void notNull(Object object, ErrorCode errorCode, String message) {
        if (object == null) {
            throw new SynapseException(errorCode, message);
        }
    }
    
    /**
     * 断言字符串不为空
     *
     * @param text 要检查的字符串
     * @param message 错误消息
     * @throws SynapseException 如果字符串为空
     */
    public static void hasText(String text, String message) {
        if (!StringUtils.hasText(text)) {
            throw new SynapseException(StandardErrorCode.PARAM_ERROR, message);
        }
    }
    
    /**
     * 断言字符串不为空
     *
     * @param text 要检查的字符串
     * @param errorCode 错误码
     * @param message 错误消息
     * @throws SynapseException 如果字符串为空
     */
    public static void hasText(String text, ErrorCode errorCode, String message) {
        if (!StringUtils.hasText(text)) {
            throw new SynapseException(errorCode, message);
        }
    }
    
    /**
     * 断言集合不为空
     *
     * @param collection 要检查的集合
     * @param message 错误消息
     * @throws SynapseException 如果集合为空
     */
    public static void notEmpty(Collection<?> collection, String message) {
        if (CollectionUtils.isEmpty(collection)) {
            throw new SynapseException(StandardErrorCode.PARAM_ERROR, message);
        }
    }
    
    /**
     * 断言集合不为空
     *
     * @param collection 要检查的集合
     * @param errorCode 错误码
     * @param message 错误消息
     * @throws SynapseException 如果集合为空
     */
    public static void notEmpty(Collection<?> collection, ErrorCode errorCode, String message) {
        if (CollectionUtils.isEmpty(collection)) {
            throw new SynapseException(errorCode, message);
        }
    }
    
    /**
     * 断言Map不为空
     *
     * @param map 要检查的Map
     * @param message 错误消息
     * @throws SynapseException 如果Map为空
     */
    public static void notEmpty(Map<?, ?> map, String message) {
        if (CollectionUtils.isEmpty(map)) {
            throw new SynapseException(StandardErrorCode.PARAM_ERROR, message);
        }
    }
    
    /**
     * 断言Map不为空
     *
     * @param map 要检查的Map
     * @param errorCode 错误码
     * @param message 错误消息
     * @throws SynapseException 如果Map为空
     */
    public static void notEmpty(Map<?, ?> map, ErrorCode errorCode, String message) {
        if (CollectionUtils.isEmpty(map)) {
            throw new SynapseException(errorCode, message);
        }
    }
    
    /**
     * 断言条件为true
     *
     * @param expression 要检查的条件
     * @param message 错误消息
     * @throws SynapseException 如果条件为false
     */
    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new SynapseException(StandardErrorCode.PARAM_ERROR, message);
        }
    }
    
    /**
     * 断言条件为true
     *
     * @param expression 要检查的条件
     * @param errorCode 错误码
     * @param message 错误消息
     * @throws SynapseException 如果条件为false
     */
    public static void isTrue(boolean expression, ErrorCode errorCode, String message) {
        if (!expression) {
            throw new SynapseException(errorCode, message);
        }
    }
    
    /**
     * 断言两个对象相等
     *
     * @param o1 第一个对象
     * @param o2 第二个对象
     * @param message 错误消息
     * @throws SynapseException 如果对象不相等
     */
    public static void equals(Object o1, Object o2, String message) {
        if (!Objects.equals(o1, o2)) {
            throw new SynapseException(StandardErrorCode.PARAM_ERROR, message);
        }
    }
    
    /**
     * 断言两个对象相等
     *
     * @param o1 第一个对象
     * @param o2 第二个对象
     * @param errorCode 错误码
     * @param message 错误消息
     * @throws SynapseException 如果对象不相等
     */
    public static void equals(Object o1, Object o2, ErrorCode errorCode, String message) {
        if (!Objects.equals(o1, o2)) {
            throw new SynapseException(errorCode, message);
        }
    }
    
    /**
     * 断言对象为null
     *
     * @param object 要检查的对象
     * @param message 错误消息
     * @throws SynapseException 如果对象不为null
     */
    public static void isNull(Object object, String message) {
        if (object != null) {
            throw new SynapseException(StandardErrorCode.PARAM_ERROR, message);
        }
    }
    
    /**
     * 断言对象为null
     *
     * @param object 要检查的对象
     * @param errorCode 错误码
     * @param message 错误消息
     * @throws SynapseException 如果对象不为null
     */
    public static void isNull(Object object, ErrorCode errorCode, String message) {
        if (object != null) {
            throw new SynapseException(errorCode, message);
        }
    }
    
    /**
     * 断言数组不为空
     *
     * @param array 要检查的数组
     * @param message 错误消息
     * @throws SynapseException 如果数组为空
     */
    public static void notEmpty(Object[] array, String message) {
        if (array == null || array.length == 0) {
            throw new SynapseException(StandardErrorCode.PARAM_ERROR, message);
        }
    }
    
    /**
     * 断言数组不为空
     *
     * @param array 要检查的数组
     * @param errorCode 错误码
     * @param message 错误消息
     * @throws SynapseException 如果数组为空
     */
    public static void notEmpty(Object[] array, ErrorCode errorCode, String message) {
        if (array == null || array.length == 0) {
            throw new SynapseException(errorCode, message);
        }
    }
    
    /**
     * 断言对象不为空，并返回该对象
     *
     * @param object 要检查的对象
     * @param message 错误消息
     * @param <T> 对象类型
     * @return 原对象
     * @throws SynapseException 如果对象为空
     */
    public static <T> T requireNonNull(T object, String message) {
        if (object == null) {
            throw new SynapseException(StandardErrorCode.PARAM_ERROR, message);
        }
        return object;
    }
    
    /**
     * 断言对象不为空，并返回该对象
     *
     * @param object 要检查的对象
     * @param errorCode 错误码
     * @param message 错误消息
     * @param <T> 对象类型
     * @return 原对象
     * @throws SynapseException 如果对象为空
     */
    public static <T> T requireNonNull(T object, ErrorCode errorCode, String message) {
        if (object == null) {
            throw new SynapseException(errorCode, message);
        }
        return object;
    }
} 