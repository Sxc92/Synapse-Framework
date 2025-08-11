package com.indigo.cache.extension.lock.resource;

/**
 * 释放级别枚举
 * 定义资源的释放策略
 */
public enum ReleaseLevel {
    
    /**
     * 永不释放
     */
    NEVER("never", "永不释放"),
    
    /**
     * 谨慎释放
     */
    CAREFUL("careful", "谨慎释放"),
    
    /**
     * 可释放
     */
    RELEASABLE("releasable", "可释放"),
    
    /**
     * 优先释放
     */
    PRIORITY("priority", "优先释放");
    
    private final String code;
    private final String description;
    
    ReleaseLevel(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
} 