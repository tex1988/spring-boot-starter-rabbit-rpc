package io.github.tex1988.boot.rpc.rabbit.integration.service.impl;

import io.github.tex1988.boot.rpc.rabbit.annotation.RabbitRpc;
import io.github.tex1988.boot.rpc.rabbit.integration.service.ITestSharedQueueServiceB;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementation for shared queue service B.
 * Shares queue with ServiceA - acts as competing consumer.
 */
@Slf4j
@Service
@RabbitRpc
public class TestSharedQueueServiceBImpl implements ITestSharedQueueServiceB {

    @Override
    public String serviceBOperation(String input) {
        log.info("Service B processing: {}", input);
        return "Service B result: " + input;
    }
}
