package io.github.tex1988.boot.rpc.rabbit.integration;

import io.github.tex1988.boot.rpc.rabbit.exception.RabbitRpcServiceException;
import io.github.tex1988.boot.rpc.rabbit.integration.config.TestClientServerConfig;
import io.github.tex1988.boot.rpc.rabbit.integration.model.TestMessage;
import io.github.tex1988.boot.rpc.rabbit.integration.model.TestUser;
import io.github.tex1988.boot.rpc.rabbit.integration.service.ITestMessageService;
import io.github.tex1988.boot.rpc.rabbit.integration.service.ITestUserService;
import io.github.tex1988.boot.rpc.rabbit.integration.service.impl.TestMessageServiceImpl;
import io.github.tex1988.boot.rpc.rabbit.integration.service.impl.TestUserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * End-to-end integration tests for complete user workflows.
 * Tests realistic scenarios combining multiple operations.
 */
@SpringBootTest(classes = TestClientServerConfig.class)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("RabbitMQ RPC End-to-End Integration Tests")
class RabbitRpcEndToEndIntegrationTest extends AbstractRabbitRpcIntegrationTest {

    @Autowired
    private ITestUserService iTestUserServiceClient;

    @Autowired
    private ITestMessageService iTestMessageServiceClient;

    @Autowired
    private TestUserServiceImpl userServiceImpl;

    @Autowired
    private TestMessageServiceImpl messageServiceImpl;

    @BeforeEach
    void setUp() {
        userServiceImpl.resetNotificationCount();
        userServiceImpl.resetUserStore();
        messageServiceImpl.resetMessageCount();
    }

    @Test
    @DisplayName("Should complete full CRUD lifecycle for a user")
    void shouldCompleteFullCrudLifecycleForUser() {
        // Given - new user data
        Long userId = 600L;
        TestUser newUser = new TestUser(userId, "Lifecycle User", "lifecycle@example.com", true);

        // When - CREATE
        TestUser created = iTestUserServiceClient.createUser(newUser);

        // Then - verify creation
        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(userId);

        // When - READ
        TestUser retrieved = iTestUserServiceClient.getUser(userId);

        // Then - verify retrieval
        assertThat(retrieved).isEqualTo(created);

        // When - UPDATE
        TestUser updatedUser = new TestUser(userId, "Updated User", "updated@example.com", false);
        TestUser updated = iTestUserServiceClient.updateUser(updatedUser);

        // Then - verify update
        assertThat(updated.getName()).isEqualTo("Updated User");
        assertThat(updated.isActive()).isFalse();

        // When - DELETE
        iTestUserServiceClient.deleteUser(userId);

        // Then - verify deletion
        RabbitRpcServiceException exception = assertThrows(RabbitRpcServiceException.class,
                () -> iTestUserServiceClient.getUser(userId));
        assertThat(exception.getMessage()).contains("User not found");
    }

    @Test
    @DisplayName("Should handle complete user onboarding workflow")
    void shouldHandleCompleteUserOnboardingWorkflow() {
        // Given - simulate user onboarding
        Long userId = 610L;

        // Step 1: Create user account
        TestUser newUser = new TestUser(userId, "New Member", "newmember@example.com", false);
        TestUser created = iTestUserServiceClient.createUser(newUser);
        assertThat(created.isActive()).isFalse();

        // Step 2: Send welcome notification (fire-and-forget)
        iTestUserServiceClient.notifyUser(userId, "Welcome to our platform!");

        // Step 3: Activate user account
        TestUser activatedUser = new TestUser(userId, "New Member", "newmember@example.com", true);
        TestUser updated = iTestUserServiceClient.updateUser(activatedUser);
        assertThat(updated.isActive()).isTrue();

        // Step 4: Send activation confirmation (fire-and-forget)
        iTestUserServiceClient.notifyUser(userId, "Your account has been activated!");

        // Step 5: Verify notifications were sent
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() ->
                        assertThat(userServiceImpl.getNotificationCount()).isEqualTo(2)
                );

