# 分布式死锁检测功能详解

## 概述

分布式死锁检测是 Synapse Cache 模块的高级功能，用于在分布式环境中检测和处理跨节点的死锁情况。该功能基于 Redis 构建全局资源分配图，实现混合检测策略和智能协调处理。

## 功能特性

### 🔍 **混合检测策略**
- **本地检测**: 快速检测单节点内的死锁
- **全局检测**: 跨节点检测分布式死锁
- **智能协调**: 本地死锁立即处理，全局死锁协调处理

### 🔄 **状态同步机制**
- **定期同步**: 将本地锁状态同步到 Redis 全局图
- **实时更新**: 锁获取和释放时实时更新状态
- **增量同步**: 只同步变化的状态，减少网络开销

### 🌐 **分布式算法**
- **DFS环检测**: 使用深度优先搜索检测死锁环
- **节点协调**: 通过心跳机制管理分布式节点
- **容错处理**: 单点故障不影响整体检测

### ⚡ **智能处理**
- **牺牲选择**: 智能选择牺牲线程打破死锁
- **自动恢复**: 自动释放锁并通知相关节点
- **状态清理**: 定期清理超时节点状态

## 架构设计

### 核心组件

```
┌─────────────────────────────────────────────────────────────┐
│                    DistributedDeadlockDetector              │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │   Local State   │  │  Global State   │  │ Coordination │ │
│  │   Management    │  │   Management    │  │   Manager    │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │   Sync Engine   │  │ Detection Algo  │  │ Recovery     │ │
│  │                 │  │                 │  │ Engine       │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                        Redis Storage                        │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────┐ │
│  │   Graph     │ │   Waits     │ │  Holders    │ │ Nodes   │ │
│  │   Data      │ │   Data      │ │   Data      │ │ Status  │ │
│  └─────────────┘ └─────────────┘ └─────────────┘ └─────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 数据流

1. **状态收集**: 收集本地线程锁信息、等待关系和锁持有者
2. **状态同步**: 将本地状态同步到 Redis 全局存储
3. **环检测**: 在全局图中使用 DFS 算法检测死锁环
4. **协调处理**: 选择牺牲线程并通知相关节点
5. **状态清理**: 清理超时节点和无效状态

## 配置说明

### 基础配置

```yaml
synapse:
  cache:
    lock:
      deadlock:
        distributed:
          # 是否启用分布式死锁检测
          enabled: true
          # 状态同步间隔(毫秒)
          sync-interval: 5000
          # 全局检测间隔(毫秒)
          global-detection-interval: 10000
          # 节点超时时间(毫秒)
          node-timeout: 30000
          # 最大节点数量
          max-nodes: 10
          # Redis键前缀
          redis-prefix: "deadlock:global"
          # 是否启用调试日志
          debug: false
          # 心跳间隔(毫秒)
          heartbeat-interval: 1000
          # 清理间隔(毫秒)
          cleanup-interval: 30000
```

### 配置参数详解

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `enabled` | `true` | 是否启用分布式死锁检测 |
| `sync-interval` | `5000` | 本地状态同步到Redis的间隔 |
| `global-detection-interval` | `10000` | 全局死锁检测的执行间隔 |
| `node-timeout` | `30000` | 节点超时时间，超过此时间认为节点失效 |
| `max-nodes` | `10` | 最大支持的节点数量 |
| `redis-prefix` | `"deadlock:global"` | Redis中存储全局状态的键前缀 |
| `debug` | `false` | 是否启用调试日志 |
| `heartbeat-interval` | `1000` | 节点心跳更新间隔 |
| `cleanup-interval` | `30000` | 超时节点清理间隔 |

## 使用示例

### 基础使用

```java
@Service
public class DeadlockDetectionService {
    
    @Autowired
    private LockManager lockManager;
    
    /**
     * 获取分布式死锁检测状态
     */
    public Map<String, Object> getDeadlockStatus() {
        return lockManager.getDistributedDeadlockStatus();
    }
    
