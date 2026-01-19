package io.github.tex1988.boot.rpc.rabbit.integration.service.impl;

import io.github.tex1988.boot.rpc.rabbit.annotation.RabbitRpc;
import io.github.tex1988.boot.rpc.rabbit.integration.model.TestUser;
import io.github.tex1988.boot.rpc.rabbit.integration.service.ITestUserService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

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

}

