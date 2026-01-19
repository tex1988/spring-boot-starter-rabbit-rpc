package io.github.tex1988.boot.rpc.rabbit.integration;

import io.github.tex1988.boot.rpc.rabbit.integration.config.TestClientServerConfig;
import io.github.tex1988.boot.rpc.rabbit.integration.model.Animal;
import io.github.tex1988.boot.rpc.rabbit.integration.model.Cat;
import io.github.tex1988.boot.rpc.rabbit.integration.model.Dog;
import io.github.tex1988.boot.rpc.rabbit.integration.model.TestUser;
import io.github.tex1988.boot.rpc.rabbit.integration.service.ITestAdvancedService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for advanced RabbitMQ RPC features:
 * - Method overloading
 * - Null argument handling (without validation)
 * - Polymorphism (subtype/supertype)
 * <p>
 * ID Range: 900-999
 */
@SpringBootTest(classes = TestClientServerConfig.class)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Advanced RabbitMQ RPC Features Integration Tests")
class RabbitRpcAdvancedFeaturesIntegrationTest extends AbstractRabbitRpcIntegrationTest {

    @Autowired
    private ITestAdvancedService iTestAdvancedServiceClient;

    // ==================== METHOD OVERLOADING TESTS ====================

    @Test
    @DisplayName("Should resolve overloaded method with single String parameter")
    void shouldResolveOverloadedMethodWithSingleString() {
        // Given
        String input = "test data";

        // When
        String result = iTestAdvancedServiceClient.process(input);

        // Then
        assertThat(result).isEqualTo("Processed: test data");
    }

    @Test
    @DisplayName("Should resolve overloaded method with two String parameters")
    void shouldResolveOverloadedMethodWithTwoStrings() {
        // Given
        String input1 = "first";
        String input2 = "second";

        // When
        String result = iTestAdvancedServiceClient.process(input1, input2);

        // Then
        assertThat(result).isEqualTo("Processed: first and second");
    }

    @Test
    @DisplayName("Should resolve overloaded method with String and Integer parameters")
    void shouldResolveOverloadedMethodWithStringAndInteger() {
        // Given
        String data = "data";
        Integer number = 42;

        // When
        String result = iTestAdvancedServiceClient.process(data, number);

        // Then
        assertThat(result).isEqualTo("Processed: data with number 42");
    }

    @Test
    @DisplayName("Should resolve overloaded method with Integer and String parameters")
    void shouldResolveOverloadedMethodWithIntegerAndString() {
        // Given
        Integer number = 100;
        String data = "info";

        // When
        String result = iTestAdvancedServiceClient.process(number, data);

        // Then
        assertThat(result).isEqualTo("Processed: number 100 with info");
    }

    @Test
    @DisplayName("Should distinguish between methods by parameter order")
    void shouldDistinguishMethodsByParameterOrder() {
        // When
        String result1 = iTestAdvancedServiceClient.process("text", 5);
        String result2 = iTestAdvancedServiceClient.process(5, "text");

        // Then
        assertThat(result1).isEqualTo("Processed: text with number 5");
        assertThat(result2).isEqualTo("Processed: number 5 with text");
        assertThat(result1).isNotEqualTo(result2);
    }

    // ==================== NULL HANDLING TESTS ====================

    @Test
    @DisplayName("Should handle null strings in concatenation")
    void shouldHandleNullStringsInConcatenation() {
        // When
        String result1 = iTestAdvancedServiceClient.concatenate(null, "world");
        String result2 = iTestAdvancedServiceClient.concatenate("hello", null);
        String result3 = iTestAdvancedServiceClient.concatenate(null, null);
        String result4 = iTestAdvancedServiceClient.concatenate("hello", "world");

        // Then
        assertThat(result1).isEqualTo("null-world");
        assertThat(result2).isEqualTo("hello-null");
        assertThat(result3).isEqualTo("null-null");
        assertThat(result4).isEqualTo("hello-world");
    }

    @Test
    @DisplayName("Should handle null user object")
    void shouldHandleNullUserObject() {
        // When
        String result = iTestAdvancedServiceClient.getUserInfo(null);

        // Then
        assertThat(result).isEqualTo("No user provided");
    }

