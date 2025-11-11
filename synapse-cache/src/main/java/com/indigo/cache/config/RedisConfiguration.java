package com.indigo.cache.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置类，基于Spring Boot自动配置进行扩展
 * 支持synapse.cache.redis前缀的配置属性
 * 
 * 重要说明：
 * 1. 使用 @AutoConfigureBefore 确保在 Spring Boot 自动配置之前运行
 * 2. 使用 @Primary 确保此 RedisTemplate 是主要的 Bean
 * 3. 所有 key 都使用 StringRedisSerializer，避免 Java 序列化导致的 key 不可读问题
 * 4. 此配置会影响 Sa-Token 等框架使用的 RedisTemplate
 * 
 * @author 史偕成
 * @date 2025/05/11 20:35
 */
@Slf4j
@Configuration
@AutoConfigureBefore(RedisAutoConfiguration.class)
@Order(1)
public class RedisConfiguration {
    
    /**
     * 配置RedisTemplate，使用JSON序列化
     * 支持synapse.cache.redis配置
     * 
     * 重要：确保所有key都使用StringRedisSerializer，避免Java序列化导致的key不可读问题
     */
    @Bean("redisTemplate")
    @Primary
    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("创建自定义RedisTemplate Bean，RedisConnectionFactory: {}", 
                 redisConnectionFactory != null ? redisConnectionFactory.getClass().getSimpleName() : "null");
        
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        
        // 配置JSON序列化器
        GenericJackson2JsonRedisSerializer jackson2JsonRedisSerializer = new GenericJackson2JsonRedisSerializer(objectMapper());
        
        // 使用StringRedisSerializer来序列化和反序列化redis的key值
        // 这是关键配置，确保所有key都是可读的字符串格式
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);
        
        // 使用GenericJackson2JsonRedisSerializer来序列化和反序列化redis的value值
        template.setValueSerializer(jackson2JsonRedisSerializer);
        
        // Hash的key也采用StringRedisSerializer的序列化方式（重要！）
        // 这是关键配置，确保Hash key也是可读的字符串格式，避免出现 \xac\xed\x00\x05 这样的Java序列化格式
        template.setHashKeySerializer(stringRedisSerializer);
        // Hash的value采用GenericJackson2JsonRedisSerializer的序列化方式
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        
        template.afterPropertiesSet();
        
        // 验证序列化器配置
        if (template.getKeySerializer() == null || !(template.getKeySerializer() instanceof StringRedisSerializer)) {
            log.warn("警告：RedisTemplate的Key序列化器未正确配置为StringRedisSerializer！");
        }
        if (template.getHashKeySerializer() == null || !(template.getHashKeySerializer() instanceof StringRedisSerializer)) {
            log.warn("警告：RedisTemplate的HashKey序列化器未正确配置为StringRedisSerializer！");
        }
        
        log.info("自定义RedisTemplate Bean 创建成功");
        log.info("  - Key序列化器: {}", template.getKeySerializer().getClass().getSimpleName());
        log.info("  - Value序列化器: {}", template.getValueSerializer().getClass().getSimpleName());
        log.info("  - HashKey序列化器: {}", template.getHashKeySerializer().getClass().getSimpleName());
        log.info("  - HashValue序列化器: {}", template.getHashValueSerializer().getClass().getSimpleName());
        
        // 重要提示：此 RedisTemplate 会被 Sa-Token 等框架使用
        // 如果发现 key 仍然被序列化，请检查：
        // 1. 是否有其他地方创建了 RedisTemplate Bean
        // 2. Sa-Token 是否使用了自定义的 RedisTemplate
        // 3. Spring Boot 自动配置是否仍然生效
        log.info("提示：此 RedisTemplate 配置为 @Primary，应该被所有框架（包括 Sa-Token）使用");
        
        return template;
    }
    
    /**
     * 配置StringRedisTemplate
     * 支持synapse.cache.redis配置
     */
    @Bean("stringRedisTemplate")
    @ConditionalOnMissingBean(name = "stringRedisTemplate")
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("创建自定义StringRedisTemplate Bean");
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(redisConnectionFactory);
        log.info("自定义StringRedisTemplate Bean 创建成功");
        return template;
    }
    
    /**
     * ObjectMapper配置，用于Redis序列化
     */
    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 注释掉默认类型信息配置，避免Redis存储二进制格式
        // mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        return mapper;
    }
}
