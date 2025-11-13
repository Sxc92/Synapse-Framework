package com.indigo.cache.extension.lock;

import com.indigo.cache.config.LockAutoConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 分布式死锁检测器
 * 基于资源分配图的死锁检测算法
 * 
 * 特性：
 * 1. 定期检测死锁：定时扫描资源分配图
 * 2. 死锁预防：通过超时机制预防死锁
 * 3. 死锁恢复：自动释放超时锁
 * 4. 资源依赖图：构建线程间的资源依赖关系
 * 
 * <p><b>注意：</b>此类通过 {@link LockAutoConfiguration} 中的 {@code @Bean} 方法注册为 Bean，
 * 不需要 {@code @Component} 注解。如果同时使用 {@code @Component} 和 {@code @Bean}，
 * 会导致创建多个 Bean 实例，引发冲突。
 *
 * @author 史偕成
 * @date 2025/01/08
 */
@Slf4j
public class DeadlockDetector {

    protected final ScheduledExecutorService scheduler;

    // 检测间隔（毫秒）
    private static final long DETECTION_INTERVAL = 5000;
    // 锁超时时间（秒）
    private static final int LOCK_TIMEOUT = 30;
    // 死锁检测超时时间（秒）
    private static final int DEADLOCK_TIMEOUT = 60;

    // 资源分配图：线程 -> 持有的锁集合
    private final ConcurrentHashMap<String, Set<String>> threadLocks = new ConcurrentHashMap<>();
    // 等待图：线程 -> 等待的锁集合
    private final ConcurrentHashMap<String, Set<String>> threadWaits = new ConcurrentHashMap<>();
    // 锁持有者：锁 -> 持有线程
    private final ConcurrentHashMap<String, String> lockHolders = new ConcurrentHashMap<>();
    // 锁等待者：锁 -> 等待线程集合
    private final ConcurrentHashMap<String, Set<String>> lockWaiters = new ConcurrentHashMap<>();
    // 线程超时时间：线程 -> 超时时间戳
    private final ConcurrentHashMap<String, Long> threadTimeouts = new ConcurrentHashMap<>();

    private final AtomicBoolean running = new AtomicBoolean(true);

    public DeadlockDetector(@Qualifier("lockScheduledExecutor") ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
        // 启动死锁检测任务
        startDeadlockDetection();
    }

    /**
     * 记录锁获取
     *
     * @param threadId 线程ID
     * @param lockKey 锁键
     */
    public void recordLockAcquired(String threadId, String lockKey) {
        threadLocks.computeIfAbsent(threadId, k -> ConcurrentHashMap.newKeySet()).add(lockKey);
        lockHolders.put(lockKey, threadId);
        threadTimeouts.put(threadId, System.currentTimeMillis() + LOCK_TIMEOUT * 1000L);

        log.debug("[DeadlockDetector] 记录锁获取: threadId={} lockKey={}", threadId, lockKey);
    }

    /**
     * 记录锁释放
     *
     * @param threadId 线程ID
     * @param lockKey 锁键
     */
    public void recordLockReleased(String threadId, String lockKey) {
        Set<String> locks = threadLocks.get(threadId);
        if (locks != null) {
            locks.remove(lockKey);
            if (locks.isEmpty()) {
                threadLocks.remove(threadId);
                threadTimeouts.remove(threadId);
            }
        }
        lockHolders.remove(lockKey);

        log.info("[DeadlockDetector] 记录锁释放: threadId={} lockKey={}", threadId, lockKey);
    }

    /**
     * 记录锁等待开始
     *
     * @param threadId 线程ID
     * @param lockKey 锁键
     */
    public void recordLockWaitStart(String threadId, String lockKey) {
        threadWaits.computeIfAbsent(threadId, k -> ConcurrentHashMap.newKeySet()).add(lockKey);
        lockWaiters.computeIfAbsent(lockKey, k -> ConcurrentHashMap.newKeySet()).add(threadId);

        log.info("[DeadlockDetector] 记录锁等待: threadId={} lockKey={}", threadId, lockKey);
    }

