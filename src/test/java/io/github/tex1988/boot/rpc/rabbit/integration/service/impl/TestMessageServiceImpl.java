package io.github.tex1988.boot.rpc.rabbit.integration.service.impl;

import io.github.tex1988.boot.rpc.rabbit.annotation.RabbitRpc;
import io.github.tex1988.boot.rpc.rabbit.integration.model.TestMessage;
import io.github.tex1988.boot.rpc.rabbit.integration.service.ITestMessageService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Test implementation of ITestMessageService for integration testing.
 */
@Slf4j
@Service
@RabbitRpc
public class TestMessageServiceImpl implements ITestMessageService {

    private final AtomicLong messageCounter = new AtomicLong(0);

    @Override
    public String processMessage(TestMessage message) {
        log.info("Processing message: {}", message);
        return "Processed: " + message.getContent() + " at " + message.getTimestamp();
    }

    @Override
    public void sendMessage(TestMessage message) {
        log.info("Sending message asynchronously: {}", message);
        long count = messageCounter.incrementAndGet();
        log.info("Total messages sent: {}", count);
    }

    @Override
    public String echo(String message) {
        log.info("Echoing message: {}", message);
        return "Echo: " + message;
    }

    @Override
    @SneakyThrows
    public String longRunningOperation(String taskId) {
        log.info("Starting long-running operation for task: {}", taskId);
        Thread.sleep(2000); //NOSONAR
        log.info("Completed long-running operation for task: {}", taskId);
        return "Completed task: " + taskId;
    }

    /**
     * Helper method for tests to get message count.
     */
    public long getMessageCount() {
        return messageCounter.get();
    }

    /**
     * Helper method for tests to reset message count.
     */
    public void resetMessageCount() {
        messageCounter.set(0);
    }
}

