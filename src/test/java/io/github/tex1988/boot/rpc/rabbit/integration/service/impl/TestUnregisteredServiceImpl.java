package io.github.tex1988.boot.rpc.rabbit.integration.service.impl;

import io.github.tex1988.boot.rpc.rabbit.annotation.RabbitRpc;
import io.github.tex1988.boot.rpc.rabbit.integration.model.TestUser;
import io.github.tex1988.boot.rpc.rabbit.integration.service.ITestUnregisteredService;
import io.github.tex1988.boot.rpc.rabbit.integration.unregistered.UnregisteredClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Test implementation for ITestUnregisteredService.
 * Used specifically for testing serialization errors with unregistered classes.
 */
@Slf4j
@Service
@RabbitRpc
public class TestUnregisteredServiceImpl implements ITestUnregisteredService {

    @Override
    public UnregisteredClass getUnregisteredData(Long id) {
        log.info("Getting unregistered data with id: {}", id);
        // This method will cause serialization to fail on the server side
        // because UnregisteredClass is not in the allowed serialization patterns
        return new UnregisteredClass(id, "Sensitive Data", true);
    }

    @Override
    public TestUser processUnregisteredData(UnregisteredClass data) {
        log.info("Processing unregistered data: {}", data);
        // This method will cause deserialization to fail on the server side
        // because UnregisteredClass is not in the allowed serialization patterns
        return new TestUser(data.getId(), "Processed-" + data.getData(), "processed@example.com", true);
    }
}
