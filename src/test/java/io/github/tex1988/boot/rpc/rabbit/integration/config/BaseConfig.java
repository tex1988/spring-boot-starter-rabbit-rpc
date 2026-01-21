package io.github.tex1988.boot.rpc.rabbit.integration.config;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.boot.autoconfigure.amqp.RabbitRetryTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.policy.SimpleRetryPolicy;

import java.util.Map;

abstract class BaseConfig {

    @Bean
    public RabbitRetryTemplateCustomizer customizeRetryPolicy() {
        SimpleRetryPolicy policy = new SimpleRetryPolicy(1,
                Map.of(AmqpRejectAndDontRequeueException.class, false,
                        MessageConversionException.class, false),
                true, true);
        return (target, retryTemplate) -> retryTemplate.setRetryPolicy(policy);
    }
}
