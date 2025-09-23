# 异步查询使用指南

## ⚠️ 重要说明

**异步查询功能目前处于实验阶段**，主要用于性能优化和特殊场景。建议仅在以下情况下使用：

- 大数据量查询（>10万条记录）
- 复杂多表关联查询
- 需要并行执行多个查询的场景
- 提升用户体验（避免界面卡顿）

## 📋 功能概述

异步查询基于 `CompletableFuture` 实现，提供以下功能：

- **异步分页查询**：`pageWithConditionAsync()`
- **异步列表查询**：`listWithConditionAsync()`
- **异步单个查询**：`getOneWithConditionAsync()`
- **异步性能监控查询**：`pageWithPerformanceAsync()`
- **异步聚合查询**：`pageWithAggregationAsync()`
- **异步增强查询**：`pageWithEnhancedAsync()`
- **异步统计查询**：`countWithConditionAsync()`
- **异步存在性查询**：`existsWithConditionAsync()`
- **异步快速查询**：`quickPageAsync()`, `quickListAsync()`, `quickGetOneAsync()`

## 🚀 基础使用

### 1. 异步分页查询

```java
@Service
public class ProductService {
    
    @Autowired
    private ProductRepository productRepository;
    
    public void asyncPageQuery() {
        // 异步分页查询
        CompletableFuture<PageResult<ProductVO>> future = 
            productRepository.pageWithConditionAsync(pageDTO, ProductVO.class);
        
        // 处理结果
        future.thenAccept(result -> {
            System.out.println("查询完成，共" + result.getTotal() + "条记录");
            // 处理查询结果
        }).exceptionally(throwable -> {
            System.err.println("查询失败：" + throwable.getMessage());
            return null;
        });
    }
}
```

### 2. 等待结果

```java
public PageResult<ProductVO> waitForResult() {
    CompletableFuture<PageResult<ProductVO>> future = 
        productRepository.pageWithConditionAsync(pageDTO, ProductVO.class);
    
    try {
        // 等待结果，最多等待30秒
        return future.get(30, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
        log.warn("查询超时，取消执行");
        future.cancel(true);
        throw new RuntimeException("查询超时");
    } catch (Exception e) {
        log.error("查询失败", e);
        throw new RuntimeException("查询失败: " + e.getMessage(), e);
    }
}
```

## 🔄 并行查询

### 1. 并行执行多个查询

```java
public void parallelQueries() {
    // 并行查询用户信息和订单信息
    CompletableFuture<PageResult<UserVO>> userFuture = 
        userRepository.pageWithConditionAsync(userPageDTO, UserVO.class);
    CompletableFuture<PageResult<OrderVO>> orderFuture = 
        orderRepository.pageWithConditionAsync(orderPageDTO, OrderVO.class);
    
    // 等待所有查询完成
    CompletableFuture.allOf(userFuture, orderFuture)
        .thenRun(() -> {
            try {
                PageResult<UserVO> users = userFuture.join();
                PageResult<OrderVO> orders = orderFuture.join();
                
                // 处理结果
                System.out.println("用户数量: " + users.getTotal());
                System.out.println("订单数量: " + orders.getTotal());
            } catch (Exception e) {
                log.error("并行查询处理失败", e);
            }
        })
        .exceptionally(throwable -> {
            log.error("并行查询失败", throwable);
            return null;
        });
}
```

### 2. 使用工具方法

```java
public void parallelQueriesWithTool() {
    CompletableFuture<PageResult<UserVO>> userFuture = 
        userRepository.pageWithConditionAsync(userPageDTO, UserVO.class);
    CompletableFuture<PageResult<OrderVO>> orderFuture = 
        orderRepository.pageWithConditionAsync(orderPageDTO, OrderVO.class);
    
    // 使用工具方法等待所有查询完成
    EnhancedQueryBuilder.executeAllAsync(userFuture, orderFuture)
        .thenRun(() -> {
            PageResult<UserVO> users = userFuture.join();
            PageResult<OrderVO> orders = orderFuture.join();
            // 处理结果
        });
}
```

## ⏰ 超时控制

### 1. 使用工具方法设置超时

```java
public void queryWithTimeout() {
    CompletableFuture<PageResult<ProductVO>> future = 
        productRepository.pageWithConditionAsync(pageDTO, ProductVO.class);
    
    // 设置30秒超时
    CompletableFuture<PageResult<ProductVO>> timeoutFuture = 
        EnhancedQueryBuilder.withTimeout(future, 30, TimeUnit.SECONDS);
    
    timeoutFuture.thenAccept(result -> {
        System.out.println("查询完成: " + result.getTotal() + "条记录");
    }).exceptionally(throwable -> {
        if (throwable instanceof TimeoutException) {
            log.warn("查询超时");
        } else {
            log.error("查询失败", throwable);
        }
        return null;
    });
}
```

### 2. 手动超时控制

```java
public void manualTimeoutControl() {
    CompletableFuture<PageResult<ProductVO>> future = 
        productRepository.pageWithConditionAsync(pageDTO, ProductVO.class);
    
    // 设置超时任务
    CompletableFuture.delayedExecutor(30, TimeUnit.SECONDS)
        .execute(() -> {
            if (!future.isDone()) {
                log.warn("查询超时，取消执行");
                future.cancel(true);
            }
        });
    
    future.thenAccept(result -> {
        System.out.println("查询完成: " + result.getTotal() + "条记录");
    }).exceptionally(throwable -> {
        if (future.isCancelled()) {
            log.warn("查询被取消");
        } else {
            log.error("查询失败", throwable);
        }
        return null;
    });
}
```

