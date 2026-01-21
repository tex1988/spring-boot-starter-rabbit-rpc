package io.github.tex1988.boot.rpc.rabbit.integration.service;

import io.github.tex1988.boot.rpc.rabbit.annotation.RabbitRpcInterface;
import io.github.tex1988.boot.rpc.rabbit.integration.model.TestUser;
import io.github.tex1988.boot.rpc.rabbit.integration.unregistered.UnregisteredClass;
import jakarta.validation.constraints.NotNull;

/**
 * Test RPC service interface specifically for testing serialization errors with unregistered classes.
 * Uses a separate exchange/queue to avoid affecting other tests.
 */
@RabbitRpcInterface(
        exchange = "test.unregistered.exchange",
        queue = "test.unregistered.queue",
        routing = "test.unregistered.routing"
)
public interface ITestUnregisteredService {

    /**
     * Method that returns an unregistered class.
     * Server will fail to serialize the response due to allowedSerializationPatterns.
     */
    UnregisteredClass getUnregisteredData(@NotNull Long id);

    /**
     * Method that accepts an unregistered class as parameter.
     * Server will fail to deserialize the request due to allowedSerializationPatterns.
     */
    TestUser processUnregisteredData(UnregisteredClass data);
}
