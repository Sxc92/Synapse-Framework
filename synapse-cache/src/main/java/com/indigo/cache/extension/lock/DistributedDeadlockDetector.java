package com.indigo.cache.extension.lock;

import com.indigo.cache.infrastructure.RedisService;
import com.indigo.cache.config.DistributedDeadlockProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 分布式死锁检测器
 * 基于Redis的全局资源分配图进行跨节点死锁检测
 * <p>
 * 特性：
 * 1. 混合检测策略：本地检测 + 全局协调检测
 * 2. 状态同步：定期同步本地状态到Redis全局图
 * 3. 分布式算法：支持跨节点的死锁检测
 * 4. 智能处理：本地死锁立即处理，全局死锁协调处理
 * 5. 容错机制：单点故障不影响整体检测
 * 
 * <p><b>注意：</b>此类通过 {@link LockAutoConfiguration} 中的 {@code @Bean} 方法注册为 Bean，
 * 不需要 {@code @Component} 注解。如果同时使用 {@code @Component} 和 {@code @Bean}，
 * 会导致创建多个 Bean 实例，引发冲突。
 *
 * @author 史偕成
 * @date 2025/01/08
 */
@Slf4j
@ConditionalOnProperty(name = "synapse.cache.lock.deadlock.distributed.enabled", havingValue = "true", matchIfMissing = true)
public class DistributedDeadlockDetector extends DeadlockDetector {

    private final RedisService redisService;
    private final DistributedDeadlockProperties properties;
    private final String nodeId;

    // 全局状态同步相关
    private final AtomicLong lastSyncTime = new AtomicLong(0);
    private final AtomicBoolean isGlobalDetectionEnabled = new AtomicBoolean(true);

    // 节点协调相关
    private final Map<String, Long> nodeHeartbeats = new ConcurrentHashMap<>();
    private final Set<String> activeNodes = ConcurrentHashMap.newKeySet();

    public DistributedDeadlockDetector(@Qualifier("lockScheduledExecutor") ScheduledExecutorService scheduler,
                                       RedisService redisService,
                                       DistributedDeadlockProperties distributedDeadlockProperties) {
        super(scheduler);
        this.redisService = redisService;
        this.properties = distributedDeadlockProperties;
        this.nodeId = generateNodeId();

        log.debug("[DistributedDeadlockDetector] 初始化分布式死锁检测器，节点ID: {}", nodeId);

        // 启动分布式检测任务
        startDistributedDetection();
    }

    /**
     * 启动分布式检测任务
     */
    private void startDistributedDetection() {
        // 启动状态同步任务
        scheduler.scheduleWithFixedDelay(this::syncLocalStateToGlobal,
                properties.getSyncInterval(), properties.getSyncInterval(), TimeUnit.MILLISECONDS);

        // 启动全局检测任务
        scheduler.scheduleWithFixedDelay(this::detectGlobalDeadlocks,
                properties.getGlobalDetectionInterval(), properties.getGlobalDetectionInterval(), TimeUnit.MILLISECONDS);

        // 启动节点心跳任务
        scheduler.scheduleWithFixedDelay(this::updateNodeHeartbeat,
                properties.getHeartbeatInterval(), properties.getHeartbeatInterval(), TimeUnit.MILLISECONDS);

        log.debug("[DistributedDeadlockDetector] 分布式检测任务已启动");
    }

    /**
     * 同步本地状态到全局Redis
     */
    public void syncLocalStateToGlobal() {
        if (!isGlobalDetectionEnabled.get()) {
            return;
        }

        try {
            long currentTime = System.currentTimeMillis();

            // 更新节点心跳
            updateNodeHeartbeat();

            // 同步本地资源分配图
            syncThreadLocksToGlobal();

            // 同步等待图
            syncThreadWaitsToGlobal();

            // 同步锁持有者信息
            syncLockHoldersToGlobal();

            lastSyncTime.set(currentTime);

            log.debug("[DistributedDeadlockDetector] 本地状态同步完成，节点: {}", nodeId);

        } catch (Exception e) {
            log.error("[DistributedDeadlockDetector] 同步本地状态失败", e);
        }
    }

    /**
     * 同步线程锁信息到全局
     */
    private void syncThreadLocksToGlobal() {
        try {
            Map<String, Object> status = getStatus();
            @SuppressWarnings("unchecked")
            Map<String, Object> localThreadLocks = (Map<String, Object>) status.get("threadLocks");
            if (localThreadLocks != null && !localThreadLocks.isEmpty()) {
                for (Map.Entry<String, Object> entry : localThreadLocks.entrySet()) {
                    String globalThreadId = nodeId + ":" + entry.getKey();
                    String lockKeys = entry.getValue().toString();

                    redisService.hset(properties.getRedisPrefix() + ":graph", globalThreadId, lockKeys);
                }
            }
        } catch (Exception e) {
            log.error("[DistributedDeadlockDetector] 同步线程锁信息失败", e);
        }
    }

