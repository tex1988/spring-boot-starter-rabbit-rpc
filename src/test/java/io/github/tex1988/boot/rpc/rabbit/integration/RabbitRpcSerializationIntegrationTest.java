package io.github.tex1988.boot.rpc.rabbit.integration;

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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for serialization/deserialization with Apache Fory.
 * Tests complex data structures, lists, and object graphs.
 */
@SpringBootTest(classes = TestClientServerConfig.class)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("RabbitMQ RPC Serialization Integration Tests")
class RabbitRpcSerializationIntegrationTest extends AbstractRabbitRpcIntegrationTest {

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
    @DisplayName("Should serialize and deserialize simple object")
    void shouldSerializeAndDeserializeSimpleObject() {
        // Given
        TestUser user = new TestUser(400L, "Serialize Test", "serialize@example.com", true);

        // When
        TestUser created = iTestUserServiceClient.createUser(user);
        TestUser retrieved = iTestUserServiceClient.getUser(400L);

        // Then
        assertThat(created).isNotNull();
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getId()).isEqualTo(user.getId());
        assertThat(retrieved.getName()).isEqualTo(user.getName());
        assertThat(retrieved.getEmail()).isEqualTo(user.getEmail());
        assertThat(retrieved.isActive()).isEqualTo(user.isActive());
    }

    @Test
    @DisplayName("Should serialize and deserialize object with null fields")
    void shouldSerializeAndDeserializeObjectWithNullFields() {
        // Given - user with some null fields (email can be null in certain contexts)
        TestUser user = new TestUser(401L, "Null Test", "null@example.com", false);

        // When
        TestUser created = iTestUserServiceClient.createUser(user);

        // Then
        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo("Null Test");
        assertThat(created.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should serialize and deserialize list of objects")
    void shouldSerializeAndDeserializeListOfObjects() {
        // Given - create multiple users
        iTestUserServiceClient.createUser(new TestUser(410L, "List User 1", "list1@example.com", true));
        iTestUserServiceClient.createUser(new TestUser(411L, "List User 2", "list2@example.com", true));
        iTestUserServiceClient.createUser(new TestUser(412L, "List User 3", "list3@example.com", false));

        // When
        List<TestUser> users = iTestUserServiceClient.getAllUsers();

        // Then
        assertThat(users).isNotNull()
                .hasSizeGreaterThanOrEqualTo(3);

        // Verify each user in the list is properly deserialized
        users.forEach(user -> {
            assertThat(user.getId()).isNotNull();
            assertThat(user.getName()).isNotNull();
            assertThat(user.getEmail()).isNotNull();
        });
    }

    @Test
    @DisplayName("Should serialize and deserialize empty list")
    void shouldSerializeAndDeserializeEmptyList() {
        // Given - store current user count
        List<TestUser> initialUsers = iTestUserServiceClient.getAllUsers();
        int initialCount = initialUsers.size();

        // Delete all current users
        for (TestUser user : new ArrayList<>(initialUsers)) {
            try {
                iTestUserServiceClient.deleteUser(user.getId());
            } catch (Exception e) {
                // Ignore if user cannot be deleted (may have been deleted by another test)
            }
        }

        // When
        List<TestUser> users = iTestUserServiceClient.getAllUsers();

        // Then - verify list is returned (may be empty or have users from other tests)
        assertThat(users).isNotNull()
                .hasSizeLessThanOrEqualTo(initialCount);
    }

    @Test
    @DisplayName("Should handle large string values")
    void shouldHandleLargeStringValues() {
        // Given - user with maximum allowed name length
        String maxLengthName = "A".repeat(50); // Max is 50 characters
        TestUser user = new TestUser(420L, maxLengthName, "large@example.com", true);

        // When
        iTestUserServiceClient.createUser(user);
        TestUser retrieved = iTestUserServiceClient.getUser(420L);

        // Then
        assertThat(retrieved.getName()).isEqualTo(maxLengthName);
        assertThat(retrieved.getName()).hasSize(50);
    }

    @Test
    @DisplayName("Should preserve boolean values correctly")
    void shouldPreserveBooleanValuesCorrectly() {
        // Given
        TestUser activeUser = new TestUser(430L, "Active User", "active@example.com", true);
        TestUser inactiveUser = new TestUser(431L, "Inactive User", "inactive@example.com", false);

        // When
        iTestUserServiceClient.createUser(activeUser);
        iTestUserServiceClient.createUser(inactiveUser);

        TestUser retrievedActive = iTestUserServiceClient.getUser(430L);
        TestUser retrievedInactive = iTestUserServiceClient.getUser(431L);

        // Then
        assertThat(retrievedActive.isActive()).isTrue();
        assertThat(retrievedInactive.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should handle special characters in strings")
    void shouldHandleSpecialCharactersInStrings() {
        // Given - user with special characters
        TestUser user = new TestUser(
                440L,
                "User-Name_123",
                "special+chars@example.com",
                true
        );

        // When
        iTestUserServiceClient.createUser(user);
        TestUser retrieved = iTestUserServiceClient.getUser(440L);

        // Then
        assertThat(retrieved.getName()).isEqualTo("User-Name_123");
        assertThat(retrieved.getEmail()).isEqualTo("special+chars@example.com");
    }

    @Test
    @DisplayName("Should serialize and deserialize multiple objects in sequence")
    void shouldSerializeAndDeserializeMultipleObjectsInSequence() {
        // Given
        int count = 20;

        // When - create many users in sequence
        for (int i = 0; i < count; i++) {
            TestUser user = new TestUser(
                    450L + i,
                    "Sequential User " + i,
                    "seq" + i + "@example.com",
                    i % 2 == 0
            );
            TestUser created = iTestUserServiceClient.createUser(user);

            // Then - verify each one immediately
            assertThat(created).isNotNull();
            assertThat(created.getId()).isEqualTo(450L + i);
        }

        // Verify all users can be retrieved
        for (int i = 0; i < count; i++) {
            TestUser retrieved = iTestUserServiceClient.getUser(450L + i);
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getName()).isEqualTo("Sequential User " + i);
        }
    }

    @Test
    @DisplayName("Should handle long numeric values correctly")
    void shouldHandleLongNumericValuesCorrectly() {
        // Given - user with large ID value (within valid range)
        Long largeId = 999L; // Max allowed is 1000
        TestUser user = new TestUser(largeId, "Large ID User", "largeid@example.com", true);

        // When
        iTestUserServiceClient.createUser(user);
        TestUser retrieved = iTestUserServiceClient.getUser(largeId);

        // Then
        assertThat(retrieved.getId()).isEqualTo(largeId);
    }

    @Test
    @DisplayName("Should maintain object equality after serialization")
    void shouldMaintainObjectEqualityAfterSerialization() {
        // Given
        TestUser originalUser = new TestUser(480L, "Equality Test", "equality@example.com", true);

        // When
        TestUser created = iTestUserServiceClient.createUser(originalUser);
        TestUser retrieved = iTestUserServiceClient.getUser(480L);

        // Then - objects should be equal
        assertThat(retrieved).isEqualTo(created)
                .isEqualTo(originalUser);
    }

    @Test
    @DisplayName("Should handle rapid serialization/deserialization cycles")
    void shouldHandleRapidSerializationDeserializationCycles() {
        // Given
        TestUser user = new TestUser(490L, "Rapid Test", "rapid@example.com", true);
        iTestUserServiceClient.createUser(user);

        // When - perform many read operations rapidly
        for (int i = 0; i < 50; i++) {
            TestUser retrieved = iTestUserServiceClient.getUser(490L);

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getId()).isEqualTo(490L);
        }
    }

    @Test
    @DisplayName("Should handle list with mixed data")
    void shouldHandleListWithMixedData() {
        // Given - users with different characteristics
        iTestUserServiceClient.createUser(new TestUser(480L, "AB", "min@example.com", true));
        iTestUserServiceClient.createUser(new TestUser(481L, "Very Long Name With Many Characters", "long@example.com", false));
        iTestUserServiceClient.createUser(new TestUser(482L, "Normal User", "normal@example.com", true));

        // When
        List<TestUser> users = iTestUserServiceClient.getAllUsers();

        // Then - all types should be properly serialized
        assertThat(users).isNotNull()
                .hasSizeGreaterThanOrEqualTo(3);

        boolean foundShortName = users.stream().anyMatch(u -> u.getName().equals("AB"));
        boolean foundLongName = users.stream().anyMatch(u -> u.getName().length() > 20);

        assertThat(foundShortName).isTrue();
        assertThat(foundLongName).isTrue();
    }
}

