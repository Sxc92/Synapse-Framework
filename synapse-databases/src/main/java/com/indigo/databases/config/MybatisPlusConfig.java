package com.indigo.databases.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.indigo.core.context.UserContext;
import com.indigo.databases.dynamic.DynamicDataSourceContextHolder;
import com.indigo.databases.enums.DatabaseType;
import com.indigo.databases.interceptor.AutoDataSourceInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.SqlSessionFactory;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus配置
 *
 * @author 史偕成
 * @date 2025/03/21
 */
@Slf4j
@Configuration
@EnableTransactionManagement
public class MybatisPlusConfig {

    private final AutoDataSourceInterceptor autoDataSourceInterceptor;
    private final SynapseDataSourceProperties synapseDataSourceProperties;

    public MybatisPlusConfig(AutoDataSourceInterceptor autoDataSourceInterceptor,
                             SynapseDataSourceProperties synapseDataSourceProperties) {
        this.autoDataSourceInterceptor = autoDataSourceInterceptor;
        this.synapseDataSourceProperties = synapseDataSourceProperties;
        log.info("MybatisPlusConfig 已加载");
    }

    /**
     * 配置MyBatis-Plus插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 分页插件 - 使用动态数据库类型
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor();
        paginationInterceptor.setDbType(getCurrentDbType());
        interceptor.addInnerInterceptor(paginationInterceptor);
        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        // 防止全表更新与删除插件
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        // 自动数据源切换拦截器
        interceptor.addInnerInterceptor(autoDataSourceInterceptor);

        return interceptor;
    }

    /**
     * 获取当前数据源对应的数据库类型
     */
    private DbType getCurrentDbType() {
        String currentDataSource = DynamicDataSourceContextHolder.getDataSource();
        if (currentDataSource == null) {
            currentDataSource = synapseDataSourceProperties.getPrimary();
        }

        DatabaseType databaseType = synapseDataSourceProperties.getDatasources()
                .get(currentDataSource)
                .getType();

        return switch (databaseType) {
            case MYSQL -> DbType.MYSQL;
            case POSTGRESQL -> DbType.POSTGRE_SQL;
            case ORACLE -> DbType.ORACLE;
            case SQLSERVER -> DbType.SQL_SERVER;
            case H2 -> DbType.H2;
        };
    }

    /**
     * 配置MyBatis-Plus全局配置
     */
    @Bean
    public GlobalConfig globalConfig() {
        GlobalConfig globalConfig = new GlobalConfig();
        // 配置数据库相关
        GlobalConfig.DbConfig dbConfig = new GlobalConfig.DbConfig();
        // 配置逻辑删除
        String DELETED = "deleted";
        dbConfig.setLogicDeleteField(DELETED);
//        dbConfig.setLogicDeleteValue(CommonConstants.ENABLED);
//        dbConfig.setLogicNotDeleteValue("0");

        // 配置主键策略
        dbConfig.setIdType(IdType.ASSIGN_ID);

        // 配置字段策略
        dbConfig.setUpdateStrategy(FieldStrategy.ALWAYS);
        dbConfig.setInsertStrategy(FieldStrategy.ALWAYS);
        globalConfig.setDbConfig(dbConfig);
        globalConfig.setBanner(synapseDataSourceProperties.getMybatisPlus().getGlobalConfig().isBanner());
        // 设置 MetaObjectHandler
        globalConfig.setMetaObjectHandler(new MyMetaObjectHandler());
        return globalConfig;
    }

    /**
     * 配置SqlSessionFactory
     */
    @Bean
    @Primary
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        
        // 设置MyBatis-Plus插件
        factoryBean.setPlugins(mybatisPlusInterceptor());
        
        // 设置全局配置
        factoryBean.setGlobalConfig(globalConfig());
        
        // 设置MyBatis-Plus配置
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setLogImpl(org.apache.ibatis.logging.stdout.StdOutImpl.class);
        factoryBean.setConfiguration(configuration);
        
