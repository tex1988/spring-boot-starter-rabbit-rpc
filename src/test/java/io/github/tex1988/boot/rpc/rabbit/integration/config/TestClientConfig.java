package io.github.tex1988.boot.rpc.rabbit.integration.config;

import io.github.tex1988.boot.rpc.rabbit.annotation.EnableRabbitRpc;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Test configuration for RabbitMQ RPC Client.
 * Enables client-side RPC functionality with test service packages.
 */
@EnableRabbitRpc(
        enableClient = true,
        scanBasePackages = {"io.github.tex1988.boot.rpc.rabbit.integration.service"},
        allowedSerializationPatterns = {"io.github.tex1988.boot.rpc.rabbit.integration.model.*"},
        replyTimeout = 10000L
)
@SpringBootApplication(scanBasePackages = {"io.github.tex1988.boot.rpc.rabbit"})
public class TestClientConfig {
}
