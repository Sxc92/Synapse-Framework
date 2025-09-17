package com.indigo.security.config;

import com.indigo.security.core.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AuthenticationService Bean 配置测试
 *
 * @author 史偕成
 * @date 2025/01/19
 */
public class AuthenticationServiceTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    SecurityAutoConfiguration.class
            ));

    @Test
    public void testAuthenticationServiceBeanWithoutDependencies() {
        this.contextRunner
                .run(context -> {
                    // 验证 AuthenticationService Bean 被正确创建（简化版本）
                    assertThat(context).hasSingleBean(AuthenticationService.class);
                    
                    // 验证 Bean 类型
                    AuthenticationService authService = context.getBean(AuthenticationService.class);
                    assertThat(authService).isNotNull();
                    assertThat(authService.getClass().getSimpleName()).isEqualTo("DefaultAuthenticationService");
                });
    }
}
