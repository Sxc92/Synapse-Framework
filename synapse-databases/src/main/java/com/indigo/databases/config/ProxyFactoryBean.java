package com.indigo.databases.config;

import com.indigo.databases.proxy.SqlMethodInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Proxy;

/**
 * 代理工厂Bean
 * 用于创建动态代理实例
 *
 * @author 史偕成
 * @date 2025/12/19
 */
@Slf4j
public class ProxyFactoryBean<T> implements FactoryBean<T>, ApplicationContextAware, InitializingBean {
    
    private Class<T> targetClass;
    private Object proxyObject;
    private ApplicationContext applicationContext;
    private SqlMethodInterceptor sqlMethodInterceptor;
    
    public void setTargetClass(Class<T> targetClass) {
        this.targetClass = targetClass;
    }
    
    public void setProxyObject(Object proxyObject) {
        this.proxyObject = proxyObject;
    }
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        if (proxyObject == null && targetClass != null) {
            // 获取SqlMethodInterceptor
            sqlMethodInterceptor = applicationContext.getBean(SqlMethodInterceptor.class);
            
            // 创建代理对象
            proxyObject = Proxy.newProxyInstance(
                targetClass.getClassLoader(),
                new Class<?>[]{targetClass},
                sqlMethodInterceptor
            );
            
            log.debug("ProxyFactoryBean created proxy for: {}", targetClass.getSimpleName());
        }
    }
    
    @Override
    public T getObject() {
        if (proxyObject == null) {
            // 如果afterPropertiesSet尚未调用，则在此处创建作为备用
            if (targetClass == null) {
                throw new IllegalStateException("Target class not set for ProxyFactoryBean");
            }
            if (sqlMethodInterceptor == null) {
                sqlMethodInterceptor = applicationContext.getBean(SqlMethodInterceptor.class);
            }
            proxyObject = Proxy.newProxyInstance(
                targetClass.getClassLoader(),
                new Class<?>[]{targetClass},
                sqlMethodInterceptor
            );
            log.debug("Proxy object for {} created on demand in getObject().", targetClass.getSimpleName());
        }
        log.debug("Returning proxy object for {}.", targetClass.getSimpleName());
        return (T) proxyObject;
    }
    
    @Override
    public Class<?> getObjectType() {
        return targetClass;
    }
    
    @Override
    public boolean isSingleton() {
        return true;
    }
} 