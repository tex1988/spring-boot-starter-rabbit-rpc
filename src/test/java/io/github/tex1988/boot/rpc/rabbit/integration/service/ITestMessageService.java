package io.github.tex1988.boot.rpc.rabbit.integration.service;

import io.github.tex1988.boot.rpc.rabbit.annotation.FireAndForget;
import io.github.tex1988.boot.rpc.rabbit.annotation.RabbitRpcInterface;
import io.github.tex1988.boot.rpc.rabbit.integration.model.TestMessage;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

/**
 * Test RPC service interface for message operations.
 * Demonstrates fire-and-forget patterns and message processing.
 */
@RabbitRpcInterface(
        exchange = "test.message.exchange",
        queue = "test.message.queue",
        routing = "test.message.routing"
)
public interface ITestMessageService {

    /**
     * Process a message synchronously and return the result.
     */
    String processMessage(@Validated TestMessage message);

    /**
     * Send a message asynchronously (fire-and-forget).
     */
    @FireAndForget
    void sendMessage(@Validated TestMessage message);

    /**
     * Echo a simple string message.
     */
    String echo(@NotNull String message);

    /**
     * Simulate a long-running operation.
     */
    String longRunningOperation(@NotNull String taskId);
}

