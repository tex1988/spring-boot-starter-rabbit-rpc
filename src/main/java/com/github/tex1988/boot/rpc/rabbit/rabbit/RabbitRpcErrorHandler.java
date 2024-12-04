package com.github.tex1988.boot.rpc.rabbit.rabbit;

import com.github.tex1988.boot.rpc.rabbit.model.ErrorRabbitResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.support.MessageBuilder;

import java.util.stream.Collectors;

import static com.github.tex1988.boot.rpc.rabbit.constant.Constants.TYPE_ID_HEADER;

/**
 * Handles errors that occur during Rabbit RPC message processing.
 * <p>
 * This class provides basic implementation of {@link RabbitListenerErrorHandler} to provide custom error handling logic
 * for RabbitMQ listeners. Errors are categorized, and appropriate error responses are generated
 * and sent back to the client.
 * </p>
 * <p>
 * The handler supports:
 * <ul>
 *     <li>Validation errors (e.g., {@link MethodArgumentNotValidException}), returning 400 status codes.</li>
 *     <li>All other exceptions, returning 500 status codes.</li>
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
     * Handles errors that occur during the execution of RabbitMQ message listeners.
     *
     * @param amqpMessage the original AMQP message that caused the error
     * @param message     the converted Spring {@link org.springframework.messaging.Message}
     * @param exception   the exception thrown during message processing
     * @return a {@link Message} containing an {@link ErrorRabbitResponse} as the payload
     */
    @Override
    public Object handleError(Message amqpMessage, org.springframework.messaging.Message<?> message, ListenerExecutionFailedException exception) {

        // Construct an error response based on the type of exception
        ErrorRabbitResponse response = switch (exception.getCause()) {
            case MethodArgumentNotValidException e -> new ErrorRabbitResponse(System.currentTimeMillis(),
                    400,
                    serviceName,
                    e.getBindingResult() != null ? e.getBindingResult().getAllErrors().stream()
                            .map(DefaultMessageSourceResolvable::getDefaultMessage)
                            .collect(Collectors.joining("\n")) : null);
            default -> {
                log.error("An error occurred during message processing", exception);
                yield new ErrorRabbitResponse(System.currentTimeMillis(),
                        500,
                        serviceName,
                        exception.getCause().getMessage());
            }
        };

        // Build and return a response message with the error details
        return MessageBuilder.withPayload(response)
                .setHeader(TYPE_ID_HEADER, ErrorRabbitResponse.class.getCanonicalName())
                .build();
    }
}
