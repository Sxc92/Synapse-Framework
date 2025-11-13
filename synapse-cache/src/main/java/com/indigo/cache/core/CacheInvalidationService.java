package com.indigo.cache.core;

import com.indigo.cache.infrastructure.RedisService;
import com.indigo.core.utils.JsonUtils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存失效通知服务
 * 用于解决分布式环境下本地缓存一致性问题
 * 
 * 工作原理：
 * 1. 当某个节点更新缓存时，通过 Redis Pub/Sub 发布失效事件
 * 2. 其他节点订阅该事件，收到后清除本地缓存
 * 3. 下次读取时从 Redis 获取最新数据
 * 
 * @author 史偕成
 * @date 2025/01/13
 */
@Slf4j
public class CacheInvalidationService {

    /**
     * Redis Pub/Sub 频道名称
     */
    private static final String CACHE_INVALIDATION_CHANNEL = "synapse:cache:invalidation";

    private final RedisService redisService;
    private final RedisMessageListenerContainer messageListenerContainer;
    private final Set<CacheInvalidationListener> listeners = ConcurrentHashMap.newKeySet();

    public CacheInvalidationService(RedisService redisService,
                                   RedisConnectionFactory connectionFactory) {
        this.redisService = redisService;
        this.messageListenerContainer = createMessageListenerContainer(connectionFactory);
        initSubscriber();
    }

    /**
     * 创建消息监听容器
     */
    private RedisMessageListenerContainer createMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setRecoveryInterval(5000L); // 恢复间隔 5 秒
        container.afterPropertiesSet();
        container.start();
        return container;
    }

    /**
     * 初始化订阅者
     */
    private void initSubscriber() {
        messageListenerContainer.addMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message message, byte[] pattern) {
                try {
                    String channel = new String(message.getChannel());
                    
                    if (CACHE_INVALIDATION_CHANNEL.equals(channel)) {
                        // 使用 RedisTemplate 的序列化器反序列化消息体
                        // message.getBody() 返回的是字节数组，需要反序列化为对象
                        byte[] bodyBytes = message.getBody();
                        CacheInvalidationEvent event = redisService.deserializeMessage(bodyBytes, CacheInvalidationEvent.class);
                        
                        if (event != null) {
                            handleInvalidationEvent(event);
                        } else {
                            log.warn("反序列化缓存失效事件失败，消息体为空或格式错误");
                        }
                    }
                } catch (Exception e) {
                    log.error("处理缓存失效消息失败", e);
                }
            }
        }, new ChannelTopic(CACHE_INVALIDATION_CHANNEL));
        
        log.debug("缓存失效通知服务已启动，订阅频道: {}", CACHE_INVALIDATION_CHANNEL);
    }

    /**
     * 注册缓存失效监听器
     * 
     * @param listener 监听器
     */
    public void registerListener(CacheInvalidationListener listener) {
        listeners.add(listener);
        log.debug("注册缓存失效监听器: {}", listener.getClass().getSimpleName());
    }

    /**
     * 取消注册缓存失效监听器
     * 
     * @param listener 监听器
     */
    public void unregisterListener(CacheInvalidationListener listener) {
        listeners.remove(listener);
        log.debug("取消注册缓存失效监听器: {}", listener.getClass().getSimpleName());
    }

    /**
     * 发布缓存失效事件
     * 
     * @param cacheType 缓存类型（如：userSession, userPermissions, userRoles）
     * @param cacheKey  缓存键（如：token）
     */
    public void publishInvalidation(String cacheType, String cacheKey) {
        try {
            CacheInvalidationEvent event = new CacheInvalidationEvent(cacheType, cacheKey);
            // 直接传递事件对象，让 RedisTemplate 的序列化器处理
            // 不要先调用 toJson()，避免双重序列化
            redisService.publish(CACHE_INVALIDATION_CHANNEL, event);
            log.debug("发布缓存失效事件: cacheType={}, cacheKey={}", cacheType, cacheKey);
        } catch (Exception e) {
            log.error("发布缓存失效事件失败: cacheType={}, cacheKey={}", cacheType, cacheKey, e);
        }
    }

    /**
     * 批量发布缓存失效事件
     * 
     * @param cacheType 缓存类型
     * @param cacheKeys 缓存键列表
     */
    public void publishInvalidationBatch(String cacheType, Set<String> cacheKeys) {
        if (cacheKeys == null || cacheKeys.isEmpty()) {
            return;
        }
        
        for (String cacheKey : cacheKeys) {
            publishInvalidation(cacheType, cacheKey);
        }
        
        log.debug("批量发布缓存失效事件: cacheType={}, count={}", cacheType, cacheKeys.size());
    }

    /**
     * 处理失效事件
     */
    private void handleInvalidationEvent(CacheInvalidationEvent event) {
        try {
            if (event == null) {
                log.warn("缓存失效事件为空，跳过处理");
                return;
            }
            
            log.debug("收到缓存失效事件: cacheType={}, cacheKey={}", event.getCacheType(), event.getCacheKey());
            
            // 通知所有监听器
            for (CacheInvalidationListener listener : listeners) {
                try {
                    listener.onCacheInvalidation(event.getCacheType(), event.getCacheKey());
                } catch (Exception e) {
                    log.error("监听器处理缓存失效事件失败: listener={}, cacheType={}, cacheKey={}", 
                            listener.getClass().getSimpleName(), event.getCacheType(), event.getCacheKey(), e);
                }
            }
        } catch (Exception e) {
            log.error("处理缓存失效事件失败: event={}", event, e);
        }
    }

    /**
     * 缓存失效事件
     */
    @Setter
    @Getter
    public static class CacheInvalidationEvent {
        private String cacheType;
        private String cacheKey;
        private long timestamp;

        public CacheInvalidationEvent() {
            this.timestamp = System.currentTimeMillis();
        }

        public CacheInvalidationEvent(String cacheType, String cacheKey) {
            this.cacheType = cacheType;
            this.cacheKey = cacheKey;
            this.timestamp = System.currentTimeMillis();
        }

        public String toJson() {
            return JsonUtils.toJsonString(this);
        }

        public static CacheInvalidationEvent fromJson(String json) {
            return JsonUtils.fromJson(json, CacheInvalidationEvent.class);
        }
    }

    /**
     * 缓存失效监听器接口
     */
    public interface CacheInvalidationListener {
        /**
         * 处理缓存失效事件
         * 
         * @param cacheType 缓存类型
         * @param cacheKey  缓存键
         */
        void onCacheInvalidation(String cacheType, String cacheKey);
    }
}

