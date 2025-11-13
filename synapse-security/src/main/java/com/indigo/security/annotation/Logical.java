package com.indigo.security.annotation;

/**
 * 逻辑运算符枚举
 * 用于权限和角色的逻辑判断
 * 
 * @author 史偕成
 * @date 2025/01/XX
 */
public enum Logical {
    /**
     * 逻辑与（AND）
     * 要求所有条件都满足
     */
    AND,
    
    /**
     * 逻辑或（OR）
     * 要求至少一个条件满足
     */
    OR
}

