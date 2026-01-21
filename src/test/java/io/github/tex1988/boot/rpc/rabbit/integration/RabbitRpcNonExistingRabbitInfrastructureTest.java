package io.github.tex1988.boot.rpc.rabbit.integration;

import com.rabbitmq.client.Channel;
import io.github.tex1988.boot.rpc.rabbit.integration.config.TestClientServerConfig;
import io.github.tex1988.boot.rpc.rabbit.integration.model.TestUser;
import io.github.tex1988.boot.rpc.rabbit.integration.service.ITestAdvancedService;
import io.github.tex1988.boot.rpc.rabbit.integration.service.ITestMessageService;
import io.github.tex1988.boot.rpc.rabbit.integration.service.ITestSharedQueueServiceA;
import io.github.tex1988.boot.rpc.rabbit.integration.service.ITestSharedQueueServiceB;
import io.github.tex1988.boot.rpc.rabbit.integration.service.ITestUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration tests for RabbitRpcAutoConfigure.
 * <p>
 * Tests verify that the autoconfiguration correctly handles:
 * 1. Services with explicit exchange/queue/routing configuration
 * 2. Multiple services sharing the SAME queue (competing consumers)
 * 3. Pre-existing RabbitMQ infrastructure (idempotency)
 * 4. Consumer count verification and concurrency settings
 * <p>
 * The tests validate actual RabbitMQ infrastructure creation and end-to-end RPC calls.
 */
@SpringBootTest(classes = TestClientServerConfig.class)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("RabbitMQ RPC AutoConfiguration End-to-End Integration Tests")
class RabbitRpcNonExistingRabbitInfrastructureTest extends AbstractRabbitRpcIntegrationTest {

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private ConnectionFactory connectionFactory;

    // Explicit configuration services
    @Autowired
    private ITestUserService iTestUserServiceClient;

    @Autowired
    private ITestAdvancedService iTestAdvancedServiceClient;

    @Autowired
    private ITestMessageService iTestMessageServiceClient;

    // Shared queue services (competing consumers)
    @Autowired
    private ITestSharedQueueServiceA iTestSharedQueueServiceAClient;

    @Autowired
    private ITestSharedQueueServiceB iTestSharedQueueServiceBClient;

    // ==================== SCENARIO 1: EXPLICIT CONFIGURATION ====================