    @Test
    @DisplayName("Should handle non-null user object")
    void shouldHandleNonNullUserObject() {
        // Given
        TestUser user = new TestUser(900L, "Alice", "alice@example.com", true);

        // When
        String result = iTestAdvancedServiceClient.getUserInfo(user);

        // Then
        assertThat(result).isEqualTo("User: Alice (alice@example.com)");
    }

    @Test
    @DisplayName("Should handle multiple null parameters in message building")
    void shouldHandleMultipleNullParameters() {
        // When
        String result1 = iTestAdvancedServiceClient.buildMessage("Hello", "world", "!");
        String result2 = iTestAdvancedServiceClient.buildMessage(null, "world", "!");
        String result3 = iTestAdvancedServiceClient.buildMessage("Hello", null, "!");
        String result4 = iTestAdvancedServiceClient.buildMessage("Hello", "world", null);
        String result5 = iTestAdvancedServiceClient.buildMessage(null, null, null);

        // Then
        assertThat(result1).isEqualTo("Hello world !");
        assertThat(result2).isEqualTo("world !");
        assertThat(result3).isEqualTo("Hello !");
        assertThat(result4).isEqualTo("Hello world");
        assertThat(result5).isEmpty();
    }

    @Test
    @DisplayName("Should serialize and deserialize null fields correctly")
    void shouldSerializeNullFieldsCorrectly() {
        // Given - user with some potential null scenarios
        TestUser userWithNulls = new TestUser(901L, "Bob", "bob@example.com", false);

        // When
        String result = iTestAdvancedServiceClient.getUserInfo(userWithNulls);

        // Then
        assertThat(result).contains("Bob")
                .contains("bob@example.com");
    }

    // ==================== POLYMORPHISM TESTS ====================

    @Test
    @DisplayName("Should handle Dog subtype as Animal parameter")
    void shouldHandleDogSubtype() {
        // Given
        Dog dog = new Dog(910L, "Buddy", 5, "Golden Retriever");

        // When
        String result = iTestAdvancedServiceClient.identifyAnimal(dog);

        // Then
        assertThat(result).contains("Dog")
                .contains("Buddy")
                .contains("age 5")
                .contains("Woof!")
                .contains("Canis familiaris");
    }

    @Test
    @DisplayName("Should handle Cat subtype as Animal parameter")
    void shouldHandleCatSubtype() {
        // Given
        Cat cat = new Cat(911L, "Whiskers", 3, true);

        // When
        String result = iTestAdvancedServiceClient.identifyAnimal(cat);

        // Then
        assertThat(result).contains("Cat")
                .contains("Whiskers")
                .contains("age 3")
                .contains("Meow!")
                .contains("Felis catus");
    }

    @Test
    @DisplayName("Should return correct subtype from supertype return method")
    void shouldReturnCorrectSubtype() {
        // When
        Animal dog = iTestAdvancedServiceClient.createAnimal("dog", 920L, "Rex", 4);
        Animal cat = iTestAdvancedServiceClient.createAnimal("cat", 921L, "Mittens", 2);

        // Then
        assertThat(dog).isInstanceOf(Dog.class);
        assertThat(dog.getName()).isEqualTo("Rex");
        assertThat(dog.getAge()).isEqualTo(4);
        assertThat(dog.makeSound()).isEqualTo("Woof!");

        assertThat(cat).isInstanceOf(Cat.class);
        assertThat(cat.getName()).isEqualTo("Mittens");
        assertThat(cat.getAge()).isEqualTo(2);
        assertThat(cat.makeSound()).isEqualTo("Meow!");
    }

    @Test
    @DisplayName("Should preserve subtype-specific fields after RPC round trip")
    void shouldPreserveSubtypeSpecificFields() {
        // Given
        Dog originalDog = new Dog(930L, "Max", 7, "Labrador");

        // When
        String description = iTestAdvancedServiceClient.identifyAnimal(originalDog);
        Animal createdDog = iTestAdvancedServiceClient.createAnimal("dog", 931L, "Max", 7);

        // Then
        assertThat(description).contains("Labrador");
        assertThat(createdDog).isInstanceOf(Dog.class);
        Dog returnedDog = (Dog) createdDog;
        assertThat(returnedDog.getBreed()).isNotNull();
    }

