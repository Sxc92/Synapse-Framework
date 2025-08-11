package com.indigo.cache.extension.lock.resource;

import com.indigo.cache.extension.lock.DistributedLockService;
import com.indigo.cache.extension.lock.FairLockService;
import com.indigo.cache.extension.lock.ReadWriteLockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 资源池管理器
 * 负责预分配和快速获取资源
 * 采用懒加载策略，避免启动时的依赖问题
 */
@Slf4j
@Component
public class ResourcePool {
    
    // 预分配服务池 - 核心服务预分配
    private final Map<Class<?>, Object> preallocatedServices = new ConcurrentHashMap<>();
    
    // 资源状态管理
    private final Map<ResourceType, ResourceState> resourceStates = new ConcurrentHashMap<>();
    
    // 依赖注入的服务（懒加载）
    @Autowired(required = false)
    private DistributedLockService distributedLockService;
    
    @Autowired(required = false)
    private ReadWriteLockService readWriteLockService;
    
    @Autowired(required = false)
    private FairLockService fairLockService;
    
    /**
     * 初始化预分配池
     * 采用懒加载策略，不在这里创建服务实例
     */
    public void initializePreallocatedPool() {
            // 标记核心服务为可用
            markResourceAvailable(ResourceType.CORE_SERVICE);
            
        log.info("预分配资源池初始化完成（懒加载模式）");
    }
    
    /**
     * 获取预分配的服务
     * 采用懒加载策略，首次使用时才获取
     */
    @SuppressWarnings("unchecked")
    public <T> T getPreallocatedService(Class<T> serviceClass) {
        T service = (T) preallocatedServices.get(serviceClass);
        if (service == null) {
            // 懒加载：从Spring容器获取服务
            service = getServiceFromSpring(serviceClass);
            if (service != null) {
                preallocatedServices.put(serviceClass, service);
                log.debug("懒加载服务: {}", serviceClass.getSimpleName());
            }
        }
        return service;
    }
    
    /**
     * 从Spring容器获取服务
     */
    @SuppressWarnings("unchecked")
    private <T> T getServiceFromSpring(Class<T> serviceClass) {
        try {
            if (serviceClass == DistributedLockService.class) {
                return (T) distributedLockService;
            } else if (serviceClass == ReadWriteLockService.class) {
                return (T) readWriteLockService;
            } else if (serviceClass == FairLockService.class) {
                return (T) fairLockService;
            }
            
            log.warn("未找到服务类型: {}", serviceClass.getSimpleName());
            return null;
        } catch (Exception e) {
            log.error("从Spring容器获取服务失败: {}", serviceClass, e);
            return null;
        }
    }
    
    /**
     * 检查服务是否可用
     */
    public boolean isServiceAvailable(Class<?> serviceClass) {
        if (preallocatedServices.containsKey(serviceClass)) {
            return true;
        }
        
        // 检查Spring容器中的服务
        return getServiceFromSpring(serviceClass) != null;
    }
    
    /**
     * 预加载所有可用服务
     * 可选调用，用于主动预热
     */
    public void preloadAllServices() {
        log.info("开始预加载所有服务...");
        
        // 预加载分布式锁服务
        if (distributedLockService != null) {
            preallocatedServices.put(DistributedLockService.class, distributedLockService);
            log.debug("预加载分布式锁服务");
        }
        
        // 预加载读写锁服务
        if (readWriteLockService != null) {
            preallocatedServices.put(ReadWriteLockService.class, readWriteLockService);
            log.debug("预加载读写锁服务");
        }
        
        // 预加载公平锁服务
        if (fairLockService != null) {
            preallocatedServices.put(FairLockService.class, fairLockService);
            log.debug("预加载公平锁服务");
        }
        
        log.info("服务预加载完成，共加载 {} 个服务", preallocatedServices.size());
    }
    
    /**
     * 标记资源为可用
     */
    public void markResourceAvailable(ResourceType resourceType) {
        ResourceState state = resourceStates.computeIfAbsent(
            resourceType, k -> new ResourceState());
        state.setAvailable(true);
        state.setLastAccessTime(System.currentTimeMillis());
        log.debug("资源标记为可用: {}", resourceType);
    }
    
    /**
     * 标记资源为已释放
     */
    public void markResourceReleased(ResourceType resourceType) {
        ResourceState state = resourceStates.get(resourceType);
        if (state != null) {
            state.setAvailable(false);
            state.setReleaseTime(System.currentTimeMillis());
            log.debug("资源标记为已释放: {}", resourceType);
        }
    }
    
    /**
     * 获取资源状态
     */
    public ResourceState getResourceState(ResourceType resourceType) {
        return resourceStates.get(resourceType);
    }
    
    /**
     * 检查资源是否可用
     */
    public boolean isResourceAvailable(ResourceType resourceType) {
        ResourceState state = resourceStates.get(resourceType);
        return state != null && state.isAvailable();
    }
    
    /**
     * 更新资源访问时间
     */
    public void updateResourceAccess(ResourceType resourceType) {
        ResourceState state = resourceStates.get(resourceType);
        if (state != null) {
            state.incrementAccessCount();
        }
    }
    
    /**
     * 清理预分配池
     */
    public void cleanupPreallocatedPool() {
        preallocatedServices.clear();
        log.debug("预分配池清理完成");
    }
    
    /**
     * 获取预分配池大小
     */
    public int getPreallocatedPoolSize() {
        return preallocatedServices.size();
    }
    
    /**
     * 获取资源状态统计
     */
    public Map<ResourceType, ResourceState> getResourceStates() {
        return new ConcurrentHashMap<>(resourceStates);
    }
} 