## 🎯 实际应用场景

### 1. 大数据量导出

```java
@Service
public class DataExportService {
    
    @Autowired
    private ProductRepository productRepository;
    
    public void exportLargeDataset() {
        // 异步查询大数据量
        CompletableFuture<PageResult<ProductVO>> future = 
            productRepository.pageWithConditionAsync(largePageDTO, ProductVO.class);
        
        future.thenAccept(result -> {
            // 异步处理导出
            CompletableFuture.runAsync(() -> {
                try {
                    exportToExcel(result.getRecords());
                    log.info("导出完成，共{}条记录", result.getTotal());
                } catch (Exception e) {
                    log.error("导出失败", e);
                }
            });
        });
    }
    
    private void exportToExcel(List<ProductVO> products) {
        // 导出逻辑
    }
}
```

### 2. 实时数据聚合

```java
@Service
public class DashboardService {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    public void updateDashboard() {
        // 并行查询多个数据源
        CompletableFuture<AggregationPageResult<ProductVO>> productStats = 
            productRepository.pageWithAggregationAsync(productAggDTO, ProductVO.class);
        CompletableFuture<AggregationPageResult<OrderVO>> orderStats = 
            orderRepository.pageWithAggregationAsync(orderAggDTO, OrderVO.class);
        
        CompletableFuture.allOf(productStats, orderStats)
            .thenRun(() -> {
                try {
                    AggregationPageResult<ProductVO> products = productStats.join();
                    AggregationPageResult<OrderVO> orders = orderStats.join();
                    
                    // 更新仪表板
                    updateDashboardData(products, orders);
                } catch (Exception e) {
                    log.error("仪表板更新失败", e);
                }
            });
    }
    
    private void updateDashboardData(AggregationPageResult<ProductVO> products, 
                                   AggregationPageResult<OrderVO> orders) {
        // 更新逻辑
    }
}
```

## ⚠️ 注意事项

### 1. 错误处理

```java
public void properErrorHandling() {
    CompletableFuture<PageResult<ProductVO>> future = 
        productRepository.pageWithConditionAsync(pageDTO, ProductVO.class);
    
    future.thenAccept(result -> {
        // 处理成功结果
        processResult(result);
    }).exceptionally(throwable -> {
        // 处理异常
        if (throwable instanceof CancellationException) {
            log.warn("查询被取消");
        } else if (throwable instanceof TimeoutException) {
            log.warn("查询超时");
        } else {
            log.error("查询失败", throwable);
        }
        return null;
    });
}
```

### 2. 资源管理

```java
@Service
public class ResourceManagedService {
    
    private final ExecutorService executorService = 
        Executors.newFixedThreadPool(10);
    
    @PreDestroy
    public void cleanup() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    public void asyncQueryWithCustomExecutor() {
        CompletableFuture<PageResult<ProductVO>> future = 
            CompletableFuture.supplyAsync(() -> {
                return productRepository.pageWithCondition(pageDTO, ProductVO.class);
            }, executorService);
        
        future.thenAccept(result -> {
            // 处理结果
        });
    }
}
```

### 3. 性能监控

```java
public void monitoredAsyncQuery() {
    long startTime = System.currentTimeMillis();
    
    CompletableFuture<PageResult<ProductVO>> future = 
        productRepository.pageWithConditionAsync(pageDTO, ProductVO.class);
    
    future.thenAccept(result -> {
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        log.info("异步查询完成，耗时: {}ms, 结果数量: {}", 
                duration, result.getTotal());
        
        // 性能监控
        if (duration > 5000) {
            log.warn("异步查询耗时过长: {}ms", duration);
        }
    });
}
```

## 🔧 最佳实践

### 1. 合理使用异步查询

- **适用场景**：大数据量查询、复杂关联查询、并行查询
- **不适用场景**：简单查询、小数据量查询、实时性要求高的查询

### 2. 错误处理策略

- 始终使用 `exceptionally()` 处理异常
- 区分不同类型的异常（超时、取消、业务异常）
- 提供降级方案

### 3. 超时控制

- 为所有异步查询设置合理的超时时间
- 使用 `withTimeout()` 工具方法
- 监控超时情况

### 4. 资源管理

- 避免创建过多的 `CompletableFuture`
- 合理使用线程池
- 及时清理资源

## 📊 性能建议

1. **数据量阈值**：建议在查询数据量超过10万条时使用异步查询
2. **超时设置**：根据数据量和复杂度设置合理的超时时间
3. **并行度**：避免同时执行过多异步查询，建议控制在10个以内
4. **监控指标**：监控异步查询的成功率、平均耗时、超时率等指标

## 🚨 已知限制

1. **API稳定性**：异步查询API可能会在后续版本中调整
2. **调试困难**：异步执行使得调试相对困难
3. **内存消耗**：异步查询会增加内存消耗
4. **线程管理**：需要合理管理线程资源

## 📝 总结

异步查询是一个强大的功能，但需要谨慎使用。建议：

1. 在性能瓶颈明确时再考虑使用
2. 充分测试异步查询的稳定性和性能
3. 建立完善的监控和错误处理机制
4. 定期评估异步查询的必要性

记住：**异步查询是性能优化的工具，不是解决所有问题的银弹**。
