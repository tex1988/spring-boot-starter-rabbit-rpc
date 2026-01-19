package io.github.tex1988.boot.rpc.rabbit.integration;

import io.github.tex1988.boot.rpc.rabbit.exception.RabbitRpcServiceException;
import io.github.tex1988.boot.rpc.rabbit.integration.config.TestClientServerConfig;
import io.github.tex1988.boot.rpc.rabbit.integration.model.TestUser;
import io.github.tex1988.boot.rpc.rabbit.integration.service.ITestUserService;
import io.github.tex1988.boot.rpc.rabbit.integration.service.impl.TestUserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration tests for basic RabbitMQ RPC operations.
 * Tests basic CRUD operations and synchronous RPC calls.
 */
@SpringBootTest(classes = TestClientServerConfig.class)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Basic RabbitMQ RPC Integration Tests")
class RabbitRpcBasicIntegrationTest extends AbstractRabbitRpcIntegrationTest {

    @Autowired
    private ITestUserService iTestUserServiceClient;

    @Autowired
    private TestUserServiceImpl userServiceImpl;

    @BeforeEach
    void setUp() {
        // Reset notification counter before each test
        userServiceImpl.resetNotificationCount();
        // Reset user store to initial state before each test
        userServiceImpl.resetUserStore();
    }

    @Test
    @DisplayName("Should get existing user by ID")
    void shouldGetExistingUser() {
        // Given
        Long userId = 1L;

        // When
        TestUser user = iTestUserServiceClient.getUser(userId);

        // Then
        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo(userId);
        assertThat(user.getName()).isEqualTo("John Doe");
        assertThat(user.getEmail()).isEqualTo("john@example.com");
        assertThat(user.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        Long nonExistentUserId = 999L;

        // When/Then
        assertThatThrownBy(() -> iTestUserServiceClient.getUser(nonExistentUserId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Should create new user")
    void shouldCreateNewUser() {
        // Given
        TestUser newUser = new TestUser(200L, "Alice Johnson", "alice@example.com", true);

        // When
        TestUser createdUser = iTestUserServiceClient.createUser(newUser);

        // Then
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getId()).isEqualTo(200L);
        assertThat(createdUser.getName()).isEqualTo("Alice Johnson");

        // Verify it was actually stored
        TestUser retrievedUser = iTestUserServiceClient.getUser(200L);
        assertThat(retrievedUser).isEqualTo(createdUser);
    }

    @Test
    @DisplayName("Should update existing user")
    void shouldUpdateExistingUser() {
        // Given
        Long userId = 2L;
        TestUser updatedUser = new TestUser(userId, "Jane Updated", "jane.updated@example.com", false);

        // When
        TestUser result = iTestUserServiceClient.updateUser(updatedUser);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Jane Updated");
        assertThat(result.getEmail()).isEqualTo("jane.updated@example.com");
        assertThat(result.isActive()).isFalse();

        // Verify the update persisted
        TestUser retrievedUser = iTestUserServiceClient.getUser(userId);
        assertThat(retrievedUser.getName()).isEqualTo("Jane Updated");
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent user")
    void shouldThrowExceptionWhenUpdatingNonExistentUser() {
        // Given
        TestUser nonExistentUser = new TestUser(999L, "Ghost User", "ghost@example.com", true);

        // When/Then
        assertThatThrownBy(() -> iTestUserServiceClient.updateUser(nonExistentUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Should delete existing user synchronously")
    void shouldDeleteExistingUser() {
        // Given
        TestUser userToDelete = new TestUser(210L, "Temp User", "temp@example.com", true);
        iTestUserServiceClient.createUser(userToDelete);

        // When
        iTestUserServiceClient.deleteUser(210L);

        // Then - verify user was deleted
        assertThatThrownBy(() -> iTestUserServiceClient.getUser(210L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent user")
    void shouldThrowExceptionWhenDeletingNonExistentUser() {
        // Given
        Long nonExistentUserId = 999L;

        // When/Then
        assertThatThrownBy(() -> iTestUserServiceClient.deleteUser(nonExistentUserId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Should get all users")
    void shouldGetAllUsers() {
        // When
        List<TestUser> users = iTestUserServiceClient.getAllUsers();

        // Then
        assertNotNull(users);
        assertThat(users).hasSizeGreaterThanOrEqualTo(3);
        assertThat(users).extracting(TestUser::getName)
                .contains("John Doe", "Jane Smith", "Bob Wilson");
    }

    @Test
    @DisplayName("Should handle null return values correctly")
    void shouldHandleNullReturnValues() {
        // This test verifies that null responses are handled correctly
        // by creating and then deleting a user (void return)
        TestUser tempUser = new TestUser(220L, "Null Test", "null@test.com", true);
        iTestUserServiceClient.createUser(tempUser);

        // deleteUser returns void, should complete without exception
        iTestUserServiceClient.deleteUser(220L);

        // Verify deletion was successful
        assertThatThrownBy(() -> iTestUserServiceClient.getUser(220L))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should handle fire-and-forget notification")
    void shouldHandleFireAndForgetNotification() {
        // Given
        Long userId = 1L;
        String message = "Test notification";

        // When
        iTestUserServiceClient.notifyUser(userId, message);

        // Then - wait for async processing
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() ->
                        assertThat(userServiceImpl.getNotificationCount()).isGreaterThan(0)
                );
    }

    @Test
    @DisplayName("Should handle error responses correctly")
    void shouldHandleErrorResponsesCorrectly() {
        // Given
        Long userId = 1L;

        // When/Then
        assertThatThrownBy(() -> iTestUserServiceClient.getUserWithError(userId))
                .isInstanceOf(RabbitRpcServiceException.class)
                .hasMessageContaining("Simulated error");
    }

    @Test
    @DisplayName("Should handle user object with all fields populated")
    void shouldHandleUserObjectWithAllFields() {
        // Given - user with all fields
        TestUser completeUser = new TestUser(230L, "Complete User", "complete@example.com", true);

        // When
        TestUser created = iTestUserServiceClient.createUser(completeUser);

        // Then
        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(230L);
        assertThat(created.getName()).isEqualTo("Complete User");
        assertThat(created.getEmail()).isEqualTo("complete@example.com");
        assertThat(created.isActive()).isTrue();
    }
}


