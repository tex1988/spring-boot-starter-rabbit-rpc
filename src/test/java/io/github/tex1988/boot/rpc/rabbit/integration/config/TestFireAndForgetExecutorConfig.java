package io.github.tex1988.boot.rpc.rabbit.integration.config;

import io.github.tex1988.boot.rpc.rabbit.annotation.EnableRabbitRpc;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration for fire-and-forget executor integration tests.
 */
@Profile("fire-and-forget-executor")
@EnableRabbitRpc(
        enableClient = true,
        enableServer = true,
        scanBasePackages = {"io.github.tex1988.boot.rpc.rabbit.integration.service"},
        allowedSerializationPatterns = {"io.github.tex1988.boot.rpc.rabbit.integration.model.*"},
        executor = "customFireAndForgetExecutor"
)
@SpringBootApplication(scanBasePackages = {"io.github.tex1988.boot.rpc.rabbit"})
public class TestFireAndForgetExecutorConfig extends BaseConfig {

    @Bean
    public CustomConfigBeans.CustomFireAndForgetExecutor customFireAndForgetExecutor() {
        return new CustomConfigBeans.CustomFireAndForgetExecutor();
    }
}
