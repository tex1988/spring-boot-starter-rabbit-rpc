package io.github.tex1988.boot.rpc.rabbit.integration.config;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.boot.amqp.autoconfigure.RabbitListenerRetrySettingsCustomizer;
import org.springframework.boot.amqp.autoconfigure.RabbitTemplateRetrySettingsCustomizer;
import org.springframework.boot.retry.RetryPolicySettings;
import org.springframework.context.annotation.Bean;

import java.util.List;

abstract class BaseConfig {

    @Bean
    public RabbitListenerRetrySettingsCustomizer customizeListenerRetryPolicy() {
        return BaseConfig::customize;
    }

    @Bean
    public RabbitTemplateRetrySettingsCustomizer customizeTemplateRetryPolicy() {
        return settings -> customize(settings.getRetryPolicySettings());
    }

    private static void customize(RetryPolicySettings settings) {
        settings.setMaxRetries(0L);
        settings.setExceptionExcludes(List.of(
                AmqpRejectAndDontRequeueException.class,
                MessageConversionException.class));
    }
}
