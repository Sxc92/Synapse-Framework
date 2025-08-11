package com.indigo.cache.extension.lock.resource;

/**
 * 资源类型枚举
 * 定义不同资源的释放级别
 */
public enum ResourceType {
    
    /**
     * 基础设施层 - 永不释放
     */
    INFRASTRUCTURE("infrastructure", ReleaseLevel.NEVER),
    
    /**
     * 监控层 - 永不释放
     */
    MONITORING("monitoring", ReleaseLevel.NEVER),
    
    /**
     * 核心服务层 - 谨慎释放
     */
    CORE_SERVICE("core_service", ReleaseLevel.CAREFUL),
    
    /**
     * 业务缓存层 - 可释放
     */
    BUSINESS_CACHE("business_cache", ReleaseLevel.RELEASABLE),
    
    /**
     * 临时资源层 - 优先释放
     */
    TEMPORARY("temporary", ReleaseLevel.PRIORITY);
    
    private final String name;
    private final ReleaseLevel releaseLevel;
    
    ResourceType(String name, ReleaseLevel releaseLevel) {
        this.name = name;
        this.releaseLevel = releaseLevel;
    }
    
    public String getName() {
        return name;
    }
    
    public ReleaseLevel getReleaseLevel() {
        return releaseLevel;
    }
} 