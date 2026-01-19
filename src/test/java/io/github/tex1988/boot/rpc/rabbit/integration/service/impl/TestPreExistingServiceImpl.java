package io.github.tex1988.boot.rpc.rabbit.integration.service.impl;

import io.github.tex1988.boot.rpc.rabbit.annotation.RabbitRpc;
import io.github.tex1988.boot.rpc.rabbit.integration.service.ITestPreExistingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementation for pre-existing infrastructure test service.
 */
@Slf4j
@Service
@RabbitRpc
public class TestPreExistingServiceImpl implements ITestPreExistingService {

    @Override
    public String testOperation(String input) {
        log.info("Pre-existing service processing: {}", input);
        return "Pre-existing result: " + input;
    }
}
