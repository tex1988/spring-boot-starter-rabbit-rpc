package io.github.tex1988.boot.rpc.rabbit.example.common.config;

import io.github.tex1988.boot.rpc.rabbit.example.common.config.properties.RabbitRpcProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.RabbitRetryTemplateCustomizer;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.retry.policy.SimpleRetryPolicy;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Map;

@Configuration
@PropertySource("classpath:application-rabbit-rpc.properties")
@ConfigurationPropertiesScan("io.github.tex1988.boot.rpc.rabbit.example.common.config.properties")
@RequiredArgsConstructor
public class DefaultConfig {

    private final RabbitRpcProperties rabbitRpcProperties;

    @Value("${spring.rabbitmq.listener.simple.retry.max-attempts}")
    private int maxRetryAttempts;

    @Bean
    public RabbitRetryTemplateCustomizer customizeRetryPolicy() {
        SimpleRetryPolicy policy = new SimpleRetryPolicy(maxRetryAttempts,
                Map.of(AmqpRejectAndDontRequeueException.class, false,
                        MessageConversionException.class, false,
                        SQLIntegrityConstraintViolationException.class, false),
                true, true);
        return (target, retryTemplate) -> retryTemplate.setRetryPolicy(policy);
    }

    @Bean
    public RabbitRpcProperties properties() {
        return rabbitRpcProperties;
    }
}
