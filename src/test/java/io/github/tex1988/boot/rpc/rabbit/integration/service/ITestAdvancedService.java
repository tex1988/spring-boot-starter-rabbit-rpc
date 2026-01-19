package io.github.tex1988.boot.rpc.rabbit.integration.service;

import io.github.tex1988.boot.rpc.rabbit.annotation.RabbitRpcInterface;
import io.github.tex1988.boot.rpc.rabbit.integration.model.Animal;
import io.github.tex1988.boot.rpc.rabbit.integration.model.TestUser;

/**
 * Test service interface for advanced RPC features:
 * - Method overloading
 * - Null argument handling (without validation)
 * - Polymorphism (subtype/supertype)
 */
@RabbitRpcInterface(
        exchange = "test.advanced.exchange",
        queue = "test.advanced.queue",
        routing = "test.advanced.routing"
)
public interface ITestAdvancedService {

    // ==================== METHOD OVERLOADING ====================

    /**
     * Overloaded method #1: Single String parameter.
     */
    String process(String data);

    /**
     * Overloaded method #2: Two String parameters.
     */
    String process(String data1, String data2);

    /**
     * Overloaded method #3: String and Integer parameters.
     */
    String process(String data, Integer number);

    /**
     * Overloaded method #4: Integer and String parameters (order matters).
     */
    String process(Integer number, String data);

    // ==================== NULL HANDLING (without validation) ====================

    /**
     * Method that accepts nullable string (no @NotNull validation).
     */
    String concatenate(String str1, String str2);

    /**
     * Method that accepts nullable object (no validation).
     */
    String getUserInfo(TestUser user);

    /**
     * Method with multiple nullable parameters.
     */
    String buildMessage(String prefix, String content, String suffix);

    // ==================== POLYMORPHISM ====================

    /**
     * Method accepting supertype (Animal) but can receive subtypes (Dog, Cat).
     */
    String identifyAnimal(Animal animal);

    /**
     * Method returning supertype.
     */
    Animal createAnimal(String type, Long id, String name, int age);

    /**
     * Method with polymorphic parameter and null handling.
     */
    String describeAnimal(Animal animal, String additionalInfo);

    // ==================== COMPLEX COMBINATIONS ====================

    /**
     * Overloaded method with polymorphism #1.
     */
    String handle(Animal animal);

    /**
     * Overloaded method with polymorphism #2.
     */
    String handle(Animal animal, String context);

    /**
     * Method combining all features: overloading, nulls, and polymorphism.
     */
    String complexOperation(String operation, Animal subject, String details);
}