        return factoryBean.getObject();
    }

    /**
     * 手动注册 MetaObjectHandler Bean
     */
    @Bean
    @ConditionalOnMissingBean(MetaObjectHandler.class)
    public MetaObjectHandler metaObjectHandler() {
        log.info("手动注册 MetaObjectHandler Bean");
        return new MyMetaObjectHandler();
    }

}

/**
 * MyBatis-Plus 元数据处理器
 * 用于自动填充创建时间、更新时间、创建人、更新人等字段
 *
 * @author 史偕成
 * @date 2025/03/21
 */
@Slf4j
@Component
@ConditionalOnMissingBean(MyMetaObjectHandler.class)
class MyMetaObjectHandler implements MetaObjectHandler {

    public MyMetaObjectHandler() {
        log.info("MyMetaObjectHandler Bean 已创建");
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        UserContext currentUser = UserContext.getCurrentUser();
        log.debug("执行插入填充: 当前时间={}", now);
        log.debug("执行插入填充: 当前user={}", currentUser);

        // 获取实体类的字段信息
        String className = metaObject.getOriginalObject().getClass().getSimpleName();
        log.debug("填充实体类: {}", className);

        // 填充创建时间
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        log.debug("填充createTime完成");

        // 填充修改时间（插入时也设置）
        this.strictInsertFill(metaObject, "modifyTime", LocalDateTime.class, now);
        log.debug("填充modifyTime完成");

        // 填充乐观锁版本号
        this.strictInsertFill(metaObject, "revision", Integer.class, 1);
        log.debug("填充revision完成");

        // 填充逻辑删除标记
        this.strictInsertFill(metaObject, "deleted", Boolean.class, false);
        log.debug("填充deleted完成");

        // 填充创建人
        if (currentUser != null) {
            // 优先使用userId，如果为null则使用username
            String userId = currentUser.getUserId();
            if (userId == null || userId.trim().isEmpty()) {
                userId = currentUser.getUsername();
            }

            String tenantId = currentUser.getTenantId();
            if (tenantId == null || tenantId.trim().isEmpty()) {
                tenantId = "default";
            }

            this.strictInsertFill(metaObject, "createUser", String.class, userId);
            log.debug("填充createUser完成, 值: {}", userId);

            this.strictInsertFill(metaObject, "modifyUser", String.class, userId);
            log.debug("填充modifyUser完成, 值: {}", userId);

            this.strictInsertFill(metaObject, "tenantId", String.class, tenantId);
            log.debug("填充tenantId完成, 值: {}", tenantId);
        } else {
            // 如果没有用户上下文，使用默认值
            this.strictInsertFill(metaObject, "createUser", String.class, "system");
            log.debug("填充createUser完成, 默认值: system");

            this.strictInsertFill(metaObject, "modifyUser", String.class, "system");
            log.debug("填充modifyUser完成, 默认值: system");

            this.strictInsertFill(metaObject, "tenantId", String.class, "default");
            log.debug("填充tenantId完成, 默认值: default");
        }

        log.debug("插入填充完成");
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        UserContext currentUser = UserContext.getCurrentUser();

        log.debug("执行更新填充: 当前时间={}", now);

        // 填充修改时间
        this.strictUpdateFill(metaObject, "modifyTime", LocalDateTime.class, now);

        // 填充修改人
        if (currentUser != null) {
            // 优先使用userId，如果为null则使用username
            String userId = currentUser.getUserId();
            if (userId == null || userId.trim().isEmpty()) {
                userId = currentUser.getUsername();
            }

            this.strictUpdateFill(metaObject, "modifyUser", String.class, userId);
            log.debug("填充modifyUser完成, 值: {}", userId);
        } else {
            this.strictUpdateFill(metaObject, "modifyUser", String.class, "system");
            log.debug("填充modifyUser完成, 默认值: system");
        }

        log.debug("更新填充完成");
    }
} 