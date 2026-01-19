package io.github.tex1988.boot.rpc.rabbit.integration.service;

import io.github.tex1988.boot.rpc.rabbit.annotation.RabbitRpcInterface;

/**
 * Test service for pre-existing infrastructure tests.
 * Uses dedicated exchange/queue that won't interfere with other tests.
 */
@RabbitRpcInterface(
        exchange = "test.preexisting.exchange",
        queue = "test.preexisting.queue",
        routing = "test.preexisting.routing"
)
public interface ITestPreExistingService {

    /**
     * Simple operation to verify infrastructure works.
     */
    String testOperation(String input);
}
