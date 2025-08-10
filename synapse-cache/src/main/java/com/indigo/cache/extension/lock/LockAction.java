package com.indigo.cache.extension.lock;

/**
 * 统一的锁操作接口
 * 用于在获取锁后执行业务逻辑
 *
 * @param <T> 返回值类型
 * @author 史偕成
 * @date 2025/01/08
 */
@FunctionalInterface
public interface LockAction<T> {
    
    /**
     * 执行业务逻辑
     *
     * @return 执行结果
     * @throws Exception 执行过程中可能抛出的异常
     */
    T execute() throws Exception;
} 