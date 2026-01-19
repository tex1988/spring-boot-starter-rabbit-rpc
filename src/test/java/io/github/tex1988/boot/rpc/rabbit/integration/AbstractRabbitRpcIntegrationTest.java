package io.github.tex1988.boot.rpc.rabbit.integration;

import lombok.Getter;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Base class for all RabbitMQ RPC integration tests.
 * Provides common setup for all integration tests.
 */
@Testcontainers
public abstract class AbstractRabbitRpcIntegrationTest {

    private static final String RABBITMQ_IMAGE = "rabbitmq:4.2";

    @Getter
    @Container
    protected static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(DockerImageName.parse(RABBITMQ_IMAGE))
            .withExposedPorts(5672)
            .waitingFor(Wait.forListeningPort())
            .waitingFor(Wait.forLogMessage(".*Time to start RabbitMQ.*", 1)
                    .withStartupTimeout(Duration.ofSeconds(120)));

    @DynamicPropertySource
    static void configureRabbitMq(DynamicPropertyRegistry registry) {
        if (rabbitMQContainer != null && rabbitMQContainer.isRunning()) {
            registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
            registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort);
            registry.add("spring.rabbitmq.username", rabbitMQContainer::getAdminUsername);
            registry.add("spring.rabbitmq.password", rabbitMQContainer::getAdminPassword);
        }
    }
}

