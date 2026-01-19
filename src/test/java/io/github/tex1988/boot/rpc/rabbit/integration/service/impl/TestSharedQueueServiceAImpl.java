package io.github.tex1988.boot.rpc.rabbit.integration.service.impl;

import io.github.tex1988.boot.rpc.rabbit.annotation.RabbitRpc;
import io.github.tex1988.boot.rpc.rabbit.integration.service.ITestSharedQueueServiceA;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementation for shared queue service A.
 * Shares queue with ServiceB - acts as competing consumer.
 */
@Slf4j
@Service
@RabbitRpc
public class TestSharedQueueServiceAImpl implements ITestSharedQueueServiceA {

    @Override
    public String serviceAOperation(String input) {
        log.info("Service A processing: {}", input);
        return "Service A result: " + input;
    }
}
