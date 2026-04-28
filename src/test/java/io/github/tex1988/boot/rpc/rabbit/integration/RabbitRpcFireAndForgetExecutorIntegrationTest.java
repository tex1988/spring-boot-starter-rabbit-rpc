package io.github.tex1988.boot.rpc.rabbit.integration;

import io.github.tex1988.boot.rpc.rabbit.integration.config.CustomConfigBeans;
import io.github.tex1988.boot.rpc.rabbit.integration.config.TestFireAndForgetExecutorConfig;
import io.github.tex1988.boot.rpc.rabbit.integration.model.TestMessage;
import io.github.tex1988.boot.rpc.rabbit.integration.service.ITestMessageService;
import io.github.tex1988.boot.rpc.rabbit.integration.service.impl.TestMessageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for fire-and-forget executor configuration.
 * Tests that @FireAndForget methods execute on custom executor.
 */
@SpringBootTest(classes = TestFireAndForgetExecutorConfig.class)
@org.springframework.test.context.ActiveProfiles("fire-and-forget-executor")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("RabbitMQ RPC Fire-and-Forget Executor Integration Tests")
class RabbitRpcFireAndForgetExecutorIntegrationTest extends AbstractRabbitRpcIntegrationTest {

    @Autowired
    private ITestMessageService iTestMessageServiceClient;

    @Autowired
    private TestMessageServiceImpl messageServiceImpl;

    @Autowired
    @Qualifier("customFireAndForgetExecutor")
    private CustomConfigBeans.CustomFireAndForgetExecutor customFireAndForgetExecutor;

    @BeforeEach
    void setUp() {
        messageServiceImpl.resetMessageCount();
    }

    @Test
    @DisplayName("Should execute fire-and-forget methods on custom executor")
    void shouldExecuteFireAndForgetOnCustomExecutor() {
        // Given
        int initialTaskCount = customFireAndForgetExecutor.getExecutedTaskCount();
        TestMessage message = new TestMessage("Test message", System.currentTimeMillis());

        // When
        iTestMessageServiceClient.sendMessage(message);

        // Then - verify message was processed
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() ->
                        assertThat(messageServiceImpl.getMessageCount()).isEqualTo(1)
                );

        // Verify custom executor was used
        assertThat(customFireAndForgetExecutor.getExecutedTaskCount()).isGreaterThan(initialTaskCount);
    }

    @Test
    @DisplayName("Should execute multiple fire-and-forget methods on custom executor")
    void shouldExecuteMultipleFireAndForgetOnCustomExecutor() {
        // Given
        int initialTaskCount = customFireAndForgetExecutor.getExecutedTaskCount();
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

        // Verify custom executor was used for all messages
        assertThat(customFireAndForgetExecutor.getExecutedTaskCount()).isGreaterThanOrEqualTo(initialTaskCount + messageCount);
    }

    @Test
    @DisplayName("Should not block listener threads during fire-and-forget execution")
    void shouldNotBlockListenerThreads() {
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
