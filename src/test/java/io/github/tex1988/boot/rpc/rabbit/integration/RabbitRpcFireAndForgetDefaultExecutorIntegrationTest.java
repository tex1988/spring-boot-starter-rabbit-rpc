package io.github.tex1988.boot.rpc.rabbit.integration;

import io.github.tex1988.boot.rpc.rabbit.integration.config.TestFireAndForgetDefaultExecutorConfig;
import io.github.tex1988.boot.rpc.rabbit.integration.model.TestMessage;
import io.github.tex1988.boot.rpc.rabbit.integration.service.ITestMessageService;
import io.github.tex1988.boot.rpc.rabbit.integration.service.impl.TestMessageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for fire-and-forget with default executor (fallback to newCachedThreadPool).
 * Verifies that fire-and-forget works without explicit fireAndForgetExecutor configuration.
 */
@SpringBootTest(classes = TestFireAndForgetDefaultExecutorConfig.class)
@ActiveProfiles("fire-and-forget-default-executor")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("RabbitMQ RPC Fire-and-Forget Default Executor Integration Tests")
class RabbitRpcFireAndForgetDefaultExecutorIntegrationTest extends AbstractRabbitRpcIntegrationTest {

    @Autowired
    private ITestMessageService iTestMessageServiceClient;

    @Autowired
    private TestMessageServiceImpl messageServiceImpl;

    @BeforeEach
    void setUp() {
        messageServiceImpl.resetMessageCount();
    }

    @Test
    @DisplayName("Should execute fire-and-forget with default executor (newCachedThreadPool)")
    void shouldExecuteFireAndForgetWithDefaultExecutor() {
        // Given
        TestMessage message = new TestMessage("Default executor test", System.currentTimeMillis());

        // When
        iTestMessageServiceClient.sendMessage(message);

        // Then - verify message was processed asynchronously
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() ->
                        assertThat(messageServiceImpl.getMessageCount()).isEqualTo(1)
                );
    }

    @Test
    @DisplayName("Should handle multiple fire-and-forget messages with default executor")
    void shouldHandleMultipleMessagesWithDefaultExecutor() {
        // Given
        int messageCount = 10;

        // When
        for (int i = 0; i < messageCount; i++) {
            TestMessage message = new TestMessage("Message " + i, System.currentTimeMillis());
            iTestMessageServiceClient.sendMessage(message);
        }

        // Then - verify all messages were processed
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() ->
                        assertThat(messageServiceImpl.getMessageCount()).isEqualTo(messageCount)
                );
    }

    @Test
    @DisplayName("Should not block listener threads with default executor")
    void shouldNotBlockListenerThreadsWithDefaultExecutor() {
        // Given
        int messageCount = 20;
        long startTime = System.currentTimeMillis();

        // When - send messages rapidly
        for (int i = 0; i < messageCount; i++) {
            TestMessage message = new TestMessage("Rapid message " + i, System.currentTimeMillis());
            iTestMessageServiceClient.sendMessage(message);
        }
        long sendDuration = System.currentTimeMillis() - startTime;

        // Then - sending should be fast (not blocked by processing)
        assertThat(sendDuration).isLessThan(3000L);

        // Verify all messages were eventually processed
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() ->
                        assertThat(messageServiceImpl.getMessageCount()).isEqualTo(messageCount)
                );
    }
}