    /**
     * 手动触发全局死锁检测
     */
    public List<Set<String>> detectDeadlocks() {
        return lockManager.detectGlobalDeadlocks();
    }
    
    /**
     * 同步本地状态到全局
     */
    public void syncLocalState() {
        lockManager.syncLocalStateToGlobal();
    }
    
    /**
     * 启用/禁用全局检测
     */
    public void setGlobalDetection(boolean enabled) {
        lockManager.setGlobalDetectionEnabled(enabled);
    }
}
```

### 监控和告警

```java
@Component
public class DeadlockMonitor {
    
    @Autowired
    private LockManager lockManager;
    
    /**
     * 定期检查死锁状态
     */
    @Scheduled(fixedRate = 30000) // 每30秒检查一次
    public void monitorDeadlocks() {
        if (!lockManager.isDistributedDeadlockEnabled()) {
            return;
        }
        
        // 获取全局状态
        Map<String, Object> status = lockManager.getDistributedDeadlockStatus();
        
        // 检查是否有死锁
        List<Set<String>> cycles = lockManager.detectGlobalDeadlocks();
        if (!cycles.isEmpty()) {
            log.warn("检测到死锁环: {}", cycles);
            // 发送告警通知
            sendDeadlockAlert(cycles);
        }
        
        // 检查节点状态
        @SuppressWarnings("unchecked")
        Map<String, Object> nodes = (Map<String, Object>) status.get("globalNodes");
        if (nodes != null) {
            checkNodeHealth(nodes);
        }
    }
    
    private void sendDeadlockAlert(List<Set<String>> cycles) {
        // 实现告警逻辑
        log.error("检测到 {} 个死锁环，需要人工干预", cycles.size());
    }
    
    private void checkNodeHealth(Map<String, Object> nodes) {
        long currentTime = System.currentTimeMillis();
        for (Map.Entry<String, Object> entry : nodes.entrySet()) {
            String nodeId = entry.getKey();
            long lastHeartbeat = Long.parseLong(entry.getValue().toString());
            
            if (currentTime - lastHeartbeat > 60000) { // 超过1分钟未心跳
                log.warn("节点 {} 心跳异常，最后心跳时间: {}", nodeId, new Date(lastHeartbeat));
            }
        }
    }
}
```

### 自定义死锁处理

```java
@Component
public class CustomDeadlockHandler {
    
    @Autowired
    private LockManager lockManager;
    
    /**
     * 自定义死锁处理逻辑
     */
    public void handleDeadlock(Set<String> deadlockCycle) {
        log.info("处理死锁环: {}", deadlockCycle);
        
        // 1. 记录死锁信息
        recordDeadlockInfo(deadlockCycle);
        
        // 2. 选择牺牲线程（自定义策略）
        String victimThread = selectVictimThread(deadlockCycle);
        
        // 3. 通知相关服务
        notifyAffectedServices(deadlockCycle);
        
        // 4. 执行恢复操作
        executeRecovery(victimThread);
    }
    
    private String selectVictimThread(Set<String> deadlockCycle) {
        // 自定义牺牲线程选择策略
        // 例如：选择持有锁最少的线程
        return deadlockCycle.iterator().next();
    }
    
    private void recordDeadlockInfo(Set<String> deadlockCycle) {
        // 记录死锁信息到数据库或日志
        log.error("死锁详情: 涉及线程 {}, 时间: {}", deadlockCycle, new Date());
    }
    
    private void notifyAffectedServices(Set<String> deadlockCycle) {
        // 通知相关服务处理死锁
        // 可以通过消息队列、HTTP调用等方式
    }
    
    private void executeRecovery(String victimThread) {
        // 执行恢复操作
        // 例如：回滚事务、释放资源等
    }
}
```

## 算法原理

### DFS环检测算法

```java
/**
 * 使用深度优先搜索检测死锁环
 */
