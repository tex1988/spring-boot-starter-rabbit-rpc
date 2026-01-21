package io.github.tex1988.boot.rpc.rabbit.integration;

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
 * Integration tests for Jakarta Validation with RabbitMQ RPC.
 * Tests parameter validation, validation groups, and error handling.
 */
@SpringBootTest(classes = TestClientServerConfig.class)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("RabbitMQ RPC Validation Integration Tests")
class RabbitRpcValidationIntegrationTest extends AbstractRabbitRpcIntegrationTest {

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
    @DisplayName("Should validate @NotNull constraint on ID parameter")
    void shouldValidateNotNullConstraintOnIdParameter() {
        // When/Then
        assertThatThrownBy(() -> iTestUserServiceClient.getUser(null))
                .isInstanceOf(RabbitRpcServiceValidationException.class)
                .hasMessageContaining("Validation failed for fields: id")
                .satisfies(ex -> {
                    RabbitRpcServiceValidationException valEx = (RabbitRpcServiceValidationException) ex;
                    assertThat(valEx.getBindingResult())
                            .containsKey("id");
                    assertThat(valEx.getStatusCode())
                            .isEqualTo(400);
                });
    }

    @Test
    @DisplayName("Should validate @Min constraint on ID parameter")
    void shouldValidateMinConstraintOnIdParameter() {
        // When/Then
        assertThatThrownBy(() -> iTestUserServiceClient.getUser(0L))
                .isInstanceOf(RabbitRpcServiceValidationException.class)
                .hasMessageContaining("Validation failed for fields: id")
                .satisfies(ex -> {
                    RabbitRpcServiceValidationException valEx = (RabbitRpcServiceValidationException) ex;
                    assertThat(valEx.getBindingResult())
                            .containsKey("id");
                    assertThat(valEx.getStatusCode())
                            .isEqualTo(400);
                });
    }

    @Test
    @DisplayName("Should validate @Max constraint on ID parameter")
    void shouldValidateMaxConstraintOnIdParameter() {
        // When/Then
        assertThatThrownBy(() -> iTestUserServiceClient.getUser(1001L))
                .isInstanceOf(RabbitRpcServiceValidationException.class)
                .hasMessageContaining("Validation failed for fields: id")
                .satisfies(ex -> {
                    RabbitRpcServiceValidationException valEx = (RabbitRpcServiceValidationException) ex;
                    assertThat(valEx.getBindingResult())
                            .containsKey("id");
                    assertThat(valEx.getStatusCode())
                            .isEqualTo(400);
                });
    }

    @Test
    @DisplayName("Should validate within valid range for ID parameter")
    void shouldValidateWithinValidRangeForIdParameter() {
        // Given - ID within valid range [1, 1000]
        Long validId = 1L;

        // When
        TestUser user = iTestUserServiceClient.getUser(validId);

        // Then
        assertThat(user).isNotNull();
    }

    @Test
    @DisplayName("Should validate @NotNull on user object fields during create")
    void shouldValidateNotNullOnUserFieldsDuringCreate() {
        // Given - User with null ID (OnCreate group validation)
        TestUser invalidUser = new TestUser(null, "Test User", "test@example.com", true);

        // When/Then
        assertThatThrownBy(() -> iTestUserServiceClient.createUser(invalidUser))
                .isInstanceOf(RabbitRpcServiceValidationException.class)
                .hasMessageContaining("Validation failed for fields: TestUser.id")
                .satisfies(ex -> {
                    RabbitRpcServiceValidationException valEx = (RabbitRpcServiceValidationException) ex;
                    assertThat(valEx.getBindingResult())
                            .containsKey("TestUser.id");
                    assertThat(valEx.getBindingResult().get("TestUser.id"))
                            .contains("ID cannot be null");
                    assertThat(valEx.getStatusCode())
                            .isEqualTo(400);
                });
    }

    @Test
    @DisplayName("Should validate @NotBlank on name field during create")
    void shouldValidateNotBlankOnNameFieldDuringCreate() {
        // Given - User with blank name (triggers both @NotBlank and @Size)
        TestUser invalidUser = new TestUser(510L, "", "test@example.com", true);

        // When/Then
        assertThatThrownBy(() -> iTestUserServiceClient.createUser(invalidUser))
                .isInstanceOf(RabbitRpcServiceValidationException.class)
                .hasMessageContaining("Validation failed for fields: TestUser.name")
                .satisfies(ex -> {
                    RabbitRpcServiceValidationException valEx = (RabbitRpcServiceValidationException) ex;
                    assertThat(valEx.getBindingResult())
                            .containsKey("TestUser.name");
                    // Blank string violates both @NotBlank and @Size(min=2)
                    String message = valEx.getBindingResult().get("TestUser.name");
                    assertThat(message)
                            .contains("Name cannot be blank")
                            .contains("Name must be between 2 and 50 characters");
                });
    }

