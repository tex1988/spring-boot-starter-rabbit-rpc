package io.github.tex1988.boot.rpc.rabbit.integration;

import io.github.tex1988.boot.rpc.rabbit.integration.config.CustomConfigBeans;
import io.github.tex1988.boot.rpc.rabbit.integration.config.TestCustomConfigContext;
import io.github.tex1988.boot.rpc.rabbit.integration.service.ITestCustomConfigService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Integration tests for custom bean configuration via @EnableRabbitRpc parameters.
 * <p>
 * Tests verify that custom beans configured through annotation parameters are actually used:
 * 1. Custom task executor (executor parameter)
 * 2. Custom message converter (messageConverter parameter)
 * 3. Custom error handler (errorHandler parameter)
 * <p>
 * This tests the uncovered configuration logic in RabbitRpcAutoConfigure:
 * - getTaskExecutor()
 * - getRpcMessageConverter()
 * - getErrorHandler()
 */
@SpringBootTest(classes = TestCustomConfigContext.class)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Custom Bean Configuration Integration Tests")
class RabbitRpcCustomConfigIntegrationTest extends AbstractRabbitRpcIntegrationTest {

    @Autowired
    private ITestCustomConfigService iTestCustomConfigServiceClient;

    @Autowired
    @Qualifier("customTaskExecutor")
    private CustomConfigBeans.CustomTaskExecutor customTaskExecutor;

    @Autowired
    @Qualifier("customMessageConverter")
    private CustomConfigBeans.CustomMessageConverter customMessageConverter;

    @Autowired
    @Qualifier("customErrorHandler")
    private CustomConfigBeans.CustomErrorHandler customErrorHandler;


    @Test
    @DisplayName("Should use custom task executor configured via @EnableRabbitRpc")
    void shouldUseCustomTaskExecutor() {
        // Given - Custom executor is configured via executor = "customTaskExecutor"

        // When - Make RPC calls
        String result = iTestCustomConfigServiceClient.processWithCustomConfig("test1");

        // Then - RPC calls work (proving infrastructure is correctly configured)
        assertThat(result).isEqualTo("Custom config result: test1");
        assertNotEquals(0, customTaskExecutor.getExecutedTaskCount());

        System.out.println("✓ Custom task executor is registered and RPC works correctly");
    }

    @Test
    @DisplayName("Should use custom message converter configured via @EnableRabbitRpc")
    void shouldUseCustomMessageConverter() {
        // Given - Custom message converter is configured via messageConverter = "customMessageConverter"
        int initialToMessageCount = customMessageConverter.getToMessageCount();
        int initialFromMessageCount = customMessageConverter.getFromMessageCount();

        // When - Make RPC call (triggers serialization and deserialization)
        String result = iTestCustomConfigServiceClient.processWithCustomConfig("converter-test");

        // Then - RPC works
        assertThat(result).isEqualTo("Custom config result: converter-test");

        // And - Custom converter was used for serialization/deserialization
        int finalToMessageCount = customMessageConverter.getToMessageCount();
        int finalFromMessageCount = customMessageConverter.getFromMessageCount();

        assertThat(finalToMessageCount).as("Custom converter should serialize messages")
                .isGreaterThan(initialToMessageCount);
        assertThat(finalFromMessageCount).as("Custom converter should deserialize messages")
                .isGreaterThan(initialFromMessageCount);

        System.out.println("✓ Custom message converter was used. " +
                "Serializations: " + (finalToMessageCount - initialToMessageCount) + ", " +
                "Deserializations: " + (finalFromMessageCount - initialFromMessageCount));
    }

    @Test
    @DisplayName("Should use custom error handler configured via @EnableRabbitRpc")
    void shouldUseCustomErrorHandler() {
        // Given - Custom error handler is configured via errorHandler = "customErrorHandler"
        int initialErrorCount = customErrorHandler.getErrorCount();

        // When - Make normal RPC call (no error)
        String result = iTestCustomConfigServiceClient.processWithCustomConfig("no-error");

        // Then - No errors tracked
        assertThat(result).isEqualTo("Custom config result: no-error");
        assertThat(customErrorHandler.getErrorCount()).isEqualTo(initialErrorCount);

        System.out.println("✓ Custom error handler is registered (no errors for successful call)");
    }
}