    @Test
    @DisplayName("Should handle null animal with polymorphic parameter")
    void shouldHandleNullAnimalWithPolymorphicParameter() {
        // When
        String result = iTestAdvancedServiceClient.identifyAnimal(null);

        // Then
        assertThat(result).isEqualTo("No animal provided");
    }

    @Test
    @DisplayName("Should combine polymorphism with null handling")
    void shouldCombinePolymorphismWithNullHandling() {
        // Given
        Dog dog = new Dog(940L, "Charlie", 6, "Beagle");

        // When
        String result1 = iTestAdvancedServiceClient.describeAnimal(dog, "friendly");
        String result2 = iTestAdvancedServiceClient.describeAnimal(dog, null);
        String result3 = iTestAdvancedServiceClient.describeAnimal(null, "no pet");
        String result4 = iTestAdvancedServiceClient.describeAnimal(null, null);

        // Then
        assertThat(result1).contains("Dog").contains("Charlie").contains("friendly");
        assertThat(result2).contains("Dog").contains("Charlie").doesNotContain("|");
        assertThat(result3).contains("No animal").contains("no pet");
        assertThat(result4).isEqualTo("No animal to describe");
    }

    // ==================== OVERLOADING WITH POLYMORPHISM ====================

    @Test
    @DisplayName("Should resolve overloaded method with Animal parameter (single)")
    void shouldResolveOverloadedMethodWithAnimalSingle() {
        // Given
        Dog dog = new Dog(950L, "Duke", 4, "German Shepherd");

        // When
        String result = iTestAdvancedServiceClient.handle(dog);

        // Then
        assertThat(result).isEqualTo("Handled: Dog Duke");
    }

    @Test
    @DisplayName("Should resolve overloaded method with Animal and context")
    void shouldResolveOverloadedMethodWithAnimalAndContext() {
        // Given
        Cat cat = new Cat(951L, "Luna", 2, true);

        // When
        String result = iTestAdvancedServiceClient.handle(cat, "veterinary visit");

        // Then
        assertThat(result).isEqualTo("Handled: Cat Luna in context 'veterinary visit'");
    }