    /**
     * 同步等待图到全局
     */
    private void syncThreadWaitsToGlobal() {
        try {
            Map<String, Object> status = getStatus();
            @SuppressWarnings("unchecked")
            Map<String, Object> localThreadWaits = (Map<String, Object>) status.get("threadWaits");
            if (localThreadWaits != null && !localThreadWaits.isEmpty()) {
                for (Map.Entry<String, Object> entry : localThreadWaits.entrySet()) {
                    String globalThreadId = nodeId + ":" + entry.getKey();
                    String waitKeys = entry.getValue().toString();

                    redisService.hset(properties.getRedisPrefix() + ":waits", globalThreadId, waitKeys);
                }
            }
        } catch (Exception e) {
            log.error("[DistributedDeadlockDetector] 同步等待图失败", e);
        }
    }

    /**
     * 同步锁持有者信息到全局
     */
    private void syncLockHoldersToGlobal() {
        try {
            Map<String, Object> status = getStatus();
            @SuppressWarnings("unchecked")
            Map<String, Object> localLockHolders = (Map<String, Object>) status.get("lockHolders");
            if (localLockHolders != null && !localLockHolders.isEmpty()) {
                for (Map.Entry<String, Object> entry : localLockHolders.entrySet()) {
                    String lockKey = entry.getKey();
                    String holderThreadId = entry.getValue().toString();
                    String globalHolderId = nodeId + ":" + holderThreadId;

                    redisService.hset(properties.getRedisPrefix() + ":holders", lockKey, globalHolderId);
                }
            }
        } catch (Exception e) {
            log.error("[DistributedDeadlockDetector] 同步锁持有者信息失败", e);
        }
    }

    /**
     * 检测全局死锁
     */
    public List<Set<String>> detectGlobalDeadlocks() {
        if (!isGlobalDetectionEnabled.get()) {
            return Collections.emptyList();
        }

        try {
            // 清理超时节点
            cleanupTimeoutNodes();

            // 构建全局资源分配图
            Map<String, Set<String>> globalThreadLocks = buildGlobalThreadLocks();
            Map<String, Set<String>> globalThreadWaits = buildGlobalThreadWaits();
            Map<String, String> globalLockHolders = buildGlobalLockHolders();

            // 检测全局死锁环
            List<Set<String>> globalDeadlockCycles = findGlobalDeadlockCycles(
                    globalThreadLocks, globalThreadWaits, globalLockHolders);

            if (!globalDeadlockCycles.isEmpty()) {
                log.warn("[DistributedDeadlockDetector] 检测到全局死锁，死锁环数量: {}", globalDeadlockCycles.size());
                for (int i = 0; i < globalDeadlockCycles.size(); i++) {
                    Set<String> cycle = globalDeadlockCycles.get(i);
                    log.warn("[DistributedDeadlockDetector] 全局死锁环 {}: {}", i + 1, cycle);
                }

                // 处理全局死锁
                handleGlobalDeadlocks(globalDeadlockCycles);
            }

            return globalDeadlockCycles;

        } catch (Exception e) {
            log.error("[DistributedDeadlockDetector] 全局死锁检测异常", e);
            return Collections.emptyList();
        }
    }

    /**
     * 构建全局线程锁信息
     */
    private Map<String, Set<String>> buildGlobalThreadLocks() {
        Map<String, Set<String>> globalLocks = new HashMap<>();

        try {
            Map<String, Object> globalGraph = redisService.hashGetAll(properties.getRedisPrefix() + ":graph");
            if (globalGraph != null) {
                for (Map.Entry<String, Object> entry : globalGraph.entrySet()) {
                    String threadId = entry.getKey();
                    String lockKeysStr = entry.getValue().toString();

                    Set<String> lockKeys = new HashSet<>();
                    if (lockKeysStr != null && !lockKeysStr.isEmpty()) {
                        String[] keys = lockKeysStr.split(",");
                        for (String key : keys) {
                            if (!key.trim().isEmpty()) {
                                lockKeys.add(key.trim());
                            }
                        }
                    }

                    globalLocks.put(threadId, lockKeys);
                }
            }
        } catch (Exception e) {
            log.error("[DistributedDeadlockDetector] 构建全局线程锁信息失败", e);
        }

        return globalLocks;
    }