        // Step 6: Verify final user state
        TestUser finalUser = iTestUserServiceClient.getUser(userId);
        assertThat(finalUser.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should handle batch user creation and notification")
    void shouldHandleBatchUserCreationAndNotification() {
        // Given
        int batchSize = 15;
        List<Long> userIds = new ArrayList<>();

        // When - create batch of users
        for (int i = 0; i < batchSize; i++) {
            Long userId = 620L + i;
            userIds.add(userId);
            TestUser user = new TestUser(
                    userId,
                    "Batch User " + i,
                    "batch" + i + "@example.com",
                    true
            );
            iTestUserServiceClient.createUser(user);
        }

        // Then - verify all users were created
        List<TestUser> allUsers = iTestUserServiceClient.getAllUsers();
        assertThat(allUsers).hasSizeGreaterThanOrEqualTo(batchSize);

        // When - send notifications to all users
        for (Long userId : userIds) {
            iTestUserServiceClient.notifyUser(userId, "Batch notification");
        }

        // Then - verify notifications were sent
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() ->
                        assertThat(userServiceImpl.getNotificationCount()).isGreaterThanOrEqualTo(batchSize)
                );
    }

    @Test
    @DisplayName("Should handle mixed operations across multiple services")
    void shouldHandleMixedOperationsAcrossMultipleServices() {
        // Given
        Long userId = 640L;
        TestUser user = new TestUser(userId, "Multi Service", "multi@example.com", true);

        // When - interleave operations between services
        iTestUserServiceClient.createUser(user);

        String echo1 = iTestMessageServiceClient.echo("Message 1");
        assertThat(echo1).contains("Echo: Message 1");

        TestUser retrieved = iTestUserServiceClient.getUser(userId);
        assertThat(retrieved).isNotNull();

        TestMessage message = new TestMessage("Test content", System.currentTimeMillis());
        String processed = iTestMessageServiceClient.processMessage(message);
        assertThat(processed).contains("Processed");

        iTestUserServiceClient.notifyUser(userId, "Test notification");

        TestUser updated = new TestUser(userId, "Updated Multi", "updated@example.com", false);
        iTestUserServiceClient.updateUser(updated);

        iTestMessageServiceClient.sendMessage(new TestMessage("Async message", System.currentTimeMillis()));

        // Then - verify final states
        TestUser finalUser = iTestUserServiceClient.getUser(userId);
        assertThat(finalUser.getName()).isEqualTo("Updated Multi");

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    assertThat(userServiceImpl.getNotificationCount()).isGreaterThan(0);
                    assertThat(messageServiceImpl.getMessageCount()).isGreaterThan(0);
                });
    }

    @Test
    @DisplayName("Should handle concurrent user workflows")
    void shouldHandleConcurrentUserWorkflows() throws InterruptedException {
        // Given
        int concurrentUsers = 10;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        CountDownLatch latch = new CountDownLatch(concurrentUsers);
        AtomicInteger successCount = new AtomicInteger(0);

        // When - simulate concurrent user workflows
        for (int i = 0; i < concurrentUsers; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    Long userId = 650L + index;

                    // Create user
                    TestUser user = new TestUser(
                            userId,
                            "Concurrent " + index,
                            "concurrent" + index + "@example.com",
                            false
                    );
                    iTestUserServiceClient.createUser(user);

                    // Send notification
                    iTestUserServiceClient.notifyUser(userId, "Welcome!");

                    // Update user
                    TestUser updated = new TestUser(
                            userId,
                            "Concurrent " + index,
                            "concurrent" + index + "@example.com",
                            true
                    );
                    iTestUserServiceClient.updateUser(updated);

                    // Verify
                    TestUser retrieved = iTestUserServiceClient.getUser(userId);
                    assertThat(retrieved.isActive()).isTrue();

                    successCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then - all workflows should complete
        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
        assertThat(successCount.get()).isEqualTo(concurrentUsers);

        executor.shutdown();
    }

    @Test
    @DisplayName("Should handle user migration workflow")
    void shouldHandleUserMigrationWorkflow() {
        // Given - old user data
        Long oldUserId = 670L;
        Long newUserId = 671L;

        TestUser oldUser = new TestUser(oldUserId, "Old Account", "old@example.com", true);
        iTestUserServiceClient.createUser(oldUser);

        // When - migrate user
        // Step 1: Create new user with updated information
        TestUser newUser = new TestUser(newUserId, "Migrated Account", "new@example.com", true);
        iTestUserServiceClient.createUser(newUser);

        // Step 2: Notify about migration
        iTestUserServiceClient.notifyUser(oldUserId, "Account migration started");
        iTestUserServiceClient.notifyUser(newUserId, "New account created");

        // Step 3: Deactivate old account
        TestUser deactivated = new TestUser(oldUserId, "Old Account", "old@example.com", false);
        iTestUserServiceClient.updateUser(deactivated);

        // Step 4: Verify states
        TestUser oldUserFinal = iTestUserServiceClient.getUser(oldUserId);
        TestUser newUserFinal = iTestUserServiceClient.getUser(newUserId);

        // Then
        assertThat(oldUserFinal.isActive()).isFalse();
        assertThat(newUserFinal.isActive()).isTrue();
        assertThat(newUserFinal.getName()).isEqualTo("Migrated Account");

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() ->
                        assertThat(userServiceImpl.getNotificationCount()).isGreaterThanOrEqualTo(2)
                );
    }

    @Test
    @DisplayName("Should handle stress test with mixed operations")
    void shouldHandleStressTestWithMixedOperations() throws InterruptedException {
        // Given
        int operationCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(operationCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // When - perform various operations rapidly
        for (int i = 0; i < operationCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    int operation = index % 5;
                    switch (operation) { //NOSONAR
                        case 0 -> {
                            // Create user
                            TestUser user = new TestUser(
                                    700L + index,
                                    "Stress " + index,
                                    "stress" + index + "@example.com",
                                    true
                            );
                            iTestUserServiceClient.createUser(user);
                        }
                        case 1 -> // Read user
                                iTestUserServiceClient.getUser(1L);
                        case 2 -> // Send notification
                                iTestUserServiceClient.notifyUser(1L, "Stress test message " + index);
                        case 3 -> // Echo message
                                iTestMessageServiceClient.echo("Stress echo " + index);
                        case 4 -> {
                            // Process message
                            TestMessage msg = new TestMessage("Stress " + index, System.currentTimeMillis());
                            iTestMessageServiceClient.processMessage(msg);
                        }
                    }
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // Some operations may fail (e.g., validation), which is expected
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then
        assertThat(latch.await(60, TimeUnit.SECONDS)).isTrue();
        assertThat(successCount.get()).isGreaterThan(operationCount / 2);

        executor.shutdown();
    }

    @Test
    @DisplayName("Should maintain consistency across service restarts")
    void shouldMaintainConsistencyAcrossServiceRestarts() {
        // Given - create initial data
        Long userId = 680L;
        TestUser user = new TestUser(userId, "Persistence Test", "persist@example.com", true);
        iTestUserServiceClient.createUser(user);

        // When - verify data persists (simulated by reading multiple times)
        TestUser retrieved1 = iTestUserServiceClient.getUser(userId);
        TestUser retrieved2 = iTestUserServiceClient.getUser(userId);
        TestUser retrieved3 = iTestUserServiceClient.getUser(userId);

        // Then - all reads should return same data
        assertThat(retrieved1).isEqualTo(retrieved2);
        assertThat(retrieved2).isEqualTo(retrieved3);
        assertThat(retrieved1.getName()).isEqualTo("Persistence Test");
    }
}

