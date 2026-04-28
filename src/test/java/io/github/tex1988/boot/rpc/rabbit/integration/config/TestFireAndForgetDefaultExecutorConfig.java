package io.github.tex1988.boot.rpc.rabbit.integration.config;

import io.github.tex1988.boot.rpc.rabbit.annotation.EnableRabbitRpc;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration for fire-and-forget with default executor (no fireAndForgetExecutor specified).
 * Verifies that the fallback to Executors.newCachedThreadPool() works correctly.
 */
@Profile("fire-and-forget-default-executor")
@EnableRabbitRpc(
        enableClient = true,
        enableServer = true,
        scanBasePackages = {"io.github.tex1988.boot.rpc.rabbit.integration.service"},
        allowedSerializationPatterns = {"io.github.tex1988.boot.rpc.rabbit.integration.model.*"}
        // Note: fireAndForgetExecutor is NOT specified - should use default fallback
)
@SpringBootApplication(scanBasePackages = {"io.github.tex1988.boot.rpc.rabbit"})
public class TestFireAndForgetDefaultExecutorConfig extends BaseConfig {
}
