package io.github.tex1988.boot.rpc.rabbit.integration.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Test model class for integration tests.
 * Demonstrates various validation scenarios.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestUser implements Serializable {

    @NotNull(groups = {OnCreate.class, OnUpdate.class}, message = "ID cannot be null")
    private Long id;

    @NotBlank(groups = {OnCreate.class, OnUpdate.class}, message = "Name cannot be blank")
    @Size(groups = {OnCreate.class, OnUpdate.class}, min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;

    @Email(groups = {OnCreate.class, OnUpdate.class}, message = "Email must be valid")
    @NotBlank(groups = {OnCreate.class, OnUpdate.class}, message = "Email cannot be blank")
    private String email;

    private boolean active;

    public interface OnCreate {
    }

    public interface OnUpdate {
    }
}
