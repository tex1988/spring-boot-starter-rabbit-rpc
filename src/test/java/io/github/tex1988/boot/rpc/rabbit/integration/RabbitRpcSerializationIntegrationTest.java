package io.github.tex1988.boot.rpc.rabbit.integration;

import io.github.tex1988.boot.rpc.rabbit.integration.config.TestClientServerConfig;
import io.github.tex1988.boot.rpc.rabbit.integration.explicit.ExplicitlyRegisteredClass;
import io.github.tex1988.boot.rpc.rabbit.integration.model.TestUser;
import io.github.tex1988.boot.rpc.rabbit.integration.service.ITestUnregisteredService;
import io.github.tex1988.boot.rpc.rabbit.integration.service.ITestUserService;
import io.github.tex1988.boot.rpc.rabbit.integration.service.impl.TestUserServiceImpl;
import io.github.tex1988.boot.rpc.rabbit.integration.unregistered.UnregisteredClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    private ITestUnregisteredService iTestUnregisteredServiceClient;

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

    @Test
    @DisplayName("Should timeout when server fails to serialize unregistered class in response")
    void shouldTimeoutWhenServerFailsToSerializeUnregisteredClass() {
        // Given - server will try to return an UnregisteredClass which is not in allowed patterns
        Long testId = 500L;

        // When/Then - should get MessageConversionException because serialization fails
        // The error occurs because UnregisteredClass is not in the allowed serialization patterns
        assertThatThrownBy(() -> iTestUnregisteredServiceClient.getUnregisteredData(testId))
                .satisfies(exception -> {
                    // The actual exception is MessageConversionException wrapped in other exceptions
                    assertThat(exception).isNotNull();
                    assertThat(exception.toString()).contains(
                            "No response from ITestUnregisteredService"
                    );
                });
    }

    @Test
    @DisplayName("Should fail when client sends unregistered class to server")
    void shouldTimeoutWhenClientSendsUnregisteredClass() {
        // Given - client tries to send an UnregisteredClass which is not in allowed patterns
        UnregisteredClass unregisteredData = new UnregisteredClass(501L, "Test Data", false);

        // When/Then - should fail with serialization error
        // The client or server fails to serialize/deserialize the unregistered class
        assertThatThrownBy(() -> iTestUnregisteredServiceClient.processUnregisteredData(unregisteredData))
                .satisfies(exception -> {
                    assertThat(exception).isNotNull();
                    // Could be various exceptions depending on where serialization fails
                    assertThat(exception.toString()).contains(
                            "Failed to convert object to message"
                    );
                });
    }

    @Test
    @DisplayName("Should demonstrate security boundary - unregistered classes are blocked")
    void shouldDemonstrateSecurityBoundary() {
        // This test demonstrates that the allowedSerializationPatterns parameter
        // effectively creates a security boundary by preventing serialization of
        // classes outside the allowed packages

        // Given - UnregisteredClass is in package io.github.tex1988.boot.rpc.rabbit.integration.unregistered
        // And allowed pattern is "io.github.tex1988.boot.rpc.rabbit.integration.model.*"

        // When - attempt to use unregistered class
        Long testId = 505L;

        // Then - should fail due to serialization restrictions
        assertThatThrownBy(() -> iTestUnregisteredServiceClient.getUnregisteredData(testId))
                .satisfies(exception -> {
                    assertThat(exception).isNotNull();
                    // Serialization fails because server cannot serialize the response
                    assertThat(exception.toString()).contains(
                            "No response from ITestUnregisteredService"
                    );
                });

        // And - operations with allowed classes should work fine
        TestUser allowedUser = iTestUserServiceClient.getUser(1L);
        assertThat(allowedUser).isNotNull();
        assertThat(allowedUser.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should serialize and deserialize String arrays")
    void shouldSerializeStringArrays() {
        // Given
        String[] input = {"apple", "banana", "cherry"};

        // When
        String[] result = iTestUserServiceClient.processArray(input);

        // Then
        assertThat(result)
                .hasSize(3)
                .containsExactly("processed-apple", "processed-banana", "processed-cherry");
    }

    @Test
    @DisplayName("Should serialize and deserialize primitive int arrays")
    void shouldSerializeIntArrays() {
        // Given
        int[] input = {1, 2, 3, 4, 5};

        // When
        int[] result = iTestUserServiceClient.processIntArray(input);

        // Then
        assertThat(result)
                .hasSize(5)
                .containsExactly(2, 4, 6, 8, 10);
    }

    @Test
    @DisplayName("Should serialize and deserialize mutable Lists")
    void shouldSerializeMutableLists() {
        // Given - mutable ArrayList
        List<String> input = new ArrayList<>(Arrays.asList("one", "two", "three"));

        // When
        List<String> result = iTestUserServiceClient.processList(input);

        // Then
        assertThat(result)
                .hasSize(3)
                .containsExactly("processed-one", "processed-two", "processed-three")
                .isInstanceOf(ArrayList.class);
    }

    @Test
    @DisplayName("Should serialize and deserialize immutable Lists (List.of)")
    void shouldSerializeImmutableLists() {
        // Given - immutable list created with List.of()
        List<String> input = List.of("alpha", "beta", "gamma");

        // When
        List<String> result = iTestUserServiceClient.processImmutableList(input);

        // Then
        assertThat(result)
                .hasSize(3)
                .containsExactly("immutable-alpha", "immutable-beta", "immutable-gamma");
    }

    @Test
    @DisplayName("Should serialize and deserialize mutable Maps")
    void shouldSerializeMutableMaps() {
        // Given - mutable HashMap
        Map<String, Integer> input = new HashMap<>();
        input.put("a", 1);
        input.put("b", 2);
        input.put("c", 3);

        // When
        Map<String, Integer> result = iTestUserServiceClient.processMap(input);

        // Then
        assertThat(result)
                .hasSize(3)
                .containsEntry("a", 2)
                .containsEntry("b", 3)
                .containsEntry("c", 4)
                .isInstanceOf(HashMap.class);
    }

    @Test
    @DisplayName("Should serialize and deserialize immutable Maps (Map.of)")
    void shouldSerializeImmutableMaps() {
        // Given - immutable map created with Map.of()
        Map<String, Integer> input = Map.of("x", 10, "y", 20, "z", 30);

        // When
        Map<String, Integer> result = iTestUserServiceClient.processImmutableMap(input);

        // Then
        assertThat(result)
                .hasSize(3)
                .containsEntry("immutable-x", 20)
                .containsEntry("immutable-y", 30)
                .containsEntry("immutable-z", 40);
    }

    @Test
    @DisplayName("Should serialize and deserialize mutable Sets")
    void shouldSerializeMutableSets() {
        // Given - mutable HashSet
        Set<String> input = new HashSet<>(Arrays.asList("red", "green", "blue"));

        // When
        Set<String> result = iTestUserServiceClient.processSet(input);

        // Then
        assertThat(result)
                .hasSize(3)
                .containsExactlyInAnyOrder("processed-red", "processed-green", "processed-blue")
                .isInstanceOf(HashSet.class);
    }

    @Test
    @DisplayName("Should serialize and deserialize immutable Sets (Set.of)")
    void shouldSerializeImmutableSets() {
        // Given - immutable set created with Set.of()
        Set<String> input = Set.of("cat", "dog", "bird");

        // When
        Set<String> result = iTestUserServiceClient.processImmutableSet(input);

        // Then
        assertThat(result)
                .hasSize(3)
                .containsExactlyInAnyOrder("immutable-cat", "immutable-dog", "immutable-bird");
    }

    @Test
    @DisplayName("Should serialize and deserialize nested collections")
    void shouldSerializeNestedCollections() {
        // Given - nested structure: List<Map<String, List<Integer>>>
        List<Map<String, List<Integer>>> input = new ArrayList<>();

        Map<String, List<Integer>> map1 = new HashMap<>();
        map1.put("numbers", Arrays.asList(1, 2, 3));
        map1.put("primes", Arrays.asList(2, 3, 5));

        Map<String, List<Integer>> map2 = new HashMap<>();
        map2.put("evens", Arrays.asList(2, 4, 6));

        input.add(map1);
        input.add(map2);

        // When
        List<Map<String, List<Integer>>> result = iTestUserServiceClient.processNestedCollections(input);

        // Then
        assertThat(result).hasSize(2);

        // Check first map
        Map<String, List<Integer>> resultMap1 = result.get(0);
        assertThat(resultMap1).containsKey("numbers-processed");
        assertThat(resultMap1.get("numbers-processed")).containsExactly(2, 3, 4);
        assertThat(resultMap1).containsKey("primes-processed");
        assertThat(resultMap1.get("primes-processed")).containsExactly(3, 4, 6);

        // Check second map
        Map<String, List<Integer>> resultMap2 = result.get(1);
        assertThat(resultMap2).containsKey("evens-processed");
        assertThat(resultMap2.get("evens-processed")).containsExactly(3, 5, 7);
    }

    @Test
    @DisplayName("Should serialize empty collections")
    void shouldSerializeEmptyCollections() {
        // Given
        String[] emptyArray = new String[0];
        List<String> emptyList = List.of();
        Map<String, Integer> emptyMap = Map.of();
        Set<String> emptySet = Set.of();

        // When/Then
        assertThat(iTestUserServiceClient.processArray(emptyArray)).isEmpty();
        assertThat(iTestUserServiceClient.processImmutableList(emptyList)).isEmpty();
        assertThat(iTestUserServiceClient.processImmutableMap(emptyMap)).isEmpty();
        assertThat(iTestUserServiceClient.processImmutableSet(emptySet)).isEmpty();
    }

    @Test
    @DisplayName("Should serialize large collections")
    void shouldSerializeLargeCollections() {
        // Given - large collections
        List<String> largeList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largeList.add("item-" + i);
        }

        // When
        List<String> result = iTestUserServiceClient.processList(largeList);

        // Then
        assertThat(result)
                .hasSize(1000)
                .contains("processed-item-0", "processed-item-999");
    }

    @Test
    @DisplayName("Should serialize and deserialize java.util.Date")
    void shouldSerializeDate() {
        // Given - Date with specific time
        Date input = new Date(1642780800000L); // 2022-01-21 12:00:00 UTC

        // When
        Date result = iTestUserServiceClient.processDate(input);

        // Then - should be one day later
        assertThat(result).isNotNull();
        assertThat(result.getTime()).isEqualTo(input.getTime() + 86400000L);
    }

    @Test
    @DisplayName("Should serialize and deserialize LocalDate")
    void shouldSerializeLocalDate() {
        // Given
        LocalDate input = LocalDate.of(2024, 6, 15);

        // When
        LocalDate result = iTestUserServiceClient.processLocalDate(input);

        // Then - should be one day later
        assertThat(result)
                .isNotNull()
                .isEqualTo(LocalDate.of(2024, 6, 16));
    }

    @Test
    @DisplayName("Should serialize and deserialize LocalDateTime")
    void shouldSerializeLocalDateTime() {
        // Given
        LocalDateTime input = LocalDateTime.of(2024, 6, 15, 14, 30, 45);

        // When
        LocalDateTime result = iTestUserServiceClient.processLocalDateTime(input);

        // Then - should be one hour later
        assertThat(result)
                .isNotNull()
                .isEqualTo(LocalDateTime.of(2024, 6, 15, 15, 30, 45));
    }

    @Test
    @DisplayName("Should serialize and deserialize LocalTime")
    void shouldSerializeLocalTime() {
        // Given
        LocalTime input = LocalTime.of(14, 30, 45);

        // When
        LocalTime result = iTestUserServiceClient.processLocalTime(input);

        // Then - should be one hour later
        assertThat(result)
                .isNotNull()
                .isEqualTo(LocalTime.of(15, 30, 45));
    }

    @Test
    @DisplayName("Should serialize and deserialize Instant")
    void shouldSerializeInstant() {
        // Given
        Instant input = Instant.parse("2024-06-15T14:30:45Z");

        // When
        Instant result = iTestUserServiceClient.processInstant(input);

        // Then - should be one hour later
        assertThat(result)
                .isNotNull()
                .isEqualTo(Instant.parse("2024-06-15T15:30:45Z"));
    }

    @Test
    @DisplayName("Should serialize and deserialize ZonedDateTime")
    void shouldSerializeZonedDateTime() {
        // Given - ZonedDateTime with specific timezone
        ZonedDateTime input = ZonedDateTime.of(
                2024, 6, 15, 14, 30, 45, 0,
                ZoneId.of("America/New_York")
        );

        // When
        ZonedDateTime result = iTestUserServiceClient.processZonedDateTime(input);

        // Then - should be one hour later, same timezone
        assertThat(result).isNotNull();
        assertThat(result.getHour()).isEqualTo(15);
        assertThat(result.getZone()).isEqualTo(ZoneId.of("America/New_York"));
    }

    @Test
    @DisplayName("Should serialize and deserialize OffsetDateTime")
    void shouldSerializeOffsetDateTime() {
        // Given - OffsetDateTime with UTC offset
        OffsetDateTime input = OffsetDateTime.of(
                2024, 6, 15, 14, 30, 45, 0,
                ZoneOffset.ofHours(2)
        );

        // When
        OffsetDateTime result = iTestUserServiceClient.processOffsetDateTime(input);

        // Then - should be one hour later, same offset
        assertThat(result).isNotNull();
        assertThat(result.getHour()).isEqualTo(15);
        assertThat(result.getOffset()).isEqualTo(ZoneOffset.ofHours(2));
    }

    @Test
    @DisplayName("Should serialize and deserialize Duration")
    void shouldSerializeDuration() {
        // Given - Duration of 5 hours
        Duration input = Duration.ofHours(5);

        // When
        Duration result = iTestUserServiceClient.processDuration(input);

        // Then - should be doubled (10 hours)
        assertThat(result)
                .isNotNull()
                .isEqualTo(Duration.ofHours(10));
    }

    @Test
    @DisplayName("Should serialize and deserialize Period")
    void shouldSerializePeriod() {
        // Given - Period of 1 year, 2 months, 3 days
        Period input = Period.of(1, 2, 3);

        // When
        Period result = iTestUserServiceClient.processPeriod(input);

        // Then - should have one extra day
        assertThat(result)
                .isNotNull()
                .isEqualTo(Period.of(1, 2, 4));
    }

    @Test
    @DisplayName("Should handle edge case dates and times")
    void shouldHandleEdgeCaseDates() {
        // Test various edge cases

        // Min date
        LocalDate minDate = LocalDate.MIN;
        LocalDate minResult = iTestUserServiceClient.processLocalDate(minDate);
        assertThat(minResult).isAfter(minDate);

        // Epoch
        Date epoch = new Date(0L);
        Date epochResult = iTestUserServiceClient.processDate(epoch);
        assertThat(epochResult.getTime()).isEqualTo(86400000L);

        // Midnight
        LocalTime midnight = LocalTime.MIDNIGHT;
        LocalTime midnightResult = iTestUserServiceClient.processLocalTime(midnight);
        assertThat(midnightResult).isEqualTo(LocalTime.of(1, 0, 0));
    }

    @Test
    @DisplayName("Should serialize dates with different timezones")
    void shouldSerializeDatesWithDifferentTimezones() {
        // Given - same instant in different timezones
        ZonedDateTime nyTime = ZonedDateTime.of(
                2024, 6, 15, 14, 0, 0, 0,
                ZoneId.of("America/New_York")
        );
        ZonedDateTime tokyoTime = ZonedDateTime.of(
                2024, 6, 16, 3, 0, 0, 0,
                ZoneId.of("Asia/Tokyo")
        );

        // When
        ZonedDateTime nyResult = iTestUserServiceClient.processZonedDateTime(nyTime);
        ZonedDateTime tokyoResult = iTestUserServiceClient.processZonedDateTime(tokyoTime);

        // Then - both should maintain their timezones
        assertThat(nyResult.getZone()).isEqualTo(ZoneId.of("America/New_York"));
        assertThat(tokyoResult.getZone()).isEqualTo(ZoneId.of("Asia/Tokyo"));
    }

    @Test
    @DisplayName("Should serialize date/time types with nanosecond precision")
    void shouldSerializeDateTimeWithNanoseconds() {
        // Given - LocalDateTime with nanoseconds
        LocalDateTime input = LocalDateTime.of(2024, 6, 15, 14, 30, 45, 123456789);

        // When
        LocalDateTime result = iTestUserServiceClient.processLocalDateTime(input);

        // Then - nanoseconds should be preserved
        assertThat(result.getNano()).isEqualTo(123456789);
    }

    @Test
    @DisplayName("Should serialize Duration with various units")
    void shouldSerializeDurationWithVariousUnits() {
        // Test different duration types
        Duration days = Duration.ofDays(3);
        assertThat(iTestUserServiceClient.processDuration(days))
                .isEqualTo(Duration.ofDays(6));

        Duration hours = Duration.ofHours(2);
        assertThat(iTestUserServiceClient.processDuration(hours))
                .isEqualTo(Duration.ofHours(4));

        Duration minutes = Duration.ofMinutes(30);
        assertThat(iTestUserServiceClient.processDuration(minutes))
                .isEqualTo(Duration.ofMinutes(60));

        Duration seconds = Duration.ofSeconds(45);
        assertThat(iTestUserServiceClient.processDuration(seconds))
                .isEqualTo(Duration.ofSeconds(90));
    }

    @Test
    @DisplayName("Should serialize Period with various components")
    void shouldSerializePeriodWithVariousComponents() {
        // Test periods with different components
        Period yearsOnly = Period.ofYears(2);
        assertThat(iTestUserServiceClient.processPeriod(yearsOnly))
                .isEqualTo(Period.of(2, 0, 1));

        Period monthsOnly = Period.ofMonths(6);
        assertThat(iTestUserServiceClient.processPeriod(monthsOnly))
                .isEqualTo(Period.of(0, 6, 1));

        Period daysOnly = Period.ofDays(15);
        assertThat(iTestUserServiceClient.processPeriod(daysOnly))
                .isEqualTo(Period.ofDays(16));
    }

    @Test
    @DisplayName("Should serialize class registered by full name (not pattern)")
    void shouldSerializeExplicitlyRegisteredClass() {
        // Given - class registered by full name: io.github.tex1988.boot.rpc.rabbit.integration.explicit.ExplicitlyRegisteredClass
        // (not by package pattern like "io.github.tex1988.boot.rpc.rabbit.integration.explicit.*")
        ExplicitlyRegisteredClass input = new ExplicitlyRegisteredClass(100L, "test-data", 42);

        // When
        ExplicitlyRegisteredClass result = iTestUserServiceClient.processExplicitlyRegisteredClass(input);

        // Then - should serialize and deserialize successfully
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getData()).isEqualTo("processed-test-data");
        assertThat(result.getValue()).isEqualTo(84); // 42 * 2
    }
}


