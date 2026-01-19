package io.github.tex1988.boot.rpc.rabbit.integration.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Dog implementation for polymorphism testing.
 */
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Dog extends Animal {

    private String breed;

    public Dog(Long id, String name, int age, String breed) {
        super(id, name, age);
        this.breed = breed;
    }

    @Override
    public String makeSound() {
        return "Woof!";
    }

    @Override
    public String getSpecies() {
        return "Canis familiaris";
    }
}