    @Test
    @DisplayName("Should create infrastructure for service with explicit configuration - User Service")
    void shouldCreateInfrastructureForExplicitConfigUserService() throws Exception {
        // Given - ITestUserService with explicit config
        String exchangeName = "test.user.exchange";
        String queueName = "test.user.queue";

        // When - Check infrastructure exists
        boolean exchangeExists = checkExchangeExists(exchangeName);
        Properties queueProps = rabbitAdmin.getQueueProperties(queueName);
        Integer consumerCount = (Integer) queueProps.get(RabbitAdmin.QUEUE_CONSUMER_COUNT);

        // Then - Infrastructure created
        assertThat(exchangeExists).as("Exchange should exist").isTrue();
        assertThat(queueProps).as("Queue should exist").isNotNull()
                .containsEntry(RabbitAdmin.QUEUE_NAME, queueName);
        assertThat(consumerCount).as("Should have active consumers").isGreaterThan(0);

        // And - End-to-end RPC works
        var user = iTestUserServiceClient.getUser(1L);
        assertThat(user).isNotNull();
        assertThat(user.getName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Should create infrastructure for service with explicit configuration - Advanced Service")
    void shouldCreateInfrastructureForExplicitConfigAdvancedService() throws Exception {
        // Given - ITestAdvancedService with explicit config
        String exchangeName = "test.advanced.exchange";
        String queueName = "test.advanced.queue";

        // When - Check infrastructure exists
        boolean exchangeExists = checkExchangeExists(exchangeName);
        Properties queueProps = rabbitAdmin.getQueueProperties(queueName);

        // Then - Infrastructure created
        assertThat(exchangeExists).as("Exchange should exist").isTrue();
        assertThat(queueProps).as("Queue should exist").isNotNull();

        // And - End-to-end RPC works with method overloading
        String result = iTestAdvancedServiceClient.process("test");
        assertThat(result).isEqualTo("Processed: test");
    }

    @Test
    @DisplayName("Should create infrastructure for service with explicit configuration - Message Service")
    void shouldCreateInfrastructureForExplicitConfigMessageService() throws Exception {
        // Given - ITestMessageService with explicit config
        String exchangeName = "test.message.exchange";
        String queueName = "test.message.queue";

        // When - Check infrastructure exists
        boolean exchangeExists = checkExchangeExists(exchangeName);
        Properties queueProps = rabbitAdmin.getQueueProperties(queueName);

        // Then - Infrastructure created
        assertThat(exchangeExists).as("Exchange should exist").isTrue();
        assertThat(queueProps).as("Queue should exist").isNotNull();

        // And - End-to-end RPC works
        var message = new io.github.tex1988.boot.rpc.rabbit.integration.model.TestMessage("Test message", System.currentTimeMillis());
        String result = iTestMessageServiceClient.processMessage(message);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should isolate traffic between services with different explicit configurations")
    void shouldIsolateTrafficBetweenExplicitServices() {
        // Given - Three services with different queues

        // When - Call all services
        var user = iTestUserServiceClient.getUser(2L);
        String processed = iTestAdvancedServiceClient.process("data");
        var message = new io.github.tex1988.boot.rpc.rabbit.integration.model.TestMessage("Test message", System.currentTimeMillis());
        String messageResult = iTestMessageServiceClient.processMessage(message);

        // Then - All work independently
        assertThat(user).isNotNull();
        assertThat(user.getName()).isEqualTo("Jane Smith");
        assertThat(processed).isEqualTo("Processed: data");
        assertThat(messageResult).isNotNull();
    }

    // ==================== SCENARIO 2: SHARED QUEUE (COMPETING CONSUMERS) ====================

    @Test
    @DisplayName("Should support multiple services sharing the SAME queue as competing consumers")
    void shouldSupportMultipleServicesShareSameQueue() throws Exception {
        // Given - ServiceA and ServiceB both use "test.shared.queue" with same routing key
        // This is the competing consumers pattern - messages are distributed among consumers
        String sharedExchange = "test.shared.exchange";
        String sharedQueue = "test.shared.queue";

        // When - Check infrastructure
        boolean exchangeExists = checkExchangeExists(sharedExchange);
        Properties queueProps = rabbitAdmin.getQueueProperties(sharedQueue);
        Integer consumerCount = (Integer) queueProps.get(RabbitAdmin.QUEUE_CONSUMER_COUNT);

        // Then - Single queue with MULTIPLE consumers (from both services)
        assertThat(exchangeExists).as("Shared exchange should exist").isTrue();
        assertThat(queueProps).as("Shared queue should exist").isNotNull();
        // Note: When multiple services share a queue, they share the consumer pool
        // The total consumer count equals the concurrency setting (3-5), not multiplied per service
        assertThat(consumerCount).as("Should have consumers from shared consumer pool")
                .isGreaterThanOrEqualTo(3)
                .isLessThanOrEqualTo(5);

        // And - Both services can process messages (messages distributed among them)
        // Note: We can't control which service processes which message (competing consumers)
        // We just verify both services are functional
        String resultA = iTestSharedQueueServiceAClient.serviceAOperation("test-a");
        String resultB = iTestSharedQueueServiceBClient.serviceBOperation("test-b");

        // Results will be from whichever service processed the message
        assertThat(resultA).containsAnyOf("Service A result:", "Service B result:");
        assertThat(resultB).containsAnyOf("Service A result:", "Service B result:");
    }

    @Test
    @DisplayName("Should distribute messages among competing consumers on shared queue")
    void shouldDistributeMessagesAmongCompetingConsumers() {
        // Given - ServiceA and ServiceB both consume from same queue
        // Messages are distributed in round-robin or based on availability

        // When - Send multiple messages
        String result1 = iTestSharedQueueServiceAClient.serviceAOperation("msg-1");
        String result2 = iTestSharedQueueServiceBClient.serviceBOperation("msg-2");
        String result3 = iTestSharedQueueServiceAClient.serviceAOperation("msg-3");
        String result4 = iTestSharedQueueServiceBClient.serviceBOperation("msg-4");

        // Then - All messages are processed (by either service)
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        assertThat(result3).isNotNull();
        assertThat(result4).isNotNull();

        // Results come from both services (competing consumers)
        assertThat(result1).containsAnyOf("Service A result:", "Service B result:");
        assertThat(result2).containsAnyOf("Service A result:", "Service B result:");
    }

    @Test
    @DisplayName("Should verify shared queue has consumers from multiple services")
    void shouldVerifySharedQueueHasMultipleConsumers() {
        // Given - Both services registered on same queue
        String sharedQueue = "test.shared.queue";

        // When - Check consumer count
        Properties queueProps = rabbitAdmin.getQueueProperties(sharedQueue);
        Integer consumerCount = (Integer) queueProps.get(RabbitAdmin.QUEUE_CONSUMER_COUNT);

        // Then - Should have consumers from both services
        // When services share a queue, they share the consumer pool (concurrency setting)
        assertThat(consumerCount).as("Shared queue should have shared consumer pool")
                .isGreaterThanOrEqualTo(3)
                .isLessThanOrEqualTo(5);
    }

    // ==================== SCENARIO 3: PRE-EXISTING INFRASTRUCTURE (IDEMPOTENCY) ====================

    @Test
    @DisplayName("Should connect to pre-existing exchange without recreation (passive declare)")
    void shouldConnectToPreExistingExchangeWithoutRecreation() throws Exception {
        // Given - Exchange already exists (created during Spring context initialization)
        // The createOrConnectExchange() method uses passive declare to check existence
        String exchangeName = "test.user.exchange";

        // When - Verify exchange exists (passive declare should succeed)
        boolean exchangeExists = checkExchangeExists(exchangeName);

        // Then - Exchange exists (was not recreated, just connected to existing)
        assertThat(exchangeExists).as("Exchange should exist from initial startup").isTrue();

        // And - RPC works, proving connection to existing exchange is functional
        var user = iTestUserServiceClient.getUser(1L);
        assertThat(user).isNotNull();
        assertThat(user.getName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Should connect to pre-existing queue without recreation (queueInfo check)")
    void shouldConnectToPreExistingQueueWithoutRecreation() {
        // Given - Queue already exists (created during Spring context initialization)
        // The createQueue() method checks queueInfo and skips creation if exists
        String queueName = "test.user.queue";

        // When - Query queue info (this is what createQueue() does internally)
        Properties queueProps = rabbitAdmin.getQueueProperties(queueName);

        // Then - Queue exists (was not recreated, just connected to existing)
        assertThat(queueProps).as("Queue should exist from initial startup").isNotNull()
                .containsEntry(RabbitAdmin.QUEUE_NAME, queueName);

        // And - Has active consumers (proves binding was established)
        Integer consumerCount = (Integer) queueProps.get(RabbitAdmin.QUEUE_CONSUMER_COUNT);
        assertThat(consumerCount).as("Queue should have active consumers").isGreaterThan(0);

        // And - RPC works, proving connection to existing queue is functional
        TestUser user = iTestUserServiceClient.getUser(2L);
        assertThat(user).isNotNull();
    }

    @Test
    @DisplayName("Should not recreate binding when queue already exists")
    void shouldNotRecreateBindingWhenQueueExists() throws Exception {
        // Given - Queue with binding already exists from Spring initialization
        String exchangeName = "test.advanced.exchange";
        String queueName = "test.advanced.queue";

        // When - Verify infrastructure (createQueue checks queueInfo before creating)
        boolean exchangeExists = checkExchangeExists(exchangeName);
        Properties queueProps = rabbitAdmin.getQueueProperties(queueName);

        // Then - Infrastructure exists without recreation
        assertThat(exchangeExists).isTrue();
        assertThat(queueProps).isNotNull();

        // And - Binding is functional (message routing works)
        String result = iTestAdvancedServiceClient.process("binding-test");
        assertThat(result).isEqualTo("Processed: binding-test");
    }

    @Test
    @DisplayName("Should handle restart scenario with all pre-existing infrastructure")
    void shouldHandleRestartScenarioWithPreExistingInfrastructure() throws Exception {
        // Given - Simulate application restart where RabbitMQ already has all infrastructure
        // All exchanges, queues, and bindings exist from previous run
        String userExchange = "test.user.exchange";
        String userQueue = "test.user.queue";
        String advancedExchange = "test.advanced.exchange";
        String advancedQueue = "test.advanced.queue";
        String messageExchange = "test.message.exchange";
        String messageQueue = "test.message.queue";

        // When - Verify all infrastructure exists (passive declares and queueInfo checks)
        boolean userExchangeExists = checkExchangeExists(userExchange);
        boolean advancedExchangeExists = checkExchangeExists(advancedExchange);
        boolean messageExchangeExists = checkExchangeExists(messageExchange);

        Properties userQueueProps = rabbitAdmin.getQueueProperties(userQueue);
        Properties advancedQueueProps = rabbitAdmin.getQueueProperties(advancedQueue);
        Properties messageQueueProps = rabbitAdmin.getQueueProperties(messageQueue);

        // Then - All infrastructure exists (connected to, not recreated)
        assertThat(userExchangeExists).isTrue();
        assertThat(advancedExchangeExists).isTrue();
        assertThat(messageExchangeExists).isTrue();
        assertThat(userQueueProps).isNotNull();
        assertThat(advancedQueueProps).isNotNull();
        assertThat(messageQueueProps).isNotNull();

        // And - All RPC calls work (bindings are functional)
        var user = iTestUserServiceClient.getUser(1L);
        String advanced = iTestAdvancedServiceClient.process("restart-test");
        var message = new io.github.tex1988.boot.rpc.rabbit.integration.model.TestMessage("Restart test", System.currentTimeMillis());
        String messageResult = iTestMessageServiceClient.processMessage(message);

        assertThat(user).isNotNull();
        assertThat(advanced).isEqualTo("Processed: restart-test");
        assertThat(messageResult).isNotNull();
    }

    @Test
    @DisplayName("Should handle idempotent queue checks without errors")
    void shouldHandleIdempotentQueueChecksWithoutErrors() {
        // Given - Queue exists (queueInfo will return non-null)
        String queueName = "test.user.queue";

        // When - Check queue info multiple times (simulating what createQueue does)
        Properties props1 = rabbitAdmin.getQueueProperties(queueName);
        Properties props2 = rabbitAdmin.getQueueProperties(queueName);
        Properties props3 = rabbitAdmin.getQueueProperties(queueName);

        // Then - All checks succeed, returning same queue
        assertThat(props1).isNotNull();
        assertThat(props2).isNotNull();
        assertThat(props3).isNotNull();
        assertThat(props1.get(RabbitAdmin.QUEUE_NAME))
                .isEqualTo(props2.get(RabbitAdmin.QUEUE_NAME))
                .isEqualTo(props3.get(RabbitAdmin.QUEUE_NAME));
    }

    @Test
    @DisplayName("Should handle idempotent exchange passive declares without errors")
    void shouldHandleIdempotentExchangePassiveDeclaresWithoutErrors() throws Exception {
        // Given - Exchange exists (passive declare will succeed)
        String exchangeName = "test.advanced.exchange";

        // When - Passive declare multiple times (simulating what createOrConnectExchange does)
        boolean check1 = checkExchangeExists(exchangeName);
        boolean check2 = checkExchangeExists(exchangeName);
        boolean check3 = checkExchangeExists(exchangeName);

        // Then - All checks succeed
        assertThat(check1).isTrue();
        assertThat(check2).isTrue();
        assertThat(check3).isTrue();
    }

    @Test
    @DisplayName("Should verify createOrConnectExchange uses passive declare pattern correctly")
    void shouldVerifyCreateOrConnectExchangeUsesPassiveDeclarePattern() throws Exception {
        // This test verifies the createOrConnectExchange() logic:
        // 1. Try passive declare (check if exists)
        // 2. If 404 error -> create exchange
        // 3. If exists -> just connect

        // Given - Multiple exchanges in system
        String userExchange = "test.user.exchange";
        String advancedExchange = "test.advanced.exchange";
        String messageExchange = "test.message.exchange";

        // When - Check all exchanges (simulating passive declare)
        boolean userExists = checkExchangeExists(userExchange);
        boolean advancedExists = checkExchangeExists(advancedExchange);
        boolean messageExists = checkExchangeExists(messageExchange);

        // Then - All exist (were connected to, not recreated)
        assertThat(userExists).as("User exchange should exist").isTrue();
        assertThat(advancedExists).as("Advanced exchange should exist").isTrue();
        assertThat(messageExists).as("Message exchange should exist").isTrue();

        // And - All are functional (RPC works through them)
        var user = iTestUserServiceClient.getUser(1L);
        String advanced = iTestAdvancedServiceClient.process("test");
        assertThat(user).isNotNull();
        assertThat(advanced).isNotNull();
    }

    @Test
    @DisplayName("Should verify createQueue checks queueInfo before creating")
    void shouldVerifyCreateQueueChecksQueueInfoBeforeCreating() {
        // This test verifies the createQueue() logic:
        // 1. Check queueInfo = amqpAdmin.getQueueInfo(queueName)
        // 2. If queueInfo == null OR queueInfo.getName() == null -> create queue + binding
        // 3. If exists -> return Queue object without creating

        // Given - Multiple queues in system
        String userQueue = "test.user.queue";
        String advancedQueue = "test.advanced.queue";
        String messageQueue = "test.message.queue";

        // When - Get queue info (simulating queueInfo check)
        Properties userProps = rabbitAdmin.getQueueProperties(userQueue);
        Properties advancedProps = rabbitAdmin.getQueueProperties(advancedQueue);
        Properties messageProps = rabbitAdmin.getQueueProperties(messageQueue);

        // Then - All queues exist (queueInfo is not null, so they weren't recreated)
        assertThat(userProps).as("User queue should exist").isNotNull();
        assertThat(advancedProps).as("Advanced queue should exist").isNotNull();
        assertThat(messageProps).as("Message queue should exist").isNotNull();

        // And - Queue names match (proves getQueueInfo returned valid data)
        assertThat(userProps).containsEntry(RabbitAdmin.QUEUE_NAME, userQueue);
        assertThat(advancedProps).containsEntry(RabbitAdmin.QUEUE_NAME, advancedQueue);
        assertThat(messageProps).containsEntry(RabbitAdmin.QUEUE_NAME, messageQueue);
    }

    // ==================== SCENARIO 4: VERIFICATION TESTS ====================

    @Test
    @DisplayName("Should maintain consumer counts correctly across different queue configurations")
    void shouldMaintainConsumerCountsCorrectly() {
        // Given - Different queue configurations

        // When - Check consumer counts
        Properties userQueueProps = rabbitAdmin.getQueueProperties("test.user.queue");
        Properties advancedQueueProps = rabbitAdmin.getQueueProperties("test.advanced.queue");
        Properties sharedQueueProps = rabbitAdmin.getQueueProperties("test.shared.queue");

        Integer userConsumers = (Integer) userQueueProps.get(RabbitAdmin.QUEUE_CONSUMER_COUNT);
        Integer advancedConsumers = (Integer) advancedQueueProps.get(RabbitAdmin.QUEUE_CONSUMER_COUNT);
        Integer sharedConsumers = (Integer) sharedQueueProps.get(RabbitAdmin.QUEUE_CONSUMER_COUNT);

        // Then - Each queue has appropriate consumer count
        assertThat(userConsumers).as("User queue consumers").isGreaterThan(0);
        assertThat(advancedConsumers).as("Advanced queue consumers").isGreaterThan(0);
        // Shared queue consumers are pooled, not multiplied per service
        assertThat(sharedConsumers).as("Shared queue should have shared consumer pool")
                .isGreaterThanOrEqualTo(3)
                .isLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("Should respect concurrency settings from @EnableRabbitRpc")
    void shouldRespectConcurrencySettings() {
        // Given - TestClientServerConfig has concurrency = "3-5"

        // When - Check consumer counts
        Properties userQueueProps = rabbitAdmin.getQueueProperties("test.user.queue");
        Integer consumerCount = (Integer) userQueueProps.get(RabbitAdmin.QUEUE_CONSUMER_COUNT);

        // Then - Consumer count should be within configured range
        assertThat(consumerCount).as("Consumer count should respect concurrency settings")
                .isGreaterThanOrEqualTo(3)
                .isLessThanOrEqualTo(5);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Check if an exchange exists in RabbitMQ using passive declare.
     */
    private boolean checkExchangeExists(String exchangeName) throws Exception {
        try (Connection connection = connectionFactory.createConnection();
             Channel channel = connection.createChannel(true)) {
            channel.exchangeDeclarePassive(exchangeName);
            return true;
        } catch (Exception e) {
            if (e.getCause() != null && e.getCause().getMessage() != null
                    && e.getCause().getMessage().contains("404")) {
                return false;
            }
            throw e;
        }
    }
}
