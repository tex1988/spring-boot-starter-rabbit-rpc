package io.github.tex1988.boot.rpc.rabbit.integration;

import io.github.tex1988.boot.rpc.rabbit.exception.RabbitRpcServiceException;
import io.github.tex1988.boot.rpc.rabbit.exception.RabbitRpcServiceValidationException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Integration tests for error handling in RabbitMQ RPC.
 * Tests exception propagation, validation errors, and error recovery.
 */
@SpringBootTest(classes = TestClientServerConfig.class)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("RabbitMQ RPC Error Handling Integration Tests")
class RabbitRpcErrorHandlingIntegrationTest extends AbstractRabbitRpcIntegrationTest {

    @Autowired
    private ITestUserService iTestUserServiceClient;

    @Autowired
    private TestUserServiceImpl userServiceImpl;

    @BeforeEach
    void setUp() {
        // Reset user store to initial state before each test
        userServiceImpl.resetUserStore();
    }

    @Test
    @DisplayName("Should propagate RuntimeException from server to client")
    void shouldPropagateRuntimeExceptionFromServerToClient() {
        // Given
        Long nonExistentUserId = 999L;

        // When/Then
        assertThatThrownBy(() -> iTestUserServiceClient.getUser(nonExistentUserId))
                .isInstanceOf(RabbitRpcServiceException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Should propagate IllegalStateException from server to client")
    void shouldPropagateIllegalStateExceptionFromServerToClient() {
        // Given
        Long userId = 1L;

        // When/Then
        assertThatThrownBy(() -> iTestUserServiceClient.getUserWithError(userId))
                .isInstanceOf(RabbitRpcServiceException.class)
                .hasMessageContaining("Simulated error");
    }

    @Test
    @DisplayName("Should handle validation errors on parameters")
    void shouldHandleValidationErrorsOnParameters() {
        // When/Then - null parameter
        assertThatThrownBy(() -> iTestUserServiceClient.getUser(null))
                .isInstanceOf(RabbitRpcServiceValidationException.class);
    }

    @Test
    @DisplayName("Should handle validation errors on object fields")
    void shouldHandleValidationErrorsOnObjectFields() {
        // Given - invalid user object (name "X" is too short, violates @Size min=2)
        TestUser invalidUser = new TestUser(700L, "X", "invalid@example.com", true);

        // When/Then
        assertThatThrownBy(() -> iTestUserServiceClient.createUser(invalidUser))
                .isInstanceOf(RabbitRpcServiceValidationException.class)
                .satisfies(ex -> {
                    RabbitRpcServiceValidationException valEx = (RabbitRpcServiceValidationException) ex;
                    assertThat(valEx.getMessage()).contains("Validation failed for fields: TestUser.name");
                    assertThat(valEx.getBindingResult()).containsKey("TestUser.name");
                    assertThat(valEx.getBindingResult().get("TestUser.name")).contains("Name must be between 2 and 50 characters");
                });
    }

    @Test
    @DisplayName("Should recover after error and process next request successfully")
    void shouldRecoverAfterErrorAndProcessNextRequestSuccessfully() {
        // Given - first request will fail
        Long nonExistentUserId = 999L;

        // When - first request fails
        assertThatThrownBy(() -> iTestUserServiceClient.getUser(nonExistentUserId))
                .isInstanceOf(RabbitRpcServiceException.class);

        // Then - subsequent request should succeed
        TestUser user = iTestUserServiceClient.getUser(1L);
        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should handle multiple consecutive errors")
    void shouldHandleMultipleConsecutiveErrors() {
        // When/Then - multiple errors in sequence
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> iTestUserServiceClient.getUser(999L))
                    .isInstanceOf(RabbitRpcServiceException.class);
        }

        // Should still work after multiple errors
        TestUser user = iTestUserServiceClient.getUser(1L);
        assertThat(user).isNotNull();
    }

    @Test
    @DisplayName("Should handle error during update operation")
    void shouldHandleErrorDuringUpdateOperation() {
        // Given - try to update non-existent user
        TestUser nonExistentUser = new TestUser(999L, "Ghost User", "ghost@example.com", true);

        // When/Then
        assertThatThrownBy(() -> iTestUserServiceClient.updateUser(nonExistentUser))
                .isInstanceOf(RabbitRpcServiceException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Should handle error during delete operation")
    void shouldHandleErrorDuringDeleteOperation() {
        // Given - try to delete non-existent user
        Long nonExistentUserId = 999L;

        // When/Then
        assertThatThrownBy(() -> iTestUserServiceClient.deleteUser(nonExistentUserId))
                .isInstanceOf(RabbitRpcServiceException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Should maintain system stability after validation errors")
    void shouldMaintainSystemStabilityAfterValidationErrors() {
        // Given - multiple invalid operations
        TestUser invalidUser1 = new TestUser(null, "Test", "test@example.com", true);
        TestUser invalidUser2 = new TestUser(501L, "", "test@example.com", true);
        TestUser invalidUser3 = new TestUser(502L, "Test", "invalid-email", true);

        // When - multiple validation failures
        assertThatThrownBy(() -> iTestUserServiceClient.createUser(invalidUser1))
                .isInstanceOf(RabbitRpcServiceValidationException.class);

        assertThatThrownBy(() -> iTestUserServiceClient.createUser(invalidUser2))
                .isInstanceOf(RabbitRpcServiceValidationException.class);

        assertThatThrownBy(() -> iTestUserServiceClient.createUser(invalidUser3))
                .isInstanceOf(RabbitRpcServiceValidationException.class);

        // Then - system should still work
        TestUser validUser = new TestUser(503L, "Valid User", "valid@example.com", true);
        TestUser created = iTestUserServiceClient.createUser(validUser);
        assertThat(created).isNotNull();
    }

    @Test
    @DisplayName("Should handle alternating success and error requests")
    void shouldHandleAlternatingSuccessAndErrorRequests() {
        // When/Then - alternate between success and failure
        TestUser user1 = iTestUserServiceClient.getUser(1L);
        assertThat(user1).isNotNull();

        assertThatThrownBy(() -> iTestUserServiceClient.getUser(999L))
                .isInstanceOf(RabbitRpcServiceException.class);

        TestUser user2 = iTestUserServiceClient.getUser(2L);
        assertThat(user2).isNotNull();

        assertThatThrownBy(() -> iTestUserServiceClient.getUser(998L))
                .isInstanceOf(RabbitRpcServiceException.class);

        TestUser user3 = iTestUserServiceClient.getUser(3L);
        assertThat(user3).isNotNull();
    }

    @Test
    @DisplayName("Should preserve error message details")
    void shouldPreserveErrorMessageDetails() {
        // Given
        Long userId = 1L;

        // When/Then - error message should contain specific details
        assertThatThrownBy(() -> iTestUserServiceClient.getUserWithError(userId))
                .isInstanceOf(RabbitRpcServiceException.class)
                .hasMessageContaining("Simulated error for user id: " + userId);
    }

    @Test
    @DisplayName("Should handle validation errors with custom messages")
    void shouldHandleValidationErrorsWithCustomMessages() {
        // Given - user violating email constraint
        TestUser invalidEmailUser = new TestUser(710L, "Test User", "not-an-email", true);

        // When/Then
        assertThatThrownBy(() -> iTestUserServiceClient.createUser(invalidEmailUser))
                .isInstanceOf(RabbitRpcServiceValidationException.class)
                .satisfies(ex -> {
                    RabbitRpcServiceValidationException valEx = (RabbitRpcServiceValidationException) ex;
                    assertThat(valEx.getMessage()).contains("Validation failed for fields: TestUser.email");
                    assertThat(valEx.getBindingResult()).containsKey("TestUser.email");
                    assertThat(valEx.getBindingResult().get("TestUser.email")).contains("Email must be valid");
                });
    }

    @Test
    @DisplayName("Should handle errors in transaction-like scenarios")
    void shouldHandleErrorsInTransactionLikeScenarios() {
        // Given - create a user
        TestUser user = new TestUser(720L, "Transaction Test", "transaction@example.com", true);
        iTestUserServiceClient.createUser(user);

        // When - try to update with invalid data
        TestUser invalidUpdate = new TestUser(720L, "X", "test@example.com", true);

        assertThatThrownBy(() -> iTestUserServiceClient.updateUser(invalidUpdate))
                .isInstanceOf(RabbitRpcServiceValidationException.class);

        // Then - original data should still be intact
        TestUser retrieved = iTestUserServiceClient.getUser(720L);
        assertThat(retrieved.getName()).isEqualTo("Transaction Test");
    }

    @Test
    @DisplayName("Should handle rapid fire error requests")
    void shouldHandleRapidFireErrorRequests() {
        // When/Then - send many error-causing requests rapidly
        for (int i = 0; i < 20; i++) {
            assertThatThrownBy(() -> iTestUserServiceClient.getUser(999L))
                    .isInstanceOf(RabbitRpcServiceException.class);
        }

        // System should still be functional
        TestUser user = iTestUserServiceClient.getUser(1L);
        assertThat(user).isNotNull();
    }

    @Test
    @DisplayName("Should handle null value validation correctly")
    void shouldHandleNullValueValidationCorrectly() {
        // When/Then - various null scenarios
        assertThatThrownBy(() -> iTestUserServiceClient.getUser(null))
                .isInstanceOf(RabbitRpcServiceValidationException.class);

        assertThatThrownBy(() -> iTestUserServiceClient.deleteUser(null))
                .isInstanceOf(RabbitRpcServiceValidationException.class);

        assertDoesNotThrow(() -> iTestUserServiceClient.notifyUser(null, "message"));
    }

    @Test
    @DisplayName("Should provide meaningful error context")
    void shouldProvideMeaningfulErrorContext() {
        // Given - ID that exceeds @Max(1000) validation constraint
        Long specificUserId = 88888L;

        // When/Then - should get validation error, not a user not found error
        assertThatThrownBy(() -> iTestUserServiceClient.getUser(specificUserId))
                .isInstanceOf(RabbitRpcServiceValidationException.class);
    }
}

