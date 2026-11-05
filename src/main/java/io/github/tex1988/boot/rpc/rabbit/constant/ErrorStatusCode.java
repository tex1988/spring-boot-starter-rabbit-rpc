package io.github.tex1988.boot.rpc.rabbit.constant;

import lombok.Getter;

/**
 * Enum representing standard HTTP status codes.
 * <p>
 * This enum provides a set of commonly used HTTP status codes,
 * allowing for consistent representation and usage across the application.
 * Each status code is associated with its corresponding numeric value.
 * </p>
 *
 * <p>Client errors (4xx):</p>
 * <ul>
 *     <li>{@link #BAD_REQUEST} - 400: The server could not understand the request due to invalid syntax.</li>
 *     <li>{@link #UNAUTHORIZED} - 401: The client must authenticate itself to get the requested response.</li>
 *     <li>{@link #PAYMENT_REQUIRED} - 402: Payment is required to access the requested resource.</li>
 *     <li>{@link #FORBIDDEN} - 403: The client does not have access rights to the content.</li>
 *     <li>{@link #NOT_FOUND} - 404: The server cannot find the requested resource.</li>
 *     <li>{@link #METHOD_NOT_ALLOWED} - 405: The requested method is not supported for the target resource.</li>
 *     <li>{@link #NOT_ACCEPTABLE} - 406: No representation matching the client's acceptable criteria is available.</li>
 *     <li>{@link #REQUEST_TIMEOUT} - 408: The server timed out waiting for the request.</li>
 *     <li>{@link #CONFLICT} - 409: The request conflicts with the current state of the resource.</li>
 *     <li>{@link #GONE} - 410: The requested resource is no longer available and will not be available again.</li>
 *     <li>{@link #PRECONDITION_FAILED} - 412: One or more preconditions in the request evaluated to false.</li>
 *     <li>{@link #PAYLOAD_TOO_LARGE} - 413: The request payload is larger than the server is willing to process.</li>
 *     <li>{@link #UNSUPPORTED_MEDIA_TYPE} - 415: The payload format is not supported.</li>
 *     <li>{@link #UNPROCESSABLE_ENTITY} - 422: The request is well-formed but contains semantic errors.</li>
 *     <li>{@link #LOCKED} - 423: The resource being accessed is locked.</li>
 *     <li>{@link #PRECONDITION_REQUIRED} - 428: The server requires the request to be conditional.</li>
 *     <li>{@link #TOO_MANY_REQUESTS} - 429: The client has sent too many requests in a given amount of time.</li>
 * </ul>
 *
 * <p>Server errors (5xx):</p>
 * <ul>
 *     <li>{@link #INTERNAL_SERVER_ERROR} - 500: The server encountered an unexpected condition.</li>
 *     <li>{@link #NOT_IMPLEMENTED} - 501: The server does not support the functionality required to fulfill the request.</li>
 *     <li>{@link #BAD_GATEWAY} - 502: The server received an invalid response from an upstream server.</li>
 *     <li>{@link #SERVICE_UNAVAILABLE} - 503: The server is temporarily unable to handle the request.</li>
 *     <li>{@link #GATEWAY_TIMEOUT} - 504: The server did not receive a timely response from an upstream server.</li>
 *     <li>{@link #INSUFFICIENT_STORAGE} - 507: The server is unable to store the representation needed to complete the request.</li>
 * </ul>
 *
 * @author tex1988
 * @since 2024-04-12
 */
@Getter
public enum ErrorStatusCode {

    /**
     * 400: The server could not understand the request due to invalid syntax.
     */
    BAD_REQUEST(400),

    /**
     * 401: The client must authenticate itself to get the requested response.
     */
    UNAUTHORIZED(401),

    /**
     * 402: Payment is required to access the requested resource.
     */
    PAYMENT_REQUIRED(402),

    /**
     * 403: The client does not have access rights to the content.
     */
    FORBIDDEN(403),

    /**
     * 404: The server cannot find the requested resource.
     */
    NOT_FOUND(404),

    /**
     * 405: The requested method is not supported for the target resource.
     */
    METHOD_NOT_ALLOWED(405),

    /**
     * 406: No representation matching the client's acceptable criteria is available.
     */
    NOT_ACCEPTABLE(406),

    /**
     * 408: The server timed out waiting for the request.
     */
    REQUEST_TIMEOUT(408),

    /**
     * 409: The request conflicts with the current state of the resource.
     */
    CONFLICT(409),

    /**
     * 410: The requested resource is no longer available and will not be available again.
     */
    GONE(410),

    /**
     * 412: One or more preconditions in the request evaluated to false.
     */
    PRECONDITION_FAILED(412),

    /**
     * 413: The request payload is larger than the server is willing to process.
     */
    PAYLOAD_TOO_LARGE(413),

    /**
     * 415: The payload format is not supported.
     */
    UNSUPPORTED_MEDIA_TYPE(415),

    /**
     * 422: The request is well-formed but contains semantic errors.
     */
    UNPROCESSABLE_ENTITY(422),

    /**
     * 423: The resource being accessed is locked.
     */
    LOCKED(423),

    /**
     * 428: The server requires the request to be conditional.
     */
    PRECONDITION_REQUIRED(428),

    /**
     * 429: The client has sent too many requests in a given amount of time.
     */
    TOO_MANY_REQUESTS(429),

    /**
     * 500: The server encountered an unexpected condition.
     */
    INTERNAL_SERVER_ERROR(500),

    /**
     * 501: The server does not support the functionality required to fulfill the request.
     */
    NOT_IMPLEMENTED(501),

    /**
     * 502: The server received an invalid response from an upstream server.
     */
    BAD_GATEWAY(502),

    /**
     * 503: The server is temporarily unable to handle the request.
     */
    SERVICE_UNAVAILABLE(503),

    /**
     * 504: The server did not receive a timely response from an upstream server.
     */
    GATEWAY_TIMEOUT(504),

    /**
     * 507: The server is unable to store the representation needed to complete the request.
     */
    INSUFFICIENT_STORAGE(507);

    /**
     * The numeric value of the HTTP status code.
     */
    private final int code;

    /**
     * Constructs a {@code StatusCode} with the specified numeric value.
     *
     * @param code the numeric value of the HTTP status code
     */
    ErrorStatusCode(int code) {
        this.code = code;
    }
}
