package io.github.tex1988.boot.rpc.rabbit.rabbit;

import com.rabbitmq.client.Channel;
import io.github.tex1988.boot.rpc.rabbit.annotation.FireAndForget;
import io.github.tex1988.boot.rpc.rabbit.constant.ErrorStatusCode;
import io.github.tex1988.boot.rpc.rabbit.exception.RabbitRpcServiceException;
import io.github.tex1988.boot.rpc.rabbit.exception.RabbitRpcServiceValidationException;
import io.github.tex1988.boot.rpc.rabbit.model.ErrorRabbitResponse;
import io.github.tex1988.boot.rpc.rabbit.model.RabbitRpcErrorMapping;
import io.github.tex1988.boot.rpc.rabbit.util.Utils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.messaging.support.MessageBuilder;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.Map;

import static io.github.tex1988.boot.rpc.rabbit.constant.Constants.METHOD_HEADER;
import static io.github.tex1988.boot.rpc.rabbit.constant.Constants.SERVICE_HEADER;
import static io.github.tex1988.boot.rpc.rabbit.constant.Constants.TYPE_ID_HEADER;

/**
 * Handles errors that occur during Rabbit RPC message processing.
 * <p>
 * This class provides a flexible implementation of {@link RabbitListenerErrorHandler} to enable custom error handling
 * logic for RabbitMQ listeners. Errors are categorized based on a mapping of exception types to {@link ErrorStatusCode},
 * or resolved using default behavior when no mapping is provided.
 * <p>
 * The handler supports:
 * <ul>
 *     <li>Validation errors (e.g., {@link RabbitRpcServiceValidationException}), returning {@link ErrorStatusCode#BAD_REQUEST}</li>
 *     <li>Mapped exceptions, returning the associated {@link ErrorStatusCode} as defined in the {@code errorCodes} map</li>
 *     <li>All other exceptions, returning {@link ErrorStatusCode#INTERNAL_SERVER_ERROR} by default</li>
 * </ul>
 *
 * @author tex1988
 * @since 2024-04-12
 */
@Slf4j
@AllArgsConstructor
public class RabbitRpcErrorHandler implements RabbitListenerErrorHandler {

    /**
     * The name of the Rabbit RPC service.
     */
    private final String serviceName;

    /**
     * A mapping of exception types to {@link ErrorStatusCode} for custom error resolution.
     *
     * @see RabbitRpcErrorMapping
     */
    private final RabbitRpcErrorMapping errorCodes;

    /**
     * A mapping between service interfaces and their methods, associated with {@link MethodHandle}s for invocation.
     */
    private final Map<Class<?>, Map<Method, MethodHandle>> methodHandles;

    /**
     * Handles errors that occur during the execution of RabbitMQ message listeners.
     *
     * @param amqpMessage the original AMQP message that caused the error
     * @param message     the converted Spring {@link org.springframework.messaging.Message}
     * @param exception   the exception thrown during message processing
     * @return a {@link Message} containing an {@link ErrorRabbitResponse} as the payload
     */
    @Override
    public Object handleError(Message amqpMessage, org.springframework.messaging.Message<?> message,
                              ListenerExecutionFailedException exception) {
        Throwable cause = exception.getCause();
        ErrorRabbitResponse response;
        String methodName = getHeader(amqpMessage, METHOD_HEADER);
        String className = getHeader(amqpMessage, SERVICE_HEADER);
        if (errorCodes != null && errorCodes.containsKey(cause.getClass())) {
            response = resolveByMapping(cause);
        } else {
            response = resolveByDefault(cause);
        }
        if (response.getStatusCode() == ErrorStatusCode.INTERNAL_SERVER_ERROR.getCode()) {
            log.error("An error occurred during RabbitMQ RPC message processing for class: {}, method: {}()",
                    className, methodName, exception);
        }

        if (isReturn(className, methodName)) {
            return MessageBuilder.withPayload(response)
                    .setHeader(TYPE_ID_HEADER, ErrorRabbitResponse.class.getCanonicalName())
                    .build();
        } else {
            return null;
        }
    }

    /**
     * Handles errors that occur during the execution of RabbitMQ message listeners.
     *
     * @param amqpMessage the original AMQP message that caused the error
     * @param channel     the AMQP channel for manual acks
     * @param message     the converted Spring {@link org.springframework.messaging.Message}
     * @param exception   the exception thrown during message processing
     * @return a {@link Message} containing an {@link ErrorRabbitResponse} as the payload
     */
    @Override
    public Object handleError(Message amqpMessage, Channel channel,
                              org.springframework.messaging.Message<?> message, ListenerExecutionFailedException exception) {
        return handleError(amqpMessage, message, exception);
    }

    private ErrorRabbitResponse resolveByMapping(Throwable exception) {
        return new ErrorRabbitResponse(exception instanceof RabbitRpcServiceException ?
                ((RabbitRpcServiceValidationException) exception).getTimestamp() :
                System.currentTimeMillis(),
                errorCodes.get(exception.getClass()).getCode(),
                serviceName,
                exception.getMessage(),
                exception instanceof RabbitRpcServiceValidationException ?
                        ((RabbitRpcServiceValidationException) exception).getBindingResult() :
                        null);
    }

    private ErrorRabbitResponse resolveByDefault(Throwable exception) {
        if (exception instanceof RabbitRpcServiceValidationException) {
            RabbitRpcServiceValidationException e = (RabbitRpcServiceValidationException) exception;
            return new ErrorRabbitResponse(e.getTimestamp(),
                    e.getStatusCode(),
                    e.getServiceName(),
                    e.getMessage(),
                    e.getBindingResult());
        } else if (exception instanceof RabbitRpcServiceException) {
            RabbitRpcServiceException e = (RabbitRpcServiceException) exception;
            return new ErrorRabbitResponse(e.getTimestamp(),
                    e.getStatusCode(),
                    e.getServiceName(),
                    e.getMessage(),
                    null);
        } else {
            return new ErrorRabbitResponse(System.currentTimeMillis(),
                    ErrorStatusCode.INTERNAL_SERVER_ERROR.getCode(),
                    serviceName,
                    exception.getMessage(),
                    null);
        }
    }

    private boolean isReturn(String className, String methodName) {
        Class<?> iClazz = Utils.getClassByName(this, className);
        Map.Entry<Method, MethodHandle> methodEntry = Utils.getMethodEntry(methodHandles, iClazz, methodName);
        Method method = methodEntry.getKey();
        return !method.isAnnotationPresent(FireAndForget.class);
    }

    private String getHeader(Message amqpMessage, String header) {
        MessageProperties properties = amqpMessage.getMessageProperties();
        if (properties != null) {
            Map<String, Object> headers = properties.getHeaders();
            if (headers != null && !headers.isEmpty()) {
                return (String) headers.get(header);
            }
        }
        return null;
    }
}
