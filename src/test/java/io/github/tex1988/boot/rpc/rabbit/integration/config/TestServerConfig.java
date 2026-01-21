package io.github.tex1988.boot.rpc.rabbit.integration.config;

import io.github.tex1988.boot.rpc.rabbit.annotation.EnableRabbitRpc;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Test configuration for RabbitMQ RPC Server.
 * Enables server-side RPC functionality with test service packages.
 */
@EnableRabbitRpc(
        enableServer = true,
        scanBasePackages = {"io.github.tex1988.boot.rpc.rabbit.integration.service"},
        allowedSerializationPatterns = {"io.github.tex1988.boot.rpc.rabbit.integration.model.*"},
        concurrency = "3-5"
)
@SpringBootApplication(scanBasePackages = {"io.github.tex1988.boot.rpc.rabbit"})
public class TestServerConfig extends BaseConfig {
}
