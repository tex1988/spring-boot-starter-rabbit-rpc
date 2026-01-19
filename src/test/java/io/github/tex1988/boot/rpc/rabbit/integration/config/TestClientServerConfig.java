package io.github.tex1988.boot.rpc.rabbit.integration.config;

import io.github.tex1988.boot.rpc.rabbit.annotation.EnableRabbitRpc;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Test configuration for combined RabbitMQ RPC Client and Server.
 * Enables both client and server-side RPC functionality for testing scenarios
 * where both roles exist in the same application.
 */
@EnableRabbitRpc(
        enableClient = true,
        enableServer = true,
        scanBasePackages = {"io.github.tex1988.boot.rpc.rabbit.integration.service"},
        allowedSerializationPatterns = {
                "io.github.tex1988.boot.rpc.rabbit.integration.model.*"
        },
        replyTimeout = 10000L,
        concurrency = "3-5"
)
@SpringBootApplication(scanBasePackages = {"io.github.tex1988.boot.rpc.rabbit"})
public class TestClientServerConfig {
}

