package io.github.tex1988.boot.rpc.rabbit.integration.service;

import io.github.tex1988.boot.rpc.rabbit.annotation.FireAndForget;
import io.github.tex1988.boot.rpc.rabbit.annotation.RabbitRpcInterface;
import io.github.tex1988.boot.rpc.rabbit.integration.explicit.ExplicitlyRegisteredClass;
import io.github.tex1988.boot.rpc.rabbit.integration.model.TestUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test RPC service interface for user operations.
 * Demonstrates various RPC patterns including validation and fire-and-forget.
 */
@RabbitRpcInterface(
        exchange = "test.user.exchange",
        queue = "test.user.queue",
        routing = "test.user.routing"
)
public interface ITestUserService {

    /**
     * Get a user by ID with validation on the ID parameter.
     */
    TestUser getUser(@NotNull @Min(1) @Max(1000) Long id);

    /**
     * Create a new user with validation groups.
     */
    TestUser createUser(@Validated(TestUser.OnCreate.class) TestUser user);

    /**
     * Update an existing user with validation groups.
     */
    TestUser updateUser(@Validated(TestUser.OnUpdate.class) TestUser user);

    /**
     * Delete a user by ID (synchronous void method).
     */
    void deleteUser(@NotNull Long id);

    /**
     * Get all users (tests list serialization).
     */
    List<TestUser> getAllUsers();

    /**
     * Async notification method (fire-and-forget).
     */
    @FireAndForget
    void notifyUser(@NotNull Long userId, String message);

    /**
     * Method that throws an exception to test error handling.
     */
    TestUser getUserWithError(@NotNull Long id);

    /**
     * Test array serialization.
     */
    String[] processArray(String[] input);

    /**
     * Test int array serialization.
     */
    int[] processIntArray(int[] input);

    /**
     * Test List serialization (mutable).
     */
    List<String> processList(List<String> input);

    /**
     * Test immutable List serialization (created with List.of()).
     */
    List<String> processImmutableList(List<String> input);

    /**
     * Test Map serialization (mutable).
     */
    Map<String, Integer> processMap(Map<String, Integer> input);

    /**
     * Test immutable Map serialization (created with Map.of()).
     */
    Map<String, Integer> processImmutableMap(Map<String, Integer> input);

    /**
     * Test Set serialization (mutable).
     */
    Set<String> processSet(Set<String> input);

    /**
     * Test immutable Set serialization (created with Set.of()).
     */
    Set<String> processImmutableSet(Set<String> input);

    /**
     * Test nested collections.
     */
    List<Map<String, List<Integer>>> processNestedCollections(List<Map<String, List<Integer>>> input);

    /**
     * Test java.util.Date serialization.
     */
    Date processDate(Date input);

    /**
     * Test LocalDate serialization.
     */
    LocalDate processLocalDate(LocalDate input);

    /**
     * Test LocalDateTime serialization.
     */
    LocalDateTime processLocalDateTime(LocalDateTime input);

    /**
     * Test LocalTime serialization.
     */
    LocalTime processLocalTime(LocalTime input);

    /**
     * Test Instant serialization.
     */
    Instant processInstant(Instant input);

    /**
     * Test ZonedDateTime serialization.
     */
    ZonedDateTime processZonedDateTime(ZonedDateTime input);

    /**
     * Test OffsetDateTime serialization.
     */
    OffsetDateTime processOffsetDateTime(OffsetDateTime input);

    /**
     * Test Duration serialization.
     */
    Duration processDuration(Duration input);

    /**
     * Test Period serialization.
     */
    Period processPeriod(Period input);

    /**
     * Test explicitly registered class (by full name, not pattern).
     */
    ExplicitlyRegisteredClass processExplicitlyRegisteredClass(ExplicitlyRegisteredClass input);
}

