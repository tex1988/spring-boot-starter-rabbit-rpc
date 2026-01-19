package io.github.tex1988.boot.rpc.rabbit.integration.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Cat implementation for polymorphism testing.
 */
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Cat extends Animal {

    private boolean indoor;

    public Cat(Long id, String name, int age, boolean indoor) {
        super(id, name, age);
        this.indoor = indoor;
    }

    @Override
    public String makeSound() {
        return "Meow!";
    }

    @Override
    public String getSpecies() {
        return "Felis catus";
    }
}