    @Test
    @DisplayName("Should validate @Size constraint on name field during create")
    void shouldValidateSizeConstraintOnNameFieldDuringCreate() {
        // Given - User with name exceeding max length
        TestUser invalidUser = new TestUser(511L,
                "This is a very long name that exceeds the maximum allowed length",
                "test@example.com", true);

        // When/Then
        assertThatThrownBy(() -> iTestUserServiceClient.createUser(invalidUser))
                .isInstanceOf(RabbitRpcServiceValidationException.class)
                .hasMessageContaining("Validation failed for fields: TestUser.name")
                .satisfies(ex -> {
                    RabbitRpcServiceValidationException valEx = (RabbitRpcServiceValidationException) ex;
                    assertThat(valEx.getBindingResult()).containsKey("TestUser.name");
                    assertThat(valEx.getBindingResult().get("TestUser.name")).contains("Name must be between 2 and 50 characters");
                    assertThat(valEx.getStatusCode()).isEqualTo(400);
                });
    }

    @Test
    @DisplayName("Should validate @Size constraint with minimum length on name field")
    void shouldValidateSizeConstraintWithMinimumLengthOnNameField() {
        // Given - User with name below min length
        TestUser invalidUser = new TestUser(512L, "A", "test@example.com", true);

        // When/Then
        assertThatThrownBy(() -> iTestUserServiceClient.createUser(invalidUser))
                .isInstanceOf(RabbitRpcServiceValidationException.class)
                .hasMessageContaining("Validation failed for fields: TestUser.name")
                .satisfies(ex -> {
                    RabbitRpcServiceValidationException valEx = (RabbitRpcServiceValidationException) ex;
                    assertThat(valEx.getBindingResult()).containsKey("TestUser.name");
                    assertThat(valEx.getBindingResult().get("TestUser.name")).contains("Name must be between 2 and 50 characters");
                    assertThat(valEx.getStatusCode()).isEqualTo(400);
                });
    }

    @Test
    @DisplayName("Should validate @Email constraint on email field during create")
    void shouldValidateEmailConstraintDuringCreate() {
        // Given - User with invalid email
        TestUser invalidUser = new TestUser(513L, "Test User", "invalid-email", true);

        // When/Then
        assertThatThrownBy(() -> iTestUserServiceClient.createUser(invalidUser))
                .isInstanceOf(RabbitRpcServiceValidationException.class)
                .hasMessageContaining("Validation failed for fields: TestUser.email")
                .satisfies(ex -> {
                    RabbitRpcServiceValidationException valEx = (RabbitRpcServiceValidationException) ex;
                    assertThat(valEx.getBindingResult()).containsKey("TestUser.email");
                    assertThat(valEx.getBindingResult().get("TestUser.email")).contains("Email must be valid");
                    assertThat(valEx.getStatusCode()).isEqualTo(400);
                });
    }

    @Test
    @DisplayName("Should validate @NotBlank on email field during create")
    void shouldValidateNotBlankOnEmailFieldDuringCreate() {
        // Given - User with blank email (triggers both @NotBlank and @Email)
        TestUser invalidUser = new TestUser(514L, "Test User", "", true);

        // When/Then
        assertThatThrownBy(() -> iTestUserServiceClient.createUser(invalidUser))
                .isInstanceOf(RabbitRpcServiceValidationException.class)
                .hasMessageContaining("Validation failed for fields: TestUser.email")
                .satisfies(ex -> {
                    RabbitRpcServiceValidationException valEx = (RabbitRpcServiceValidationException) ex;
                    assertThat(valEx.getBindingResult()).containsKey("TestUser.email");
                    // Blank string violates both @NotBlank and @Email
                    String message = valEx.getBindingResult().get("TestUser.email");
                    assertThat(message).contains("Email cannot be blank");
                    assertThat(valEx.getStatusCode()).isEqualTo(400);
                });
    }