    /**
     * 构建全局等待图
     */
    private Map<String, Set<String>> buildGlobalThreadWaits() {
        Map<String, Set<String>> globalWaits = new HashMap<>();

        try {
            Map<String, Object> globalWaitsData = redisService.hashGetAll(properties.getRedisPrefix() + ":waits");
            if (globalWaitsData != null) {
                for (Map.Entry<String, Object> entry : globalWaitsData.entrySet()) {
                    String threadId = entry.getKey();
                    String waitKeysStr = entry.getValue().toString();

                    Set<String> waitKeys = new HashSet<>();
                    if (waitKeysStr != null && !waitKeysStr.isEmpty()) {
                        String[] keys = waitKeysStr.split(",");
                        for (String key : keys) {
                            if (!key.trim().isEmpty()) {
                                waitKeys.add(key.trim());
                            }
                        }
                    }

                    globalWaits.put(threadId, waitKeys);
                }
            }
        } catch (Exception e) {
            log.error("[DistributedDeadlockDetector] 构建全局等待图失败", e);
        }

        return globalWaits;
    }

    /**
     * 构建全局锁持有者信息
     */
    private Map<String, String> buildGlobalLockHolders() {
        Map<String, String> globalHolders = new HashMap<>();

        try {
            Map<String, Object> globalHoldersData = redisService.hashGetAll(properties.getRedisPrefix() + ":holders");
            if (globalHoldersData != null) {
                for (Map.Entry<String, Object> entry : globalHoldersData.entrySet()) {
                    String lockKey = entry.getKey();
                    String holderThreadId = entry.getValue().toString();
                    globalHolders.put(lockKey, holderThreadId);
                }
            }
        } catch (Exception e) {
            log.error("[DistributedDeadlockDetector] 构建全局锁持有者信息失败", e);
        }

        return globalHolders;
    }

    /**
     * 查找全局死锁环
     */
    private List<Set<String>> findGlobalDeadlockCycles(Map<String, Set<String>> globalThreadLocks,
                                                       Map<String, Set<String>> globalThreadWaits,
                                                       Map<String, String> globalLockHolders) {
        List<Set<String>> cycles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String threadId : globalThreadLocks.keySet()) {
            if (!visited.contains(threadId)) {
                Set<String> cycle = new HashSet<>();
                if (hasGlobalCycle(threadId, globalThreadLocks, globalThreadWaits,
                        globalLockHolders, visited, recursionStack, cycle)) {
                    cycles.add(new HashSet<>(cycle));
                }
            }
        }

