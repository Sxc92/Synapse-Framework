package com.indigo.security.aspect;

/**
 * 数据权限上下文
 * 
 * <p><b>注意：</b>此功能已暂时注释，待业务完整后扩展
 *
 * @author 史偕成
 * @date 2025/01/09
 */
// TODO: 待业务完整后恢复数据权限上下文功能
/*
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DataPermissionContext {

    private static final ThreadLocal<String> DATA_SCOPE = new ThreadLocal<>();

    public static void setDataScope(String dataScope) {
        DATA_SCOPE.set(dataScope);
        log.info("设置数据范围: {}", dataScope);
    }

    public static String getDataScope() {
        String dataScope = DATA_SCOPE.get();
        log.info("获取数据范围: {}", dataScope);
        return dataScope;
    }

    public static void clear() {
        DATA_SCOPE.remove();
        log.info("清除数据范围");
    }
} 
*/