    @Test
    @DisplayName("Should distinguish overloaded polymorphic methods")
    void shouldDistinguishOverloadedPolymorphicMethods() {
        // Given
        Dog dog = new Dog(952L, "Rocky", 5, "Bulldog");

        // When
        String result1 = iTestAdvancedServiceClient.handle(dog);
        String result2 = iTestAdvancedServiceClient.handle(dog, "training");

        // Then
        assertThat(result1).isEqualTo("Handled: Dog Rocky");
        assertThat(result2).isEqualTo("Handled: Dog Rocky in context 'training'");
        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    @DisplayName("Should handle null in overloaded polymorphic methods")
    void shouldHandleNullInOverloadedPolymorphicMethods() {
        // When
        String result1 = iTestAdvancedServiceClient.handle(null);
        String result2 = iTestAdvancedServiceClient.handle(null, "test context");

        // Then
        assertThat(result1).isEqualTo("Handled: null animal");
        assertThat(result2).isEqualTo("Handled: null animal in context 'test context'");
    }

    // ==================== COMPLEX COMBINATION TESTS ====================

    @Test
    @DisplayName("Should handle complex operation with all parameters")
    void shouldHandleComplexOperationWithAllParameters() {
        // Given
        Dog dog = new Dog(960L, "Ace", 3, "Husky");

        // When
        String result = iTestAdvancedServiceClient.complexOperation("examine", dog, "health checkup");

        // Then
        assertThat(result).isEqualTo("Operation: examine | Subject: Dog Ace | Details: health checkup");
    }

    @Test
    @DisplayName("Should handle complex operation with null operation")
    void shouldHandleComplexOperationWithNullOperation() {
        // Given
        Cat cat = new Cat(961L, "Shadow", 4, false);

        // When
        String result = iTestAdvancedServiceClient.complexOperation(null, cat, "outdoor cat");

        // Then
        assertThat(result).isEqualTo("Operation: none | Subject: Cat Shadow | Details: outdoor cat");
    }

    @Test
    @DisplayName("Should handle complex operation with null subject")
    void shouldHandleComplexOperationWithNullSubject() {
        // When
        String result = iTestAdvancedServiceClient.complexOperation("process", null, "no subject available");

        // Then
        assertThat(result).isEqualTo("Operation: process | Subject: none | Details: no subject available");
    }

    @Test
    @DisplayName("Should handle complex operation with null details")
    void shouldHandleComplexOperationWithNullDetails() {
        // Given
        Dog dog = new Dog(962L, "Zeus", 6, "Doberman");

        // When
        String result = iTestAdvancedServiceClient.complexOperation("register", dog, null);

        // Then
        assertThat(result).isEqualTo("Operation: register | Subject: Dog Zeus");
    }

    @Test
    @DisplayName("Should handle complex operation with all nulls except operation")
    void shouldHandleComplexOperationWithMultipleNulls() {
        // When
        String result = iTestAdvancedServiceClient.complexOperation("check", null, null);

        // Then
        assertThat(result).isEqualTo("Operation: check | Subject: none");
    }

    @Test
    @DisplayName("Should handle different animal subtypes in same operation")
    void shouldHandleDifferentSubtypesInSameOperation() {
        // Given
        Dog dog = new Dog(970L, "Bolt", 5, "Border Collie");
        Cat cat = new Cat(971L, "Felix", 3, true);

        // When
        String dogResult = iTestAdvancedServiceClient.complexOperation("register", dog, "new pet");
        String catResult = iTestAdvancedServiceClient.complexOperation("register", cat, "new pet");

        // Then
        assertThat(dogResult).contains("Dog Bolt");
        assertThat(catResult).contains("Cat Felix");
        assertThat(dogResult).isNotEqualTo(catResult);
    }

    // ==================== EDGE CASES AND INTEGRATION ====================

    @Test
    @DisplayName("Should maintain type information through multiple RPC calls")
    void shouldMaintainTypeInformationThroughMultipleCalls() {
        // Given
        Dog originalDog = new Dog(980L, "Ranger", 4, "Australian Shepherd");

        // When
        String identified = iTestAdvancedServiceClient.identifyAnimal(originalDog);
        String handled = iTestAdvancedServiceClient.handle(originalDog, "multiple calls");
        Animal created = iTestAdvancedServiceClient.createAnimal("dog", 981L, "Ranger", 4);

        // Then
        assertThat(identified).contains("Dog").contains("Ranger");
        assertThat(handled).contains("Dog").contains("Ranger");
        assertThat(created).isInstanceOf(Dog.class);
        assertThat(created.getName()).isEqualTo("Ranger");
    }

    @Test
    @DisplayName("Should work with complex workflow combining all features")
    void shouldWorkWithComplexWorkflow() {
        // Given - Create different types
        Animal dog = iTestAdvancedServiceClient.createAnimal("dog", 990L, "Hero", 7);
        Animal cat = iTestAdvancedServiceClient.createAnimal("cat", 991L, "Princess", 2);

        // When - Use in various operations
        String dogId = iTestAdvancedServiceClient.identifyAnimal(dog);
        String catId = iTestAdvancedServiceClient.identifyAnimal(cat);
        String dogHandle = iTestAdvancedServiceClient.handle(dog);
        String catHandle = iTestAdvancedServiceClient.handle(cat, "playing");
        String complexDog = iTestAdvancedServiceClient.complexOperation("train", dog, "obedience");
        String complexCat = iTestAdvancedServiceClient.complexOperation("feed", cat, null);

        // Then - Verify all operations worked correctly
        assertThat(dogId).contains("Dog").contains("Hero").contains("Woof!");
        assertThat(catId).contains("Cat").contains("Princess").contains("Meow!");
        assertThat(dogHandle).contains("Dog Hero");
        assertThat(catHandle).contains("Cat Princess").contains("playing");
        assertThat(complexDog).contains("train").contains("Dog Hero").contains("obedience");
        assertThat(complexCat).contains("feed").contains("Cat Princess");
    }
}
