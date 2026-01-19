package io.github.tex1988.boot.rpc.rabbit.integration.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Test model for polymorphism testing.
 * Abstract base class for different animal types.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class Animal implements Serializable {

    private Long id;
    private String name;
    private int age;

    /**
     * Abstract method to get the sound the animal makes.
     */
    public abstract String makeSound();

    /**
     * Get species name.
     */
    public abstract String getSpecies();
}
