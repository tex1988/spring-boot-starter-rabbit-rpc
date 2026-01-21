package io.github.tex1988.boot.rpc.rabbit.integration.service.impl;

import io.github.tex1988.boot.rpc.rabbit.annotation.RabbitRpc;
import io.github.tex1988.boot.rpc.rabbit.integration.explicit.ExplicitlyRegisteredClass;
import io.github.tex1988.boot.rpc.rabbit.integration.model.TestUser;
import io.github.tex1988.boot.rpc.rabbit.integration.service.ITestUserService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Test implementation of ITestUserService for integration testing.
 * Maintains an in-memory store for testing purposes.
 */
@Slf4j
@Service
@RabbitRpc
public class TestUserServiceImpl implements ITestUserService {

    /**
     * -- GETTER --
     * Helper method to get user store for verification.
     */
    @Getter
    private final Map<Long, TestUser> userStore = new ConcurrentHashMap<>();
    private final AtomicLong notificationCounter = new AtomicLong(0);

    public TestUserServiceImpl() {
        // Seed with test data
        userStore.put(1L, new TestUser(1L, "John Doe", "john@example.com", true));
        userStore.put(2L, new TestUser(2L, "Jane Smith", "jane@example.com", true));
        userStore.put(3L, new TestUser(3L, "Bob Wilson", "bob@example.com", false));
    }

    @Override
    public synchronized TestUser getUser(Long id) {
        log.info("Getting user with id: {}", id);
        TestUser user = userStore.get(id);
        if (user == null) {
            throw new RuntimeException("User not found with id: " + id);
        }
        return user;
    }

    @Override
    public synchronized TestUser createUser(TestUser user) {
        log.info("Creating user: {}", user);
        userStore.put(user.getId(), user);
        return user;
    }

    @Override
    public synchronized TestUser updateUser(TestUser user) {
        log.info("Updating user: {}", user);
        if (!userStore.containsKey(user.getId())) {
            throw new RuntimeException("User not found with id: " + user.getId());
        }
        userStore.put(user.getId(), user);
        return user;
    }

