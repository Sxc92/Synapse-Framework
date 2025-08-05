package com.indigo.cache.extension;

import com.indigo.cache.infrastructure.RedisService;
import com.indigo.cache.manager.CacheKeyGenerator;
import com.indigo.core.utils.ThreadUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 分布式锁增强功能测试
 * 测试读写锁、公平锁、死锁检测、性能监控等功能
 *
 * @author 史偕成
 * @date 2025/01/08
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("分布式锁增强功能测试")
public class DistributedLockEnhancementTest {

    @Autowired
    private LockManager lockManager;

    @Autowired
    private ReadWriteLockService readWriteLockService;

    @Autowired
    private FairLockService fairLockService;

    @Autowired
    private DeadlockDetector deadlockDetector;

    @Autowired
    private LockPerformanceMonitor performanceMonitor;

    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(10);
    }

    @Test
    @DisplayName("测试读写锁功能")
    void testReadWriteLock() throws InterruptedException {
        String lockName = "test-rw-lock";
        String key = "test-key";
        AtomicInteger readCount = new AtomicInteger(0);
        AtomicInteger writeCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(6);

        // 启动多个读线程
        for (int i = 0; i < 3; i++) {
            executorService.submit(() -> {
                try {
                    String lockValue = readWriteLockService.tryReadLock(lockName, key, 10);
                    if (lockValue != null) {
                        readCount.incrementAndGet();
                        Thread.sleep(100); // 模拟读操作
                        readWriteLockService.releaseReadLock(lockName, key);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        // 启动写线程
        executorService.submit(() -> {
            try {
                String lockValue = readWriteLockService.tryWriteLock(lockName, key, 10);
                if (lockValue != null) {
                    writeCount.incrementAndGet();
                    Thread.sleep(200); // 模拟写操作
                    readWriteLockService.releaseWriteLock(lockName, key, lockValue);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        // 启动更多读线程
        for (int i = 0; i < 2; i++) {
            executorService.submit(() -> {
                try {
                    String lockValue = readWriteLockService.tryReadLock(lockName, key, 10);
                    if (lockValue != null) {
                        readCount.incrementAndGet();
                        Thread.sleep(50); // 模拟读操作
                        readWriteLockService.releaseReadLock(lockName, key);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);

        // 验证读写锁功能
        assertTrue(readCount.get() > 0, "应该有读锁成功获取");
        assertTrue(writeCount.get() > 0, "应该有写锁成功获取");
        
        System.out.println("读锁获取次数: " + readCount.get());
        System.out.println("写锁获取次数: " + writeCount.get());
    }

    @Test
    @DisplayName("测试公平锁功能")
    void testFairLock() throws InterruptedException {
        String lockName = "test-fair-lock";
        String key = "test-key";
        AtomicLong sequence = new AtomicLong(0);
        CountDownLatch latch = new CountDownLatch(5);

        // 启动多个线程竞争公平锁
        for (int i = 0; i < 5; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    String lockValue = fairLockService.tryFairLock(lockName, key, 10);
                    if (lockValue != null) {
                        long currentSeq = sequence.incrementAndGet();
                        System.out.println("线程 " + threadId + " 获取锁，序列号: " + currentSeq);
                        Thread.sleep(100); // 模拟操作
                        fairLockService.releaseFairLock(lockName, key, lockValue);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);

        // 验证公平锁功能
        assertTrue(sequence.get() > 0, "应该有锁成功获取");
        System.out.println("公平锁获取次数: " + sequence.get());
    }

    @Test
    @DisplayName("测试统一锁管理器")
    void testLockManager() throws InterruptedException {
        String lockName = "test-manager-lock";
        String key = "test-key";
        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3);

        // 测试可重入锁
        executorService.submit(() -> {
            try {
                String result = lockManager.executeWithLock(lockName, key, () -> {
                    Thread.sleep(100);
                    return "reentrant-success";
                }, LockManager.LockType.REENTRANT);
                
                if (result != null) {
                    successCount.incrementAndGet();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        // 测试读写锁
        executorService.submit(() -> {
            try {
                String result = lockManager.executeWithReadLock(lockName, key, () -> {
                    Thread.sleep(100);
                    return "read-success";
                });
                
                if (result != null) {
                    successCount.incrementAndGet();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        // 测试公平锁
        executorService.submit(() -> {
            try {
                String result = lockManager.executeWithLock(lockName, key, () -> {
                    Thread.sleep(100);
                    return "fair-success";
                }, LockManager.LockType.FAIR);
                
                if (result != null) {
                    successCount.incrementAndGet();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        latch.await(5, TimeUnit.SECONDS);

        // 验证锁管理器功能
        assertTrue(successCount.get() > 0, "应该有锁操作成功");
        System.out.println("锁管理器成功次数: " + successCount.get());
    }

    @Test
    @DisplayName("测试性能监控")
    void testPerformanceMonitoring() throws InterruptedException {
        String lockName = "test-monitor-lock";
        String key = "test-key";
        CountDownLatch latch = new CountDownLatch(5);

        // 执行多次锁操作
        for (int i = 0; i < 5; i++) {
            executorService.submit(() -> {
                try {
                    lockManager.executeWithLock(lockName, key, () -> {
                        Thread.sleep(50);
                        return "success";
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);

        // 验证性能监控
        LockPerformanceMonitor.LockStats stats = lockManager.getLockStats(lockName);
        assertNotNull(stats, "应该有性能统计信息");
        
        LockPerformanceMonitor.GlobalStats globalStats = lockManager.getGlobalStats();
        assertNotNull(globalStats, "应该有全局统计信息");

        System.out.println("锁统计信息:");
        System.out.println("  尝试次数: " + stats.attempts.sum());
        System.out.println("  成功次数: " + stats.successes.sum());
        System.out.println("  失败次数: " + stats.failures.sum());
        System.out.println("  成功率: " + stats.getSuccessRate());
        System.out.println("  平均等待时间: " + stats.getAverageWaitTime() + "ms");
        System.out.println("  平均持有时间: " + stats.getAverageHoldTime() + "ms");
    }

    @Test
    @DisplayName("测试死锁检测状态")
    void testDeadlockDetectionStatus() {
        // 获取死锁检测状态
        var status = lockManager.getDeadlockStatus();
        assertNotNull(status, "应该有死锁检测状态");
        
        System.out.println("死锁检测状态:");
        status.forEach((key, value) -> {
            System.out.println("  " + key + ": " + value);
        });
    }

    @Test
    @DisplayName("测试锁竞争场景")
    void testLockContention() throws InterruptedException {
        String lockName = "test-contention-lock";
        String key = "test-key";
        CountDownLatch latch = new CountDownLatch(10);
        AtomicInteger successCount = new AtomicInteger(0);

        // 启动多个线程竞争同一个锁
        for (int i = 0; i < 10; i++) {
            executorService.submit(() -> {
                try {
                    String result = lockManager.executeWithLock(lockName, key, () -> {
                        Thread.sleep(200); // 较长的持有时间
                        return "contention-success";
                    });
                    
                    if (result != null) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);

        // 验证锁竞争处理
        assertTrue(successCount.get() > 0, "应该有锁操作成功");
        System.out.println("锁竞争成功次数: " + successCount.get());

        // 检查性能统计
        LockPerformanceMonitor.LockStats stats = lockManager.getLockStats(lockName);
        if (stats != null) {
            System.out.println("竞争场景统计:");
            System.out.println("  最大等待线程数: " + stats.maxWaitingThreads.get());
            System.out.println("  竞争事件次数: " + stats.totalContentionEvents.sum());
        }
    }
} 