    /**
     * 记录锁等待结束
     *
     * @param threadId 线程ID
     * @param lockKey 锁键
     */
    public void recordLockWaitEnd(String threadId, String lockKey) {
        Set<String> waits = threadWaits.get(threadId);
        if (waits != null) {
            waits.remove(lockKey);
            if (waits.isEmpty()) {
                threadWaits.remove(threadId);
            }
        }

        Set<String> waiters = lockWaiters.get(lockKey);
        if (waiters != null) {
            waiters.remove(threadId);
            if (waiters.isEmpty()) {
                lockWaiters.remove(lockKey);
            }
        }

        log.info("[DeadlockDetector] 记录锁等待结束: threadId={} lockKey={}", threadId, lockKey);
    }

    /**
     * 启动死锁检测任务
     */
    private void startDeadlockDetection() {
        scheduler.scheduleWithFixedDelay(this::detectDeadlocks,
            DETECTION_INTERVAL, DETECTION_INTERVAL, TimeUnit.MILLISECONDS);
        log.debug("[DeadlockDetector] 死锁检测任务已启动，检测间隔: {}ms", DETECTION_INTERVAL);
    }

    /**
     * 死锁检测主方法
     */
    private void detectDeadlocks() {
        if (!running.get()) return;

        try {
            // 清理超时线程
            cleanupTimeoutThreads();

            // 检测死锁
            List<Set<String>> deadlockCycles = findDeadlockCycles();

            if (!deadlockCycles.isEmpty()) {
                log.warn("[DeadlockDetector] 检测到死锁，死锁环数量: {}", deadlockCycles.size());
                for (int i = 0; i < deadlockCycles.size(); i++) {
                    Set<String> cycle = deadlockCycles.get(i);
                    log.warn("[DeadlockDetector] 死锁环 {}: {}", i + 1, cycle);
                }

                // 处理死锁
                handleDeadlocks(deadlockCycles);
            }
        } catch (Exception e) {
            log.error("[DeadlockDetector] 死锁检测异常", e);
        }
    }

    /**
     * 清理超时线程
     */
    private void cleanupTimeoutThreads() {
        long now = System.currentTimeMillis();
        List<String> timeoutThreads = new ArrayList<>();

        for (Map.Entry<String, Long> entry : threadTimeouts.entrySet()) {
            if (entry.getValue() < now) {
                timeoutThreads.add(entry.getKey());
            }
        }

        // 只在有超时线程时记录警告
        if (!timeoutThreads.isEmpty()) {
            log.warn("[DeadlockDetector] 检测到 {} 个超时线程，强制释放锁", timeoutThreads.size());

            for (String threadId : timeoutThreads) {
                log.info("[DeadlockDetector] 线程超时，强制释放锁: threadId={}", threadId);
                forceReleaseThreadLocks(threadId);
            }
        }
    }

    /**
     * 强制释放线程的所有锁
     *
     * @param threadId 线程ID
     */
    protected void forceReleaseThreadLocks(String threadId) {
        Set<String> locks = threadLocks.get(threadId);
        if (locks != null) {
            for (String lockKey : new HashSet<>(locks)) {
                // 从Redis中强制删除锁
                String lockName = extractLockName(lockKey);
                String key = extractKey(lockKey);
                if (lockName != null && key != null) {
                    try {
                        // 这里需要根据实际的锁实现来强制释放
                        // 暂时记录日志
                        log.warn("[DeadlockDetector] 强制释放锁: lockKey={}", lockKey);
                    } catch (Exception e) {
                        log.error("[DeadlockDetector] 强制释放锁失败: lockKey={}", lockKey, e);
                    }
                }
                recordLockReleased(threadId, lockKey);
            }
        }
        
        threadWaits.remove(threadId);
        threadTimeouts.remove(threadId);
    }