    @Override
    public synchronized void deleteUser(Long id) {
        log.info("Deleting user with id: {}", id);
        if (!userStore.containsKey(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        userStore.remove(id);
    }

    @Override
    public synchronized List<TestUser> getAllUsers() {
        log.info("Getting all users, count: {}", userStore.size());
        return new ArrayList<>(userStore.values());
    }

    @Override
    public void notifyUser(Long userId, String message) {
        log.info("Notifying user {} with message: {}", userId, message);
        long count = notificationCounter.incrementAndGet();
        log.info("Total notifications sent: {}", count);
    }

    @Override
    public synchronized TestUser getUserWithError(Long id) {
        log.info("Getting user with error for id: {}", id);
        throw new IllegalStateException("Simulated error for user id: " + id);
    }

    /**
     * Helper method for tests to get notification count.
     */
    public long getNotificationCount() {
        return notificationCounter.get();
    }

    /**
     * Helper method for tests to reset notification count.
     */
    public void resetNotificationCount() {
        notificationCounter.set(0);
    }

    /**
     * Helper method for tests to reset user store to initial state.
     * Thread-safe to handle concurrent test execution.
     */
    public synchronized void resetUserStore() {
        userStore.clear();
        userStore.put(1L, new TestUser(1L, "John Doe", "john@example.com", true));
        userStore.put(2L, new TestUser(2L, "Jane Smith", "jane@example.com", true));
        userStore.put(3L, new TestUser(3L, "Bob Wilson", "bob@example.com", false));
    }

    @Override
    public String[] processArray(String[] input) {
        log.info("Processing String array: {}", java.util.Arrays.toString(input));
        String[] result = new String[input.length];
        for (int i = 0; i < input.length; i++) {
            result[i] = "processed-" + input[i];
        }
        return result;
    }

    @Override
    public int[] processIntArray(int[] input) {
        log.info("Processing int array: {}", java.util.Arrays.toString(input));
        int[] result = new int[input.length];
        for (int i = 0; i < input.length; i++) {
            result[i] = input[i] * 2;
        }
        return result;
    }

    @Override
    public List<String> processList(List<String> input) {
        log.info("Processing List: {}", input);
        return input.stream()
                .map(s -> "processed-" + s)
                .collect(Collectors.toList()); //NOSONAR;
    }

    @Override
    public List<String> processImmutableList(List<String> input) {
        log.info("Processing immutable List: {}", input);
        return input.stream()
                .map(s -> "immutable-" + s)
                .toList();
    }

    @Override
    public Map<String, Integer> processMap(Map<String, Integer> input) {
        log.info("Processing Map: {}", input);
        Map<String, Integer> result = new HashMap<>();
        input.forEach((k, v) -> result.put(k, v + 1));
        return result;
    }

    @Override
    public Map<String, Integer> processImmutableMap(Map<String, Integer> input) {
        log.info("Processing immutable Map: {}", input);
        Map<String, Integer> temp = new HashMap<>();
        input.forEach((k, v) -> temp.put("immutable-" + k, v + 10));
        return Map.copyOf(temp);
    }

    @Override
    public Set<String> processSet(Set<String> input) {
        log.info("Processing Set: {}", input);
        return input.stream()
                .map(s -> "processed-" + s)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> processImmutableSet(Set<String> input) {
        log.info("Processing immutable Set: {}", input);
        Set<String> temp = input.stream()
                .map(s -> "immutable-" + s)
                .collect(Collectors.toSet());
        return Set.copyOf(temp);
    }

    @Override
    public List<Map<String, List<Integer>>> processNestedCollections(List<Map<String, List<Integer>>> input) {
        log.info("Processing nested collections: {}", input);
        return input.stream()
                .map(map -> {
                    Map<String, List<Integer>> newMap = new HashMap<>();
                    map.forEach((k, v) -> {
                        List<Integer> newList = v.stream()
                                .map(i -> i + 1)
                                .toList();
                        newMap.put(k + "-processed", newList);
                    });
                    return newMap;
                })
                .toList();
    }

    @Override
    public Date processDate(Date input) {
        log.info("Processing Date: {}", input);
        // Add one day to the date
        return new Date(input.getTime() + 86400000L); // +1 day in milliseconds
    }

    @Override
    public LocalDate processLocalDate(LocalDate input) {
        log.info("Processing LocalDate: {}", input);
        // Add one day
        return input.plusDays(1);
    }

    @Override
    public LocalDateTime processLocalDateTime(LocalDateTime input) {
        log.info("Processing LocalDateTime: {}", input);
        // Add one hour
        return input.plusHours(1);
    }

    @Override
    public LocalTime processLocalTime(LocalTime input) {
        log.info("Processing LocalTime: {}", input);
        // Add one hour
        return input.plusHours(1);
    }

    @Override
    public Instant processInstant(Instant input) {
        log.info("Processing Instant: {}", input);
        // Add one hour
        return input.plus(Duration.ofHours(1));
    }

    @Override
    public ZonedDateTime processZonedDateTime(ZonedDateTime input) {
        log.info("Processing ZonedDateTime: {}", input);
        // Add one hour
        return input.plusHours(1);
    }

    @Override
    public OffsetDateTime processOffsetDateTime(OffsetDateTime input) {
        log.info("Processing OffsetDateTime: {}", input);
        // Add one hour
        return input.plusHours(1);
    }

    @Override
    public Duration processDuration(Duration input) {
        log.info("Processing Duration: {}", input);
        // Double the duration
        return input.multipliedBy(2);
    }

    @Override
    public Period processPeriod(Period input) {
        log.info("Processing Period: {}", input);
        // Add one day
        return input.plusDays(1);
    }

    @Override
    public ExplicitlyRegisteredClass processExplicitlyRegisteredClass(ExplicitlyRegisteredClass input) {
        log.info("Processing ExplicitlyRegisteredClass: {}", input);
        // Transform the data
        return new ExplicitlyRegisteredClass(
                input.getId(),
                "processed-" + input.getData(),
                input.getValue() * 2
        );
    }
}