        return cycles;
    }

    /**
     * 使用DFS检测全局环
     */
    private boolean hasGlobalCycle(String threadId, Map<String, Set<String>> globalThreadLocks,
                                   Map<String, Set<String>> globalThreadWaits,
                                   Map<String, String> globalLockHolders,
                                   Set<String> visited, Set<String> recursionStack, Set<String> cycle) {
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

        Set<String> waits = globalThreadWaits.get(threadId);
        if (waits != null) {
            for (String lockKey : waits) {
                String holder = globalLockHolders.get(lockKey);
                if (holder != null && !holder.equals(threadId)) {
                    if (hasGlobalCycle(holder, globalThreadLocks, globalThreadWaits,
                            globalLockHolders, visited, recursionStack, cycle)) {
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
     * 处理全局死锁
     */
    private void handleGlobalDeadlocks(List<Set<String>> deadlockCycles) {
        for (Set<String> cycle : deadlockCycles) {
            // 选择优先级最低的节点进行回滚
            String victimNode = selectVictimNode(cycle);
            if (victimNode != null) {
                log.warn("[DistributedDeadlockDetector] 选择牺牲节点: {}", victimNode);

                // 如果是当前节点，直接处理
                if (victimNode.equals(nodeId)) {
                    handleLocalVictim(cycle);
                } else {
                    // 通知其他节点处理
                    notifyNodeToHandleVictim(victimNode, cycle);
                }
            }
        }
    }

    /**
     * 选择牺牲节点（优先级最低的节点）
     */
    private String selectVictimNode(Set<String> cycle) {
        // 简单的策略：选择持有锁最少的节点
        String victim = null;
        int minLocks = Integer.MAX_VALUE;

        Map<String, Integer> nodeLockCounts = new HashMap<>();

        for (String threadId : cycle) {
            String nodeId = extractNodeId(threadId);
            if (nodeId != null) {
                nodeLockCounts.put(nodeId, nodeLockCounts.getOrDefault(nodeId, 0) + 1);
            }
        }

        for (Map.Entry<String, Integer> entry : nodeLockCounts.entrySet()) {
            if (entry.getValue() < minLocks) {
                minLocks = entry.getValue();
                victim = entry.getKey();
            }
        }

        return victim;
    }

    /**
     * 处理本地牺牲线程
     */
    private void handleLocalVictim(Set<String> cycle) {
        for (String threadId : cycle) {
            if (threadId.startsWith(nodeId + ":")) {
                String localThreadId = threadId.substring(nodeId.length() + 1);
                log.warn("[DistributedDeadlockDetector] 强制释放本地线程锁: {}", localThreadId);
                forceReleaseThreadLocks(localThreadId);
            }
        }
    }

    /**
     * 通知其他节点处理牺牲线程
     */
    private void notifyNodeToHandleVictim(String victimNode, Set<String> cycle) {
        try {
            // 通过Redis发布消息通知其他节点
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "deadlock_victim");
            notification.put("victimNode", victimNode);
            notification.put("cycle", cycle);
            notification.put("timestamp", System.currentTimeMillis());

            redisService.publish(properties.getRedisPrefix() + ":notifications", notification);

            log.debug("[DistributedDeadlockDetector] 已通知节点 {} 处理死锁牺牲", victimNode);

        } catch (Exception e) {
            log.error("[DistributedDeadlockDetector] 通知节点处理牺牲失败", e);
        }
    }

    /**
     * 更新节点心跳
     */
    private void updateNodeHeartbeat() {
        try {
            long currentTime = System.currentTimeMillis();
            redisService.hset(properties.getRedisPrefix() + ":nodes", nodeId, String.valueOf(currentTime));
            nodeHeartbeats.put(nodeId, currentTime);

        } catch (Exception e) {
            log.error("[DistributedDeadlockDetector] 更新节点心跳失败", e);
        }
    }

    /**
     * 清理超时节点
     */
    private void cleanupTimeoutNodes() {
        try {
            long currentTime = System.currentTimeMillis();
            Map<String, Object> nodeStatus = redisService.hashGetAll(properties.getRedisPrefix() + ":nodes");

            if (nodeStatus != null) {
                for (Map.Entry<String, Object> entry : nodeStatus.entrySet()) {
                    String nodeId = entry.getKey();
                    long lastHeartbeat = Long.parseLong(entry.getValue().toString());

                    if (currentTime - lastHeartbeat > properties.getNodeTimeout()) {
                        log.debug("[DistributedDeadlockDetector] 节点 {} 超时，清理其状态", nodeId);
                        cleanupNodeState(nodeId);
                    }
                }
            }

        } catch (Exception e) {
            log.error("[DistributedDeadlockDetector] 清理超时节点失败", e);
        }
    }

    /**
     * 清理节点状态
     */
    private void cleanupNodeState(String nodeId) {
        try {
            // 清理该节点的所有状态
            redisService.hashDelete(properties.getRedisPrefix() + ":graph", nodeId + ":*");
            redisService.hashDelete(properties.getRedisPrefix() + ":waits", nodeId + ":*");
            redisService.hashDelete(properties.getRedisPrefix() + ":nodes", nodeId);

            log.debug("[DistributedDeadlockDetector] 已清理节点 {} 的状态", nodeId);

        } catch (Exception e) {
            log.error("[DistributedDeadlockDetector] 清理节点状态失败", e);
        }
    }

    /**
     * 从线程ID中提取节点ID
     */
    private String extractNodeId(String threadId) {
        if (threadId != null && threadId.contains(":")) {
            return threadId.split(":")[0];
        }
        return null;
    }

    /**
     * 生成节点ID
     */
    private String generateNodeId() {
        return "node_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId();
    }

    /**
     * 获取全局状态
     */
    public Map<String, Object> getGlobalStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("nodeId", nodeId);
        status.put("lastSyncTime", lastSyncTime.get());
        status.put("isGlobalDetectionEnabled", isGlobalDetectionEnabled.get());
        status.put("activeNodes", new HashSet<>(activeNodes));
        status.put("nodeHeartbeats", new HashMap<>(nodeHeartbeats));

        // 添加全局图状态
        try {
            status.put("globalThreadLocks", buildGlobalThreadLocks());
            status.put("globalThreadWaits", buildGlobalThreadWaits());
            status.put("globalLockHolders", buildGlobalLockHolders());
        } catch (Exception e) {
            log.error("[DistributedDeadlockDetector] 获取全局状态失败", e);
        }

        return status;
    }

    /**
     * 启用/禁用全局检测
     */
    public void setGlobalDetectionEnabled(boolean enabled) {
        isGlobalDetectionEnabled.set(enabled);
        log.debug("[DistributedDeadlockDetector] 全局检测已{}", enabled ? "启用" : "禁用");
    }

    /**
     * 停止分布式检测
     */
    @Override
    public void stop() {
        super.stop();
        isGlobalDetectionEnabled.set(false);

        // 清理当前节点状态
        cleanupNodeState(nodeId);

        log.debug("[DistributedDeadlockDetector] 分布式死锁检测已停止");
    }
}