    /**
     * 查找死锁环
     * 
     * @return 死锁环列表
     */
    private List<Set<String>> findDeadlockCycles() {
        List<Set<String>> cycles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        
        for (String threadId : threadLocks.keySet()) {
            if (!visited.contains(threadId)) {
                Set<String> cycle = new HashSet<>();
                if (hasCycle(threadId, visited, recursionStack, cycle)) {
                    cycles.add(new HashSet<>(cycle));
                }
            }
        }
        
        return cycles;
    }

    /**
     * 使用DFS检测环
     * 
     * @param threadId 当前线程ID
     * @param visited 已访问的线程
     * @param recursionStack 递归栈
     * @param cycle 当前环
     * @return 是否存在环
     */
    private boolean hasCycle(String threadId, Set<String> visited, Set<String> recursionStack, Set<String> cycle) {
        if (recursionStack.contains(threadId)) {
            cycle.add(threadId);
            return true;
        }
        
        if (visited.contains(threadId)) {
            return false;
        }
        
        visited.add(threadId);
        recursionStack.add(threadId);
        cycle.add(threadId);
        
        Set<String> waits = threadWaits.get(threadId);
        if (waits != null) {
            for (String lockKey : waits) {
                String holder = lockHolders.get(lockKey);
                if (holder != null && !holder.equals(threadId)) {
                    if (hasCycle(holder, visited, recursionStack, cycle)) {
                        return true;
                    }
                }
            }
        }
        
        recursionStack.remove(threadId);
        cycle.remove(threadId);
        return false;
    }

    /**
     * 处理死锁
     * 
     * @param deadlockCycles 死锁环列表
     */
    private void handleDeadlocks(List<Set<String>> deadlockCycles) {
        for (Set<String> cycle : deadlockCycles) {
            // 选择优先级最低的线程进行回滚
            String victimThread = selectVictimThread(cycle);
            if (victimThread != null) {
                log.warn("[DeadlockDetector] 选择牺牲线程: {}", victimThread);
                forceReleaseThreadLocks(victimThread);
            }
        }
    }

    /**
     * 选择牺牲线程（优先级最低的线程）
     * 
     * @param cycle 死锁环
     * @return 牺牲线程ID
     */
    private String selectVictimThread(Set<String> cycle) {
        // 简单的策略：选择持有锁最少的线程
        String victim = null;
        int minLocks = Integer.MAX_VALUE;
        
        for (String threadId : cycle) {
            Set<String> locks = threadLocks.get(threadId);
            int lockCount = locks != null ? locks.size() : 0;
            if (lockCount < minLocks) {
                minLocks = lockCount;
                victim = threadId;
            }
        }
        
        return victim;
    }

    /**
     * 从锁键中提取锁名称
     * 
     * @param lockKey 锁键
     * @return 锁名称
     */
    private String extractLockName(String lockKey) {
        try {
            // 假设锁键格式为: module:lockName:key
            String[] parts = lockKey.split(":");
            if (parts.length >= 3) {
                return parts[1];
            }
        } catch (Exception e) {
            log.error("[DeadlockDetector] 提取锁名称失败: {}", lockKey, e);
        }
        return null;
    }

    /**
     * 从锁键中提取业务键
     * 
     * @param lockKey 锁键
     * @return 业务键
     */
    private String extractKey(String lockKey) {
        try {
            // 假设锁键格式为: module:lockName:key
            String[] parts = lockKey.split(":");
            if (parts.length >= 3) {
                return parts[2];
            }
        } catch (Exception e) {
            log.error("[DeadlockDetector] 提取业务键失败: {}", lockKey, e);
        }
        return null;
    }

    /**
     * 获取当前资源分配图状态
     * 
     * @return 状态信息
     */
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("threadLocks", new HashMap<>(threadLocks));
        status.put("threadWaits", new HashMap<>(threadWaits));
        status.put("lockHolders", new HashMap<>(lockHolders));
        status.put("lockWaiters", new HashMap<>(lockWaiters));
        status.put("threadTimeouts", new HashMap<>(threadTimeouts));
        return status;
    }

    /**
     * 停止死锁检测
     */
    public void stop() {
        running.set(false);
        log.info("[DeadlockDetector] 死锁检测已停止");
    }
} 