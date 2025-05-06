package io.github.tex1988.boot.rpc.rabbit.converter;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.Registration;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;
import com.esotericsoftware.kryo.kryo5.serializers.DefaultSerializers;
import com.esotericsoftware.kryo.kryo5.serializers.JavaSerializer;
import com.esotericsoftware.kryo.kryo5.util.Pool;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.AllowedListDeserializingMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.lang.NonNull;

import java.net.URI;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * A custom message converter for RabbitMQ that uses Kryo5 for serialization and deserialization.
 * This converter supports efficient object serialization and deserialization with optional class registration.
 * Fully thread-safe.
 *
 * @author tex1988
 * @since 2025-05-06
 */
public class Kryo5MessageConverter extends AllowedListDeserializingMessageConverter {

    private static final int CRYO_PULL_SIZE = 1024;
    private static final int IO_PULL_SIZE = 512;
    private static final int BUFFER_SIZE = 8192;
    private static final int MAX_BUFFER_SIZE = -1;
    private static final String CONTENT_TYPE = "application/x-kryo5";
    private static final String DEFAULT_CHARSET = "UTF-8";

    private final boolean registrationRequired;

    public Kryo5MessageConverter() {
        this(false);
    }

    public Kryo5MessageConverter(boolean registrationRequired) {
        this.registrationRequired = registrationRequired;
    }

    private final Pool<Kryo> kryoPool = new Pool<>(true, false, CRYO_PULL_SIZE) {

        @Override
        protected Kryo create() {
            Kryo kryo = new Kryo();
            kryo.setRegistrationRequired(registrationRequired);
            kryo.setReferences(false);
            kryo.addDefaultSerializer(Throwable.class, new JavaSerializer());
            kryo.addDefaultSerializer(UUID.class, new DefaultSerializers.UUIDSerializer());
            kryo.addDefaultSerializer(URI.class, new DefaultSerializers.URISerializer());
            kryo.addDefaultSerializer(Pattern.class, new DefaultSerializers.PatternSerializer());
            return kryo;
        }
    };

    private final Pool<Input> inputPool = new Pool<>(true, false, IO_PULL_SIZE) {

        @Override
        protected Input create() {
            return new Input(BUFFER_SIZE);
        }
    };

    private final Pool<Output> outputPool = new Pool<>(true, false, IO_PULL_SIZE) {

        @Override
        protected Output create() {
            return new Output(BUFFER_SIZE, MAX_BUFFER_SIZE);
        }
    };

    @Override
    protected Message createMessage(@NonNull Object object, @NonNull MessageProperties messageProperties) {
        Kryo kryo = kryoPool.obtain();
        Output output = outputPool.obtain();
        try {
            kryo.writeClassAndObject(output, object);
            byte[] bytes = output.toBytes();
            messageProperties.setContentType(CONTENT_TYPE);
            if (messageProperties.getContentEncoding() == null) {
                messageProperties.setContentEncoding(DEFAULT_CHARSET);
            }
            return new Message(bytes, messageProperties);
        } catch (Exception e) {
            throw new MessageConversionException("Failed to convert object to message", e);
        } finally {
            kryoPool.free(kryo);
            outputPool.free(output);
        }
    }

    @Override
    public Object fromMessage(@NonNull Message message) throws MessageConversionException {
        Kryo kryo = kryoPool.obtain();
        Input input = inputPool.obtain();
        try {
            MessageProperties properties = message.getMessageProperties();
            validateProperties(properties);
            input.setBuffer(message.getBody());
            checkAllowedList(kryo, input);
            return kryo.readClassAndObject(input);
        } catch (Exception e) {
            throw new MessageConversionException("Failed to convert message", e);
        } finally {
            kryoPool.free(kryo);
            inputPool.free(input);
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

    private void checkAllowedList(Kryo kryo, Input input) {
        Registration registration = kryo.readClass(input);
        checkAllowedList(registration.getType());
        input.setPosition(0);
    }
}