private boolean hasGlobalCycle(String threadId, Set<String> visited, 
                              Set<String> recursionStack, Set<String> cycle,
                              Map<String, Set<String>> globalThreadWaits, 
                              Map<String, String> globalLockHolders) {
    
    // 如果当前线程在递归栈中，说明存在环
    if (recursionStack.contains(threadId)) {
        cycle.add(threadId);
        return true;
    }
    
    // 如果已经访问过，直接返回
    if (visited.contains(threadId)) {
        return false;
    }
    
    // 标记为已访问和递归栈中
    visited.add(threadId);
    recursionStack.add(threadId);
    cycle.add(threadId);
    
    // 检查当前线程等待的锁
    Set<String> waits = globalThreadWaits.get(threadId);
    if (waits != null) {
        for (String lockKey : waits) {
            String holder = globalLockHolders.get(lockKey);
            if (holder != null && !holder.equals(threadId)) {
                // 递归检查锁持有者
                if (hasGlobalCycle(holder, visited, recursionStack, cycle, 
                                 globalThreadWaits, globalLockHolders)) {
                    return true;
                }
            }
        }
    }
    
    // 从递归栈中移除
    recursionStack.remove(threadId);
    cycle.remove(threadId);
    return false;
}
```

### 牺牲线程选择策略

```java
/**
 * 牺牲线程选择策略
 */
private String selectGlobalVictimThread(Set<String> cycle) {
    // 策略1: 选择节点ID最小的线程
    return cycle.stream()
               .min(Comparator.naturalOrder())
               .orElse(null);
    
    // 策略2: 选择持有锁最少的线程
    // return cycle.stream()
    //            .min(Comparator.comparing(this::getLockCount))
    //            .orElse(null);
    
    // 策略3: 选择优先级最低的线程
    // return cycle.stream()
    //            .min(Comparator.comparing(this::getThreadPriority))
    //            .orElse(null);
}
```

## 性能优化

### 1. 同步优化

- **增量同步**: 只同步变化的状态，减少网络开销
- **批量操作**: 使用Redis管道批量更新状态
- **压缩传输**: 对状态数据进行压缩传输

### 2. 检测优化

- **分层检测**: 先进行本地检测，再进行全局检测
- **异步检测**: 使用异步任务进行死锁检测
- **缓存结果**: 缓存检测结果，避免重复计算

### 3. 存储优化

- **数据过期**: 设置合理的数据过期时间
- **索引优化**: 为Redis键建立合适的索引
- **分片存储**: 对大量数据进行分片存储

## 故障排除

### 常见问题

1. **同步失败**
   - 检查Redis连接是否正常
   - 验证网络延迟和超时设置
   - 检查Redis内存使用情况

2. **检测不准确**
   - 调整同步间隔参数
   - 检查节点时钟同步
   - 验证检测算法实现

3. **性能问题**
   - 优化同步频率
   - 减少检测范围
   - 使用异步处理

### 调试技巧

```yaml
# 启用调试日志
logging:
  level:
    com.indigo.cache.extension.lock.DistributedDeadlockDetector: DEBUG

# 启用详细状态输出
synapse:
  cache:
    lock:
      deadlock:
        distributed:
          debug: true
```

## 最佳实践

### 1. 配置建议

- **同步间隔**: 根据网络延迟调整，一般5-10秒
- **检测间隔**: 根据业务需求调整，一般10-30秒
- **节点超时**: 设置为心跳间隔的3-5倍

### 2. 监控建议

- **定期检查**: 设置定期检查任务监控死锁状态
- **告警机制**: 建立死锁告警机制
- **性能监控**: 监控检测性能影响

### 3. 运维建议

- **节点管理**: 及时清理失效节点
- **状态清理**: 定期清理过期状态数据
- **日志分析**: 定期分析死锁日志

## 版本历史

- **v1.0.0**: 初始版本，基础分布式死锁检测
- **v1.1.0**: 优化检测算法，提高性能
- **v1.2.0**: 增加自定义处理策略
- **v1.3.0**: 完善监控和告警功能
