package io.github.tex1988.boot.rpc.rabbit.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents an exception specific to Rabbit RPC services.
 * <p>
 * This exception is used to capture and convey information about errors
 * that occur during RabbitMQ RPC service operations, including the timestamp
 * of the error, the name of the service, and an associated status code.
 * </p>
 *
 * <p>The exception extends {@link RuntimeException}, making it an unchecked exception.</p>
 *
 * <p>Use this exception to handle and propagate RPC-related errors in a structured manner.</p>
 *
 * @author tex1988
 * @since 2024-04-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RabbitRpcServiceException extends RuntimeException {

    /**
     * The timestamp when the exception occurred.
     */
    private Long timestamp;

    /**
     * The name of the Rabbit RPC service where the exception occurred.
     */
    private String serviceName;

    /**
     * The status code associated with the exception, providing additional context.
     */
    private Integer statusCode;

    /**
     * Constructs a new {@code RabbitRpcServiceException} with the specified details.
     *
     * @param timestamp   the timestamp when the exception occurred
     * @param serviceName the name of the Rabbit RPC service
     * @param statusCode  the status code associated with the exception
     * @param message     the detailed message explaining the reason for the exception
     */
    public RabbitRpcServiceException(Long timestamp, String serviceName, Integer statusCode, String message) {
        super(message);
        this.timestamp = timestamp;
        this.serviceName = serviceName;
        this.statusCode = statusCode;
    }
}
