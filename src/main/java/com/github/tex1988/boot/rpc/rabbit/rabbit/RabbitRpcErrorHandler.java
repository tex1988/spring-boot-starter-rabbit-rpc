package com.github.tex1988.boot.rpc.rabbit.rabbit;

import com.github.tex1988.boot.rpc.rabbit.constant.ErrorStatusCode;
import com.github.tex1988.boot.rpc.rabbit.exception.RabbitRpcServiceException;
import com.github.tex1988.boot.rpc.rabbit.exception.RabbitRpcServiceValidationException;
import com.github.tex1988.boot.rpc.rabbit.model.ErrorRabbitResponse;
import com.github.tex1988.boot.rpc.rabbit.model.RabbitRpcErrorMapping;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.messaging.support.MessageBuilder;

import static com.github.tex1988.boot.rpc.rabbit.constant.Constants.TYPE_ID_HEADER;

/**
 * Handles errors that occur during Rabbit RPC message processing.
 * <p>
 * This class provides a flexible implementation of {@link RabbitListenerErrorHandler} to enable custom error handling
 * logic for RabbitMQ listeners. Errors are categorized based on a mapping of exception types to {@link ErrorStatusCode},
 * or resolved using default behavior when no mapping is provided.
 * </p>
 * <p>
 * The handler supports:
 * <ul>
 *     <li>Validation errors (e.g., {@link RabbitRpcServiceValidationException}), returning {@link ErrorStatusCode#BAD_REQUEST}.</li>
 *     <li>Mapped exceptions, returning the associated {@link ErrorStatusCode} as defined in the {@code errorCodes} map.</li>
 *     <li>All other exceptions, returning {@link ErrorStatusCode#INTERNAL_SERVER_ERROR} by default.</li>
 * </ul>
 * </p>
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
        if (errorCodes != null && errorCodes.containsKey(cause.getClass())) {
            response = resolveByMapping(cause);
        } else {
            response = resolveByDefault(cause);
        }
        if (response.getStatusCode() == ErrorStatusCode.INTERNAL_SERVER_ERROR.getCode()) {
            log.error("An error occurred during RabbitMQ RPC message processing", exception);
        }
        return MessageBuilder.withPayload(response)
                .setHeader(TYPE_ID_HEADER, ErrorRabbitResponse.class.getCanonicalName())
                .build();
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
        return switch (exception) {
            case RabbitRpcServiceValidationException e -> new ErrorRabbitResponse(e.getTimestamp(),
                    e.getStatusCode(),
                    e.getServiceName(),
                    e.getMessage(),
                    e.getBindingResult());
            case RabbitRpcServiceException e -> new ErrorRabbitResponse(e.getTimestamp(),
                    e.getStatusCode(),
                    e.getServiceName(),
                    e.getMessage(),
                    null);
            default -> new ErrorRabbitResponse(System.currentTimeMillis(),
                    ErrorStatusCode.INTERNAL_SERVER_ERROR.getCode(),
                    serviceName,
                    exception.getCause().getMessage(),
                    null);
        };
    }
}
