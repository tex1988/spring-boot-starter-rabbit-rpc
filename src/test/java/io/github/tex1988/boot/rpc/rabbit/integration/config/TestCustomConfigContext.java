package io.github.tex1988.boot.rpc.rabbit.integration.config;

import io.github.tex1988.boot.rpc.rabbit.annotation.EnableRabbitRpc;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration with custom executor, message converter, and error handler.
 * Uses 'custom-config' profile to isolate custom beans from other test contexts.
 */
@Profile("custom-config")
@EnableRabbitRpc(
        enableClient = true,
        enableServer = true,
        scanBasePackages = {"io.github.tex1988.boot.rpc.rabbit.integration.service"},
        allowedSerializationPatterns = {"io.github.tex1988.boot.rpc.rabbit.integration.model.*"},
        replyTimeout = 8000L,
        concurrency = "2-4",
        executor = "customTaskExecutor",           // ← Custom executor
        messageConverter = "customMessageConverter", // ← Custom message converter
        errorHandler = "customErrorHandler"        // ← Custom error handler
)
@SpringBootApplication(scanBasePackages = {"io.github.tex1988.boot.rpc.rabbit"})
public class TestCustomConfigContext extends BaseConfig {

    /**
     * Custom task executor bean.
     * Registered BEFORE @EnableRabbitRpc initialization to avoid circular dependency.
     */
    @Bean
    public CustomConfigBeans.CustomTaskExecutor customTaskExecutor() {
        return new CustomConfigBeans.CustomTaskExecutor();
    }

    /**
     * Custom message converter bean.
     * Registered BEFORE @EnableRabbitRpc initialization to avoid circular dependency.
     */
    @Bean
    public CustomConfigBeans.CustomMessageConverter customMessageConverter() {
        return new CustomConfigBeans.CustomMessageConverter();
    }

    /**
     * Custom error handler bean.
     * Registered BEFORE @EnableRabbitRpc initialization to avoid circular dependency.
     */
    @Bean
    public CustomConfigBeans.CustomErrorHandler customErrorHandler() {
        return new CustomConfigBeans.CustomErrorHandler();
    }
}
