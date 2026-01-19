package io.github.tex1988.boot.rpc.rabbit.integration.service;

import io.github.tex1988.boot.rpc.rabbit.annotation.FireAndForget;
import io.github.tex1988.boot.rpc.rabbit.annotation.RabbitRpcInterface;
import io.github.tex1988.boot.rpc.rabbit.integration.model.TestUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;

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
}

