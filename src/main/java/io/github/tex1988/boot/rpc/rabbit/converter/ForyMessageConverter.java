package io.github.tex1988.boot.rpc.rabbit.converter;

import lombok.SneakyThrows;
import org.apache.fory.Fory;
import org.apache.fory.ThreadSafeFory;
import org.apache.fory.config.CompatibleMode;
import org.apache.fory.config.ForyBuilder;
import org.apache.fory.config.Language;
import org.apache.fory.config.UnknownEnumValueStrategy;
import org.apache.fory.logging.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.AbstractMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.lang.NonNull;

import java.util.List;

/**
 * A custom message converter for RabbitMQ that uses Apache Fory (Fury) for serialization and deserialization.
 * This converter supports efficient object serialization and deserialization with optional class registration.
 * Fully thread-safe.
 *
 * @author tex1988
 * @since 2025-05-06
 */
public class ForyMessageConverter extends AbstractMessageConverter {

    private static final int DEFAULT_POOL_SIZE = 16;
    private static final String CONTENT_TYPE = "application/fory";
    private static final String DEFAULT_CHARSET = "UTF-8";

    private final ThreadSafeFory fory;

    static {
        LoggerFactory.useSlf4jLogging(true);
        LoggerFactory.setLogLevel(0);
    }

    public ForyMessageConverter() {
        this(DEFAULT_POOL_SIZE, null);
    }

    public ForyMessageConverter(List<String> allowedListClasses) {
        this(DEFAULT_POOL_SIZE, allowedListClasses);
    }

    public ForyMessageConverter(int poolSize) {
        this(poolSize, null);
    }

    @SneakyThrows
    public ForyMessageConverter(int poolSize, List<String> allowedListClasses) {
        ForyBuilder builder = Fory.builder()
                .withLanguage(Language.JAVA)
                .withRefTracking(false)
                .withCompatibleMode(CompatibleMode.COMPATIBLE)
                .withUnknownEnumValueStrategy(UnknownEnumValueStrategy.RETURN_NULL)
                .withAsyncCompilation(true);
        boolean isRegistrationRequired = allowedListClasses != null && !allowedListClasses.isEmpty();
        builder.requireClassRegistration(isRegistrationRequired);
        fory = builder.buildThreadSafeForyPool(poolSize);
        if (isRegistrationRequired) {
            for (String className : allowedListClasses) {
                Class<?> clazz = Class.forName(className);
                fory.register(clazz, className);
            }
        }
    }

    @Override
    protected @NonNull Message createMessage(@NonNull Object object, @NonNull MessageProperties messageProperties) {
        try {
            byte[] bytes = fory.serialize(object);
            messageProperties.setContentType(CONTENT_TYPE);

            if (messageProperties.getContentEncoding() == null) {
                messageProperties.setContentEncoding(DEFAULT_CHARSET);
            }
            return new Message(bytes, messageProperties);
        } catch (Exception e) {
            throw new MessageConversionException("Failed to convert object to message", e);
        }
    }

    @Override
    public @NonNull Object fromMessage(@NonNull Message message) throws MessageConversionException {
        try {
            MessageProperties properties = message.getMessageProperties();
            validateProperties(properties);
            return fory.deserialize(message.getBody());
        } catch (Exception e) {
            throw new MessageConversionException("Failed to convert message", e);
        }
    }

    private void validateProperties(MessageProperties properties) {
        if (properties == null || properties.getContentType() == null) {
            throw new IllegalArgumentException("Invalid message properties: " + properties);
        }
        if (!properties.getContentType().equals(CONTENT_TYPE)) {
            throw new IllegalArgumentException("Invalid message content type: " + properties.getContentType());
        }
    }
}
