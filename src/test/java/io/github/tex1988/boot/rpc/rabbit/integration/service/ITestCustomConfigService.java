package io.github.tex1988.boot.rpc.rabbit.integration.service;

import io.github.tex1988.boot.rpc.rabbit.annotation.RabbitRpcInterface;

/**
 * Test service for custom configuration tests.
 */
@RabbitRpcInterface(
        exchange = "test.custom.config.exchange",
        queue = "test.custom.config.queue",
        routing = "test.custom.config.routing"
)
public interface ITestCustomConfigService {

    /**
     * Simple operation for testing custom configuration.
     */
    String processWithCustomConfig(String input);
}
