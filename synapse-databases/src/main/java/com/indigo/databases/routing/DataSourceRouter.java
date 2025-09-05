package com.indigo.databases.routing;

import java.util.List;

/**
 * 数据源路由策略接口
 * 定义不同路由策略的基本行为
 *
 * @author 史偕成
 * @date 2025/01/19
 */
public interface DataSourceRouter {
    
    /**
     * 选择数据源
     *
     * @param availableDataSources 可用的数据源列表
     * @param context 路由上下文
     * @return 选择的数据源名称
     */
    String selectDataSource(List<String> availableDataSources, RoutingContext context);
    
    /**
     * 获取路由策略名称
     *
     * @return 策略名称
     */
    String getStrategyName();
    
    /**
     * 路由上下文
     */
    class RoutingContext {
        private final String currentDataSource;
        private final SqlType sqlType;
        private final String userId;
        private final String tenantId;
        
        public RoutingContext(String currentDataSource, SqlType sqlType, String userId, String tenantId) {
            this.currentDataSource = currentDataSource;
            this.sqlType = sqlType;
            this.userId = userId;
            this.tenantId = tenantId;
        }
        
        public String getCurrentDataSource() {
            return currentDataSource;
        }
        
        public SqlType getSqlType() {
            return sqlType;
        }
        
        public String getUserId() {
            return userId;
        }
        
        public String getTenantId() {
            return tenantId;
        }
    }
    
    /**
     * SQL类型枚举
     */
    enum SqlType {
        SELECT(true), 
        INSERT(false), 
        UPDATE(false), 
        DELETE(false), 
        MERGE(false), 
        CALL(false), 
        OTHER(true);
        
        private final boolean isReadOperation;
        
        SqlType(boolean isReadOperation) {
            this.isReadOperation = isReadOperation;
        }
        
        public boolean isReadOperation() {
            return isReadOperation;
        }
    }
}
