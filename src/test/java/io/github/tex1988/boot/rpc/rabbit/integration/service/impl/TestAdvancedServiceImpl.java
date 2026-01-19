package io.github.tex1988.boot.rpc.rabbit.integration.service.impl;

import io.github.tex1988.boot.rpc.rabbit.annotation.RabbitRpc;
import io.github.tex1988.boot.rpc.rabbit.integration.model.Animal;
import io.github.tex1988.boot.rpc.rabbit.integration.model.Cat;
import io.github.tex1988.boot.rpc.rabbit.integration.model.Dog;
import io.github.tex1988.boot.rpc.rabbit.integration.model.TestUser;
import io.github.tex1988.boot.rpc.rabbit.integration.service.ITestAdvancedService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementation of advanced test service.
 */
@Slf4j
@Service
@RabbitRpc
public class TestAdvancedServiceImpl implements ITestAdvancedService {

    // ==================== METHOD OVERLOADING ====================

    @Override
    public String process(String data) {
        log.info("Processing single string: {}", data);
        return "Processed: " + data;
    }

    @Override
    public String process(String data1, String data2) {
        log.info("Processing two strings: {}, {}", data1, data2);
        return "Processed: " + data1 + " and " + data2;
    }

    @Override
    public String process(String data, Integer number) {
        log.info("Processing string and integer: {}, {}", data, number);
        return "Processed: " + data + " with number " + number;
    }

    @Override
    public String process(Integer number, String data) {
        log.info("Processing integer and string: {}, {}", number, data);
        return "Processed: number " + number + " with " + data;
    }

    // ==================== NULL HANDLING ====================

    @Override
    public String concatenate(String str1, String str2) {
        log.info("Concatenating: {} and {}", str1, str2);
        return (str1 == null ? "null" : str1) + "-" + (str2 == null ? "null" : str2);
    }

    @Override
    public String getUserInfo(TestUser user) {
        log.info("Getting user info for: {}", user);
        if (user == null) {
            return "No user provided";
        }
        return "User: " + user.getName() + " (" + user.getEmail() + ")";
    }

    @Override
    public String buildMessage(String prefix, String content, String suffix) {
        log.info("Building message with prefix={}, content={}, suffix={}", prefix, content, suffix);
        StringBuilder sb = new StringBuilder();
        if (prefix != null) {
            sb.append(prefix);
        }
        if (content != null) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(content);
        }
        if (suffix != null) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(suffix);
        }
        return sb.toString();
    }

    // ==================== POLYMORPHISM ====================

    @Override
    public String identifyAnimal(Animal animal) {
        log.info("Identifying animal: {}", animal);
        if (animal == null) {
            return "No animal provided";
        }

        String type = animal.getClass().getSimpleName();
        String sound = animal.makeSound();
        String species = animal.getSpecies();

        String baseInfo = String.format("%s named '%s' (age %d) says '%s' - Species: %s",
                type, animal.getName(), animal.getAge(), sound, species);

        // Add subtype-specific information
        if (animal instanceof Dog dog) {
            return baseInfo + " - Breed: " + dog.getBreed();
        } else if (animal instanceof Cat cat) {
            return baseInfo + " - Indoor: " + cat.isIndoor();
        }

        return baseInfo;
    }

    @Override
    public Animal createAnimal(String type, Long id, String name, int age) {
        log.info("Creating animal: type={}, id={}, name={}, age={}", type, id, name, age);

        if ("dog".equalsIgnoreCase(type)) {
            return new Dog(id, name, age, "Mixed");
        } else if ("cat".equalsIgnoreCase(type)) {
            return new Cat(id, name, age, true);
        } else {
            throw new IllegalArgumentException("Unknown animal type: " + type);
        }
    }

    @Override
    public String describeAnimal(Animal animal, String additionalInfo) {
        log.info("Describing animal with additional info: {}, {}", animal, additionalInfo);

        if (animal == null) {
            return "No animal to describe" + (additionalInfo != null ? " - " + additionalInfo : "");
        }

        String base = identifyAnimal(animal);
        return additionalInfo != null ? base + " | " + additionalInfo : base;
    }

    // ==================== COMPLEX COMBINATIONS ====================

    @Override
    public String handle(Animal animal) {
        log.info("Handling animal (single param): {}", animal);
        if (animal == null) {
            return "Handled: null animal";
        }
        return "Handled: " + animal.getClass().getSimpleName() + " " + animal.getName();
    }

    @Override
    public String handle(Animal animal, String context) {
        log.info("Handling animal with context: {}, {}", animal, context);
        if (animal == null) {
            return "Handled: null animal in context '" + context + "'";
        }
        return "Handled: " + animal.getClass().getSimpleName() + " " + animal.getName()
                + " in context '" + context + "'";
    }

    @Override
    public String complexOperation(String operation, Animal subject, String details) {
        log.info("Complex operation: operation={}, subject={}, details={}", operation, subject, details);

        StringBuilder result = new StringBuilder("Operation: " + (operation != null ? operation : "none"));

        if (subject != null) {
            result.append(" | Subject: ").append(subject.getClass().getSimpleName())
                    .append(" ").append(subject.getName());
        } else {
            result.append(" | Subject: none");
        }

        if (details != null) {
            result.append(" | Details: ").append(details);
        }

        return result.toString();
    }
}
