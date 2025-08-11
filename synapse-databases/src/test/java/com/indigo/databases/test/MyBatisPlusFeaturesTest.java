package com.indigo.databases.test;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.indigo.databases.entity.AuditEntity;
import com.indigo.databases.entity.BaseEntity;
import com.indigo.databases.entity.CreatedEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 功能测试
 * 验证版本控制、软删除、自动填充等功能
 *
 * @author 史偕成
 * @date 2025/12/19
 */
@Slf4j
@SpringBootTest
public class MyBatisPlusFeaturesTest {

    /**
     * 测试实体类继承关系
     */
    @Test
    public void testEntityInheritance() {
        // 测试基础实体
        BaseEntity<Long> baseEntity = BaseEntity.<Long>builder()
                .id(1L)
                .build();
        log.info("BaseEntity: {}", baseEntity);

        // 测试创建实体
        CreatedEntity<Long> createdEntity = CreatedEntity.<Long>builder()
                .id(2L)
                .createTime(LocalDateTime.now())
                .createUser(2L)
                .build();
        log.info("CreatedEntity: {}", createdEntity);

        // 测试审计实体
        AuditEntity<Long> auditEntity = AuditEntity.<Long>builder()
                .id(3L)
                .createTime(LocalDateTime.now())
                .createUser(3L)
                .modifyTime(LocalDateTime.now())
                .modifyUser(3L)
                .revision(1)
                .deleted(false)
                .build();
        log.info("AuditEntity: {}", auditEntity);
    }

    /**
     * 测试 MyBatis-Plus 注解功能
     */
    @Test
    public void testMyBatisPlusAnnotations() {
        // 测试 @TableId
        BaseEntity<Long> entity = BaseEntity.<Long>builder()
                .id(1L)
                .build();
        log.info("Entity with @TableId: {}", entity.getId());

        // 测试 @Version
        AuditEntity<Long> auditEntity = AuditEntity.<Long>builder()
                .revision(1)
                .build();
        log.info("Entity with @Version: {}", auditEntity.getRevision());

        // 测试 @TableLogic
        AuditEntity<Long> deletedEntity = AuditEntity.<Long>builder()
                .deleted(true)
                .build();
        log.info("Entity with @TableLogic: {}", deletedEntity.getDeleted());

        // 测试 @TableField
        CreatedEntity<Long> createdEntity = CreatedEntity.<Long>builder()
                .createTime(LocalDateTime.now())
                .createUser(1L)
                .build();
        log.info("Entity with @TableField: createTime={}, createUser={}", 
                createdEntity.getCreateTime(), createdEntity.getCreateUser());
    }

    /**
     * 测试查询条件构建
     */
    @Test
    public void testQueryWrapper() {
        // 测试基础查询
        QueryWrapper<AuditEntity<Long>> wrapper = new QueryWrapper<>();
        wrapper.eq("id", 1L)
               .eq("deleted", false)
               .orderByDesc("create_time");
        
        log.info("QueryWrapper SQL: {}", wrapper.getSqlSegment());
    }
} 