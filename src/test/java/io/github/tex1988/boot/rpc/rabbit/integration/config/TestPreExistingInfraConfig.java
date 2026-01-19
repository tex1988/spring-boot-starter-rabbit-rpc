package io.github.tex1988.boot.rpc.rabbit.integration.config;

import io.github.tex1988.boot.rpc.rabbit.annotation.EnableRabbitRpc;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Test configuration for pre-existing infrastructure tests.
 * Uses different scan packages and configuration to isolate from other tests.
 */
@EnableRabbitRpc(
        enableClient = true,
        enableServer = true,
        scanBasePackages = {"io.github.tex1988.boot.rpc.rabbit.integration.service"},
        allowedSerializationPatterns = {"io.github.tex1988.boot.rpc.rabbit.integration.model.*"},
        replyTimeout = 10000L,
        concurrency = "2-3"  // Different from other tests to verify it's a separate context
)
@SpringBootApplication(scanBasePackages = {"io.github.tex1988.boot.rpc.rabbit"})
public class TestPreExistingInfraConfig {
}
