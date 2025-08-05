package com.indigo.cache.annotation;

import com.indigo.cache.core.TwoLevelCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 缓存注解测试类
 * 测试 @Cacheable, @CachePut, @CacheEvict, @Caching 注解的功能
 *
 * @author 史偕成
 * @date 2025/01/08
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("缓存注解测试")
public class CacheAnnotationTest {

    @Autowired
    private TwoLevelCacheService cacheService;

    @Autowired
    private TestService testService;

    @Test
    @DisplayName("测试 @Cacheable 注解")
    public void testCacheable() {
        // 第一次调用，应该执行方法并缓存结果
        String result1 = testService.getUserById(1L);
        assertEquals("User-1", result1);

        // 第二次调用，应该从缓存获取，不执行方法
        String result2 = testService.getUserById(1L);
        assertEquals("User-1", result2);

        // 验证缓存中确实有数据
        assertTrue(cacheService.get("TestService", "user:1", 
            TwoLevelCacheService.CacheStrategy.LOCAL_AND_REDIS).isPresent());
    }

    @Test
    @DisplayName("测试 @CachePut 注解")
    public void testCachePut() {
        // 先缓存一个用户
        testService.getUserById(1L);

        // 使用 @CachePut 更新用户
        String updatedUser = testService.updateUser(1L, "UpdatedUser-1");
        assertEquals("UpdatedUser-1", updatedUser);

        // 验证缓存已被更新
        String cachedUser = testService.getUserById(1L);
        assertEquals("UpdatedUser-1", cachedUser);
    }

    @Test
    @DisplayName("测试 @CacheEvict 注解")
    public void testCacheEvict() {
        // 先缓存一个用户
        testService.getUserById(1L);

        // 验证缓存存在
        assertTrue(cacheService.get("TestService", "user:1", 
            TwoLevelCacheService.CacheStrategy.LOCAL_AND_REDIS).isPresent());

        // 使用 @CacheEvict 删除缓存
        testService.deleteUser(1L);

        // 验证缓存已被删除
        assertFalse(cacheService.get("TestService", "user:1", 
            TwoLevelCacheService.CacheStrategy.LOCAL_AND_REDIS).isPresent());
    }

    @Test
    @DisplayName("测试 @Caching 组合注解")
    public void testCaching() {
        // 先缓存一个用户
        testService.getUserById(1L);

        // 使用 @Caching 组合操作：更新用户并清除相关缓存
        String result = testService.updateUserWithCaching(1L, "NewUser-1");
        assertEquals("NewUser-1", result);

        // 验证用户缓存已更新
        String cachedUser = testService.getUserById(1L);
        assertEquals("NewUser-1", cachedUser);

        // 验证用户列表缓存已被清除（通过 allEntries = true）
        assertFalse(cacheService.get("TestService", "userList", 
            TwoLevelCacheService.CacheStrategy.LOCAL_AND_REDIS).isPresent());
    }

    @Test
    @DisplayName("测试缓存条件表达式")
    public void testCacheCondition() {
        // 测试条件为 true 的情况
        String result1 = testService.getUserWithCondition(1L, "ValidUser");
        assertEquals("ValidUser", result1);

        // 验证缓存存在
        assertTrue(cacheService.get("TestService", "user:1", 
            TwoLevelCacheService.CacheStrategy.LOCAL_AND_REDIS).isPresent());

        // 测试条件为 false 的情况
        String result2 = testService.getUserWithCondition(2L, null);
        assertEquals(null, result2);

        // 验证缓存不存在（因为条件为 false）
        assertFalse(cacheService.get("TestService", "user:2", 
            TwoLevelCacheService.CacheStrategy.LOCAL_AND_REDIS).isPresent());
    }

    /**
     * 测试服务类
     */
    public static class TestService {

        @Cacheable(key = "user:#id", expireSeconds = 3600)
        public String getUserById(Long id) {
            // 模拟数据库查询
            return "User-" + id;
        }

        @CachePut(key = "user:#id", expireSeconds = 3600)
        public String updateUser(Long id, String name) {
            // 模拟数据库更新
            return name;
        }

        @CacheEvict(key = "user:#id")
        public void deleteUser(Long id) {
            // 模拟数据库删除
        }

        @Caching(
            put = @CachePut(key = "user:#id", expireSeconds = 3600),
            evict = @CacheEvict(key = "userList", allEntries = true)
        )
        public String updateUserWithCaching(Long id, String name) {
            // 模拟数据库更新
            return name;
        }

        @Cacheable(key = "user:#id", condition = "#name != null")
        public String getUserWithCondition(Long id, String name) {
            // 只有当 name 不为 null 时才缓存
            return name;
        }
    }
} 