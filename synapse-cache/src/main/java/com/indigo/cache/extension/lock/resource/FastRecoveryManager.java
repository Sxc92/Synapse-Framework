package com.indigo.cache.extension.lock.resource;

import com.indigo.cache.extension.lock.DistributedLockService;
import com.indigo.cache.extension.lock.FairLockService;
import com.indigo.cache.extension.lock.ReadWriteLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 快速恢复管理器
 * 负责资源的快速恢复逻辑
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FastRecoveryManager {
    
    private final ResourcePool resourcePool;
    private final Map<ResourceType, RecoveryState> recoveryStates = new ConcurrentHashMap<>();
    
    /**
     * 快速恢复资源
     * 目标：5分钟内释放的资源，在100ms内恢复
     */
    public <T> T fastRecover(ResourceType resourceType, Class<T> resourceClass) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 检查是否已恢复
            if (isResourceAvailable(resourceType)) {
                T resource = getResource(resourceType, resourceClass);
                if (resource != null) {
                    log.debug("资源已可用，直接获取: {} - 耗时: {}ms", resourceType, 
                             System.currentTimeMillis() - startTime);
                    return resource;
                }
            }
            
            // 2. 执行快速恢复
            T resource = executeFastRecovery(resourceType, resourceClass);
            
            // 3. 记录恢复时间
            long recoveryTime = System.currentTimeMillis() - startTime;
            recordRecoveryTime(resourceType, recoveryTime);
            
            log.info("资源快速恢复完成: {} - 耗时: {}ms", resourceType, recoveryTime);
            return resource;
            
        } catch (Exception e) {
            log.error("快速恢复失败: {}", resourceType, e);
            // 降级到标准恢复
            return standardRecovery(resourceType, resourceClass);
        }
    }
    
    /**
     * 执行快速恢复
     */
    private <T> T executeFastRecovery(ResourceType resourceType, Class<T> resourceClass) {
        switch (resourceType) {
            case CORE_SERVICE:
                return fastRecoverCoreService(resourceClass);
            case BUSINESS_CACHE:
                return fastRecoverBusinessCache(resourceClass);
            case TEMPORARY:
                return fastRecoverTemporaryResource(resourceClass);
            default:
                return standardRecovery(resourceType, resourceClass);
        }
    }
    
    /**
     * 快速恢复核心服务
     */
    @SuppressWarnings("unchecked")
    private <T> T fastRecoverCoreService(Class<T> resourceClass) {
        if (DistributedLockService.class.isAssignableFrom(resourceClass)) {
            // 从预分配池快速获取
            return (T) resourcePool.getPreallocatedService(DistributedLockService.class);
        }
        if (ReadWriteLockService.class.isAssignableFrom(resourceClass)) {
            return (T) resourcePool.getPreallocatedService(ReadWriteLockService.class);
        }
        if (FairLockService.class.isAssignableFrom(resourceClass)) {
            return (T) resourcePool.getPreallocatedService(FairLockService.class);
        }
        return null;
    }
    
    /**
     * 快速恢复业务缓存
     */
    private <T> T fastRecoverBusinessCache(Class<T> resourceClass) {
        // 业务缓存的快速恢复逻辑
        // 这里可以根据具体需求实现
        return null;
    }
    
    /**
     * 快速恢复临时资源
     */
    private <T> T fastRecoverTemporaryResource(Class<T> resourceClass) {
        // 临时资源的快速恢复逻辑
        // 这里可以根据具体需求实现
        return null;
    }
    
    /**
     * 标准恢复（降级策略）
     */
    private <T> T standardRecovery(ResourceType resourceType, Class<T> resourceClass) {
        log.warn("执行标准恢复: {}", resourceType);
        
        try {
            // 标准恢复逻辑
            T resource = createResourceStandard(resourceType, resourceClass);
            if (resource != null) {
                resourcePool.markResourceAvailable(resourceType);
            }
            return resource;
        } catch (Exception e) {
            log.error("标准恢复也失败: {}", resourceType, e);
            return null;
        }
    }
    
    /**
     * 标准方式创建资源
     */
    private <T> T createResourceStandard(ResourceType resourceType, Class<T> resourceClass) {
        try {
            // 使用Spring容器创建Bean
            // 这里可以根据具体需求实现
            return null;
        } catch (Exception e) {
            log.error("标准创建资源失败: {}", resourceType, e);
            return null;
        }
    }
    
    /**
     * 检查资源是否可用
     */
    private boolean isResourceAvailable(ResourceType resourceType) {
        return resourcePool.isResourceAvailable(resourceType);
    }
    
    /**
     * 获取资源
     */
    private <T> T getResource(ResourceType resourceType, Class<T> resourceClass) {
        // 根据资源类型获取对应的资源
        switch (resourceType) {
            case CORE_SERVICE:
                return getCoreServiceResource(resourceClass);
            default:
                return null;
        }
    }
    
    /**
     * 获取核心服务资源
     */
    private <T> T getCoreServiceResource(Class<T> resourceClass) {
        return resourcePool.getPreallocatedService(resourceClass);
    }
    
    /**
     * 记录恢复时间
     */
    private void recordRecoveryTime(ResourceType resourceType, long recoveryTime) {
        RecoveryState state = recoveryStates.computeIfAbsent(
            resourceType, k -> new RecoveryState());
        state.setRecoveryDuration(recoveryTime);
        state.setRecovering(false);
    }
    
    /**
     * 获取恢复状态
     */
    public RecoveryState getRecoveryState(ResourceType resourceType) {
        return recoveryStates.get(resourceType);
    }
    
    /**
     * 获取所有恢复状态
     */
    public Map<ResourceType, RecoveryState> getAllRecoveryStates() {
        return new ConcurrentHashMap<>(recoveryStates);
    }
    
    /**
     * 重置恢复状态
     */
    public void resetRecoveryState(ResourceType resourceType) {
        RecoveryState state = recoveryStates.get(resourceType);
        if (state != null) {
            state.reset();
        }
    }
    
    /**
     * 获取资源池
     */
    public ResourcePool getResourcePool() {
        return resourcePool;
    }
} 