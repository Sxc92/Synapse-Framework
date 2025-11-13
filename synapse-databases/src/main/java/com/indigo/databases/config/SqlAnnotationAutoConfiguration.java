package com.indigo.databases.config;

import com.indigo.databases.annotation.AutoRepository;
import com.indigo.databases.annotation.AutoService;
import com.indigo.databases.proxy.SqlMethodInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.Ordered;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import java.lang.reflect.Proxy;
import java.util.Set;

/**
 * SQL注解自动配置类
 * 自动扫描并注册带有@AutoRepository和@AutoService注解的接口
 *
 * @author 史偕成
 * @date 2025/12/19
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(name = "synapse.databases.sql-annotation.enabled", havingValue = "true", matchIfMissing = false)
public class SqlAnnotationAutoConfiguration {
    

    
    
    /**
     * 自动注册Repository代理
     */
    @Bean
    public RepositoryProxyRegistrar repositoryProxyRegistrar() {
        return new RepositoryProxyRegistrar();
    }
    
    /**
     * 自动注册Service代理
     */
    @Bean
    public ServiceProxyRegistrar serviceProxyRegistrar() {
        return new ServiceProxyRegistrar();
    }
    
    /**
     * 自动注册Repository代理（重命名避免冲突）
     */
    @Bean
    public AutoRepositoryRegistrar sqlAnnotationAutoRepositoryRegistrar() {
        return new AutoRepositoryRegistrar();
    }
    
    /**
     * 自动Repository注册器
     * 
     * <p><b>执行顺序说明：</b>
     * 实现 {@code Ordered} 接口，设置较高的优先级值（更晚执行），
     * 确保在大部分 {@code BeanPostProcessor} 初始化之后再注册 Bean，
     * 从而避免 "not eligible for getting processed by all BeanPostProcessors" 警告。
     * 
     * <p><b>优先级说明：</b>
     * - {@code Ordered.LOWEST_PRECEDENCE} = {@code Integer.MAX_VALUE}（最晚执行）
     * - 这里使用 {@code Integer.MAX_VALUE - 100}，确保在大部分 BeanPostProcessor 之后执行
     * - 但仍然在 Bean 实例化之前完成注册
     */
    public static class AutoRepositoryRegistrar implements BeanFactoryPostProcessor, Ordered {
        
        @Override
        public int getOrder() {
            // 设置较高的优先级值（更晚执行），确保在 BeanPostProcessor 初始化之后执行
            // 使用 Integer.MAX_VALUE - 100 而不是 LOWEST_PRECEDENCE，避免与其他后处理器冲突
            return Integer.MAX_VALUE - 100;
        }
        
        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            try {
                log.debug("开始扫描@AutoRepository注解的接口...");
                
                // 扫描所有带有@AutoRepository注解的接口
                // 使用Spring的包扫描机制，支持配置的包路径
                Reflections reflections = new Reflections("com.indigo");
                Set<Class<?>> repositoryInterfaces = reflections.getTypesAnnotatedWith(AutoRepository.class);
                
                log.debug("找到{}个带有@AutoRepository注解的接口", repositoryInterfaces.size());
                
                for (Class<?> repositoryInterface : repositoryInterfaces) {
                    log.debug("发现Repository接口: {}", repositoryInterface.getName());
                    if (repositoryInterface.isInterface()) {
                        registerRepositoryProxy(repositoryInterface, beanFactory);
                    }
                }
            } catch (Exception e) {
                log.error("Error registering auto repositories", e);
            }
        }
        
        /**
         * 注册单个Repository代理
         */
        private void registerRepositoryProxy(Class<?> repositoryInterface, ConfigurableListableBeanFactory beanFactory) {
            try {
                String beanName = repositoryInterface.getSimpleName();
                
                // 创建代理对象
                Object proxy = Proxy.newProxyInstance(
                    repositoryInterface.getClassLoader(),
                    new Class<?>[]{repositoryInterface},
                    new SqlMethodInterceptor()
                );
                
                // 注册为Spring Bean
                if (!beanFactory.containsBean(beanName)) {
                    if (beanFactory instanceof DefaultListableBeanFactory defaultListableBeanFactory) {

                        // 使用FactoryBean来创建代理
                        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ProxyFactoryBean.class);
                        builder.addPropertyValue("targetClass", repositoryInterface);
                        builder.addPropertyValue("proxyObject", proxy);
                        
                        BeanDefinition beanDefinition = builder.getBeanDefinition();
                        beanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);
                        beanDefinition.setLazyInit(true);  // 设置延迟初始化
                        
                        defaultListableBeanFactory.registerBeanDefinition(beanName, beanDefinition);
                        log.debug("Registered AutoRepository proxy (lazy): {} as {}", beanName, repositoryInterface.getName());
                    }
                } else {
                    log.debug("Bean {} already exists, skipping registration", beanName);
                }
            } catch (Exception e) {
                log.error("Error registering repository proxy for {}", repositoryInterface.getName(), e);
            }
        }
    }
    
    /**
     * Repository代理注册器
     */
    @Component
    public static class RepositoryProxyRegistrar implements BeanFactoryPostProcessor, Ordered {
        
        @Override
        public void postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory) throws BeansException {
            registerProxies((BeanDefinitionRegistry) beanFactory);
        }
        
        @Override
        public int getOrder() {
            // 设置较高的优先级值（更晚执行），确保在 BeanPostProcessor 初始化之后执行
            // 与 AutoRepositoryRegistrar 保持一致，使用相同的优先级值
            return Integer.MAX_VALUE - 100;
        }
        
        public void registerProxies() {
            // 这个方法保留用于兼容性
        }
        
        public void registerProxies(BeanDefinitionRegistry registry) {
            try {
                // 扫描带有@AutoRepository注解的接口
                ClassPathScanningCandidateComponentProvider scanner = 
                    new ClassPathScanningCandidateComponentProvider(false);
                scanner.addIncludeFilter(new AnnotationTypeFilter(AutoRepository.class));
                
                Set<BeanDefinition> candidates = scanner.findCandidateComponents("com.indigo");
                
                for (BeanDefinition beanDefinition : candidates) {
                    String className = beanDefinition.getBeanClassName();
                    Class<?> interfaceClass = Class.forName(className);
                    
                    if (interfaceClass.isInterface()) {
                        // 创建BeanDefinition
                        GenericBeanDefinition beanDef = new GenericBeanDefinition();
                        beanDef.setBeanClass(ProxyFactoryBean.class);
                        beanDef.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);
                        beanDef.setLazyInit(true);  // 设置延迟初始化
                        
                        // 设置代理接口
                        beanDef.getPropertyValues().add("proxyInterface", interfaceClass);
                        
                        // 注册为Spring Bean
                        String beanName = interfaceClass.getSimpleName();
                        if (!registry.containsBeanDefinition(beanName)) {
                            registry.registerBeanDefinition(beanName, beanDef);
                            log.info("Registered Repository proxy (lazy): {}", beanName);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error registering Repository proxies", e);
            }
        }
    }
    
    /**
     * Service代理注册器
     */
    @Component
    public static class ServiceProxyRegistrar implements BeanFactoryPostProcessor, Ordered {
        
        @Override
        public void postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory) throws BeansException {
            registerProxies((BeanDefinitionRegistry) beanFactory);
        }
        
        @Override
        public int getOrder() {
            // 设置较高的优先级值（更晚执行），确保在 BeanPostProcessor 初始化之后执行
            // 与 AutoRepositoryRegistrar 保持一致，使用相同的优先级值
            return Integer.MAX_VALUE - 100;
        }
        
        public void registerProxies() {
            // 这个方法保留用于兼容性
        }
        
        public void registerProxies(BeanDefinitionRegistry registry) {
            try {
                // 扫描带有@AutoService注解的接口
                ClassPathScanningCandidateComponentProvider scanner = 
                    new ClassPathScanningCandidateComponentProvider(false);
                scanner.addIncludeFilter(new AnnotationTypeFilter(AutoService.class));
                
                Set<BeanDefinition> candidates = scanner.findCandidateComponents("com.indigo");
                
                for (BeanDefinition beanDefinition : candidates) {
                    String className = beanDefinition.getBeanClassName();
                    Class<?> interfaceClass = Class.forName(className);
                    
                    if (interfaceClass.isInterface()) {
                        // 创建BeanDefinition
                        GenericBeanDefinition beanDef = new GenericBeanDefinition();
                        beanDef.setBeanClass(ProxyFactoryBean.class);
                        beanDef.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);
                        beanDef.setLazyInit(true);  // 设置延迟初始化
                        
                        // 设置代理接口
                        beanDef.getPropertyValues().add("proxyInterface", interfaceClass);
                        
                        // 注册为Spring Bean
                        String beanName = interfaceClass.getSimpleName();
                        if (!registry.containsBeanDefinition(beanName)) {
                            registry.registerBeanDefinition(beanName, beanDef);
                            log.info("Registered Service proxy (lazy): {}", beanName);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error registering Service proxies", e);
            }
        }
    }
} 