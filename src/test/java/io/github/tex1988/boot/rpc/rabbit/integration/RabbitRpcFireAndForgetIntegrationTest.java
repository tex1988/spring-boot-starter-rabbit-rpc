package io.github.tex1988.boot.rpc.rabbit.integration;

import io.github.tex1988.boot.rpc.rabbit.integration.config.TestClientServerConfig;
import io.github.tex1988.boot.rpc.rabbit.integration.model.TestMessage;
import io.github.tex1988.boot.rpc.rabbit.integration.service.ITestMessageService;
import io.github.tex1988.boot.rpc.rabbit.integration.service.impl.TestMessageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Integration tests for fire-and-forget RPC operations.
 * Tests asynchronous message processing without waiting for responses.
 */
@SpringBootTest(classes = TestClientServerConfig.class)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("RabbitMQ RPC Fire-and-Forget Integration Tests")
class RabbitRpcFireAndForgetIntegrationTest extends AbstractRabbitRpcIntegrationTest {

    @Autowired
    private ITestMessageService iTestMessageServiceClient;

    @Autowired
    private TestMessageServiceImpl messageServiceImpl;

    @BeforeEach
    void setUp() {
        messageServiceImpl.resetMessageCount();
    }

    @Test
    @DisplayName("Should send fire-and-forget message without waiting for response")
    void shouldSendFireAndForgetMessage() {
        // Given
        TestMessage message = new TestMessage("Fire and forget test", System.currentTimeMillis());

        // When - this should return immediately without waiting for processing
        long startTime = System.currentTimeMillis();
        iTestMessageServiceClient.sendMessage(message);
        long duration = System.currentTimeMillis() - startTime;

        // Then - should complete very quickly (< 500ms) since it doesn't wait
        assertThat(duration).isLessThan(500L);

        // Verify the message was processed asynchronously
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() ->
                        assertThat(messageServiceImpl.getMessageCount()).isEqualTo(1)
                );
    }

    @Test
    @DisplayName("Should send multiple fire-and-forget messages")
    void shouldSendMultipleFireAndForgetMessages() {
        // Given
        int messageCount = 10;

        // When
        for (int i = 0; i < messageCount; i++) {
            TestMessage message = new TestMessage("Message " + i, System.currentTimeMillis());
            iTestMessageServiceClient.sendMessage(message);
        }

        // Then - verify all messages were processed
        // Increased timeout for full test suite execution where queues may be busy
        await().atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() ->
                        assertThat(messageServiceImpl.getMessageCount()).isEqualTo(messageCount)
                );
    }

    @Test
    @DisplayName("Should send fire-and-forget messages rapidly without blocking")
    void shouldSendFireAndForgetMessagesRapidlyWithoutBlocking() {
        // Given
        int messageCount = 50;
        long startTime = System.currentTimeMillis();

        // When - send many messages rapidly
        for (int i = 0; i < messageCount; i++) {
            TestMessage message = new TestMessage("Rapid message " + i, System.currentTimeMillis());
            iTestMessageServiceClient.sendMessage(message);
        }
        long duration = System.currentTimeMillis() - startTime;

        // Then - should complete quickly even with many messages
        assertThat(duration).isLessThan(5000L);

        // Verify all messages were eventually processed
        // Increased timeout for full test suite execution where queues may be busy
        await().atMost(Duration.ofSeconds(45))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() ->
                        assertThat(messageServiceImpl.getMessageCount()).isEqualTo(messageCount)
                );
    }

    @Test
    @DisplayName("Should handle synchronous echo method correctly")
    void shouldHandleSynchronousEchoMethodCorrectly() {
        // Given
        String testMessage = "Echo test message";

        // When
        String response = iTestMessageServiceClient.echo(testMessage);

        // Then - should receive immediate response
        assertThat(response).isNotNull()
                .isEqualTo("Echo: " + testMessage);
    }

    @Test
    @DisplayName("Should handle synchronous processMessage method correctly")
    void shouldHandleSynchronousProcessMessageCorrectly() {
        // Given
        long timestamp = System.currentTimeMillis();
        TestMessage message = new TestMessage("Sync process test", timestamp);

        // When
        String response = iTestMessageServiceClient.processMessage(message);

        // Then - should receive immediate response
        assertThat(response).isNotNull()
                .contains("Processed: Sync process test")
                .contains(String.valueOf(timestamp));
    }

    @Test
    @DisplayName("Should handle long-running operation with timeout")
    void shouldHandleLongRunningOperationWithTimeout() {
        // Given
        String taskId = "long-task-1";

        // When
        long startTime = System.currentTimeMillis();
        String response = iTestMessageServiceClient.longRunningOperation(taskId);
        long duration = System.currentTimeMillis() - startTime;

        // Then - should wait for the full operation to complete
        assertThat(response).isNotNull()
                .isEqualTo("Completed task: " + taskId);
        // Operation takes 2 seconds, so duration should be at least 2 seconds
        assertThat(duration).isGreaterThanOrEqualTo(1800L);
    }

    @Test
    @DisplayName("Should distinguish between sync and async methods")
    void shouldDistinguishBetweenSyncAndAsyncMethods() {
        // Given
        TestMessage asyncMessage = new TestMessage("Async", System.currentTimeMillis());
        TestMessage syncMessage = new TestMessage("Sync", System.currentTimeMillis());

        // When - send async message (fire-and-forget)
        long asyncStart = System.currentTimeMillis();
        iTestMessageServiceClient.sendMessage(asyncMessage);
        long asyncDuration = System.currentTimeMillis() - asyncStart;

        // When - send sync message
        String syncResponse = iTestMessageServiceClient.processMessage(syncMessage);

        // Then - async should be much faster
        assertThat(asyncDuration).isLessThan(100L);
        assertThat(syncResponse).isNotNull()
                .contains("Processed: Sync");
    }

    @Test
    @DisplayName("Should handle concurrent fire-and-forget and synchronous calls")
    void shouldHandleConcurrentFireAndForgetAndSynchronousCalls() {
        // Given
        int asyncCount = 20;
        int syncCount = 10;

        // When - mix async and sync calls
        for (int i = 0; i < asyncCount; i++) {
            iTestMessageServiceClient.sendMessage(
                    new TestMessage("Async " + i, System.currentTimeMillis())
            );
        }

        for (int i = 0; i < syncCount; i++) {
            String response = iTestMessageServiceClient.echo("Sync " + i);
            assertThat(response).isNotNull();
        }

        // Then - verify all async messages were processed
        // Increased timeout for full test suite execution where queues may be busy
        await().atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() ->
                        assertThat(messageServiceImpl.getMessageCount()).isEqualTo(asyncCount)
                );
    }

    @Test
    @DisplayName("Should not throw exception for fire-and-forget even if processing fails")
    void shouldNotThrowExceptionForFireAndForgetEvenIfProcessingFails() {
        // Given - invalid message (blank content violates @NotBlank)
        // Note: Validation happens on server side for fire-and-forget
        TestMessage invalidMessage = new TestMessage("", System.currentTimeMillis());

        // When/Then - should not throw exception even if server-side processing fails
        // because it's fire-and-forget
        assertDoesNotThrow(() -> iTestMessageServiceClient.sendMessage(invalidMessage));
    }
}