    @Test
    @DisplayName("Should successfully create user with valid data")
    void shouldSuccessfullyCreateUserWithValidData() {
        // Given - User with all valid data
        TestUser validUser = new TestUser(501L, "Valid User", "valid@example.com", true);

        // When
        TestUser createdUser = iTestUserServiceClient.createUser(validUser);

        // Then
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getId()).isEqualTo(501L);
        assertThat(createdUser.getName()).isEqualTo("Valid User");
        assertThat(createdUser.getEmail()).isEqualTo("valid@example.com");
    }

    @Test
    @DisplayName("Should validate OnUpdate group constraints")
    void shouldValidateOnUpdateGroupConstraints() {
        // Given - Create a user first
        TestUser user = new TestUser(502L, "Update Test", "update@example.com", true);
        iTestUserServiceClient.createUser(user);

        // When - Update with invalid data (null ID)
        TestUser invalidUpdate = new TestUser(null, "Updated Name", "updated@example.com", false);

        // Then
        assertThatThrownBy(() -> iTestUserServiceClient.updateUser(invalidUpdate))
                .isInstanceOf(RabbitRpcServiceValidationException.class)
                .hasMessageContaining("Validation failed for fields: TestUser.id")
                .satisfies(ex -> {
                    RabbitRpcServiceValidationException valEx = (RabbitRpcServiceValidationException) ex;
                    assertThat(valEx.getBindingResult()).containsKey("TestUser.id");
                    assertThat(valEx.getBindingResult().get("TestUser.id")).contains("ID cannot be null");
                    assertThat(valEx.getStatusCode()).isEqualTo(400);
                });
    }

    @Test
    @DisplayName("Should validate all constraints during update")
    void shouldValidateAllConstraintsDuringUpdate() {
        // Given - Create a user first
        TestUser user = new TestUser(503L, "Original Name", "original@example.com", true);
        iTestUserServiceClient.createUser(user);

        // When - Update with invalid email
        TestUser invalidUpdate = new TestUser(503L, "Updated Name", "invalid-email", false);

        // Then
        assertThatThrownBy(() -> iTestUserServiceClient.updateUser(invalidUpdate))
                .isInstanceOf(RabbitRpcServiceValidationException.class)
                .hasMessageContaining("Validation failed for fields: TestUser.email")
                .satisfies(ex -> {
                    RabbitRpcServiceValidationException valEx = (RabbitRpcServiceValidationException) ex;
                    assertThat(valEx.getBindingResult()).containsKey("TestUser.email");
                    assertThat(valEx.getBindingResult().get("TestUser.email")).contains("Email must be valid");
                    assertThat(valEx.getStatusCode()).isEqualTo(400);
                });
    }

    @Test
    @DisplayName("Should successfully update user with valid data")
    void shouldSuccessfullyUpdateUserWithValidData() {
        // Given - Create a user first
        TestUser user = new TestUser(504L, "Original", "original@example.com", true);
        iTestUserServiceClient.createUser(user);

        // When - Update with valid data
        TestUser validUpdate = new TestUser(504L, "Updated Name", "updated@example.com", false);
        TestUser updatedUser = iTestUserServiceClient.updateUser(validUpdate);

        // Then
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getName()).isEqualTo("Updated Name");
        assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");
        assertThat(updatedUser.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should validate multiple constraint violations at once")
    void shouldValidateMultipleConstraintViolationsAtOnce() {
        // Given - User with multiple invalid fields
        TestUser invalidUser = new TestUser(null, "", "invalid-email", true);

        // When/Then - Should report all violations (sorted alphabetically)
        assertThatThrownBy(() -> iTestUserServiceClient.createUser(invalidUser))
                .isInstanceOf(RabbitRpcServiceValidationException.class)
                .hasMessageContaining("Validation failed for fields:")
                .satisfies(ex -> {
                    RabbitRpcServiceValidationException valEx = (RabbitRpcServiceValidationException) ex;
                    // Should have violations for id, name, and email fields
                    assertThat(valEx.getBindingResult()).hasSize(3);
                    assertThat(valEx.getBindingResult()).containsKeys("TestUser.id", "TestUser.name", "TestUser.email");
                    assertThat(valEx.getStatusCode()).isEqualTo(400);
                });
    }

    @Test
    @DisplayName("Should validate @NotNull on deleteUser parameter")
    void shouldValidateNotNullOnDeleteUserParameter() {
        // When/Then
        assertThatThrownBy(() -> iTestUserServiceClient.deleteUser(null))
                .isInstanceOf(RabbitRpcServiceValidationException.class)
                .hasMessageContaining("Validation failed for fields: id")
                .satisfies(ex -> {
                    RabbitRpcServiceValidationException valEx = (RabbitRpcServiceValidationException) ex;
                    assertThat(valEx.getBindingResult()).containsKey("id");
                    assertThat(valEx.getStatusCode()).isEqualTo(400);
                });
    }

    @Test
    @DisplayName("Should validate @NotNull on notifyUser userId parameter and does not throw for @FireAndForget")
    void shouldValidateNotNullOnNotifyUserUserIdParameter() {
        // When/Then
        assertDoesNotThrow(() -> iTestUserServiceClient.notifyUser(null, "test message"));
    }
}
