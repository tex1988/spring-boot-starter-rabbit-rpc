package io.github.tex1988.boot.rpc.rabbit.integration.service;

import io.github.tex1988.boot.rpc.rabbit.annotation.RabbitRpcInterface;

/**
 * Test service B that SHARES a queue with ITestSharedQueueServiceA.
 * Both services use the same queue AND same routing key.
 * This tests the competing consumers pattern where multiple service instances
 * (or different services implementing similar interfaces) consume from the same queue.
 */
@RabbitRpcInterface(
        exchange = "test.shared.exchange",
        queue = "test.shared.queue",
        routing = "test.shared.routing"  // Same routing key as ServiceA
)
public interface ITestSharedQueueServiceB {

    /**
     * Service B operation.
     */
    String serviceBOperation(String input);
}
