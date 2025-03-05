package io.github.tex1988.boot.rpc.rabbit.exception;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Exception class representing validation errors in Rabbit RPC services.
 * <p>
 * This exception is a specialized version of {@link RabbitRpcServiceException}, designed to include
 * detailed validation errors when method arguments fail to meet expected constraints.
 * It can encapsulate a {@code bindingResult}, which maps field names to error messages for precise error reporting.
 * </p>
 * <p>
 * Use this exception to propagate validation errors from Rabbit RPC services to clients in a structured manner.
 * </p>
 *
 * @author tex1988
 * @see RabbitRpcServiceException
 * @since 2024-05-12
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class RabbitRpcServiceValidationException extends RabbitRpcServiceException {

    private Map<String, String> bindingResult;

    /**
     * Constructs a new RabbitRpcServiceValidationException with detailed validation errors.
     *
     * @param timestamp     the timestamp when the exception occurred
     * @param serviceName   the name of the service where the exception occurred
     * @param statusCode    the HTTP status code representing the error
     * @param message       a detailed message describing the exception
     * @param bindingResult a map of validation errors where the key is the field name and the value is the error message
     */
    public RabbitRpcServiceValidationException(Long timestamp, String serviceName,
                                               Integer statusCode, String message, Map<String, String> bindingResult) {
        super(timestamp, serviceName, statusCode, message);
        this.bindingResult = bindingResult;
    }

    /**
     * Constructs a new RabbitRpcServiceValidationException without detailed validation errors.
     *
     * @param timestamp   the timestamp when the exception occurred
     * @param serviceName the name of the service where the exception occurred
     * @param statusCode  the HTTP status code representing the error
     * @param message     a detailed message describing the exception
     */
    public RabbitRpcServiceValidationException(Long timestamp, String serviceName, Integer statusCode, String message) {
        super(timestamp, serviceName, statusCode, message);
    }
}
