package com.indigo.security.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sa-Token Banner 配置测试
 * 验证 is-print 配置是否正确应用
 */
@SpringBootTest(classes = SecurityAutoConfiguration.class)
@TestPropertySource(properties = {
    "synapse.security.enabled=true",
    "synapse.security.satoken.enabled=true",
    "synapse.security.satoken.is-print=false"
})
class SaTokenBannerTest {

    @Test
    void testSaTokenBannerDisabled() {
        // 验证系统属性是否正确设置
        String isPrint = System.getProperty("sa-token.is-print");
        assertEquals("false", isPrint, "sa-token.is-print 应该设置为 false");
        
        // 验证其他相关属性
        String tokenName = System.getProperty("sa-token.token-name");
        assertEquals("satoken", tokenName, "sa-token.token-name 应该设置为 satoken");
        
        String timeout = System.getProperty("sa-token.timeout");
        assertEquals("2592000", timeout, "sa-token.timeout 应该设置为 2592000");
    }
}
