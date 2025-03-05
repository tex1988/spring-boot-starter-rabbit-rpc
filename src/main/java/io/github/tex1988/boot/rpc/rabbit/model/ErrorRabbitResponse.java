package io.github.tex1988.boot.rpc.rabbit.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * Represents an error response sent from the Rabbit RPC service when an exception occurs.
 * <p>
 * This class encapsulates the details of the error, including:
 * <ul>
 *     <li>Timestamp of the error occurrence</li>
 *     <li>Status code representing the error</li>
 *     <li>The service name where the error originated</li>
 *     <li>The error message with details about the failure</li>
 * </ul>
 * </p>
 *
 * @author tex1988
 * @since 2024-04-12
 */
@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class ErrorRabbitResponse implements Serializable {

    /**
     * The timestamp when the error occurred, in milliseconds since the epoch.
     */
    private Long timestamp;

    /**
     * The error status code associated with the error.
     * This could represent various types of errors, such as 400 for validation errors,
     * 500 for internal server errors, etc.
     */
    private Integer statusCode;

    /**
     * The name of the Rabbit RPC service that encountered the error.
     */
    private String serviceName;

    /**
     * A detailed message describing the error.
     * This message will provide additional context for the failure, such as validation errors or exception details.
     */
    private String message;

    /**
     * An optional binding result containing the validation errors.
     * This is only non-null when the error is a result of a validation error.
     */
    private Map<String, String> bindingResult;
}
