package com.github.tex1988.boot.rpc.rabbit.constant;

import lombok.Getter;

/**
 * Enum representing standard HTTP status codes.
 * <p>
 * This enum provides a set of commonly used HTTP status codes,
 * allowing for consistent representation and usage across the application.
 * Each status code is associated with its corresponding numeric value.
 * </p>
 *
 * <ul>
 *     <li>{@link #BAD_REQUEST} - 400: The server could not understand the request due to invalid syntax.</li>
 *     <li>{@link #UNAUTHORIZED} - 401: The client must authenticate itself to get the requested response.</li>
 *     <li>{@link #FORBIDDEN} - 403: The client does not have access rights to the content.</li>
 *     <li>{@link #NOT_FOUND} - 404: The server cannot find the requested resource.</li>
 *     <li>{@link #INTERNAL_SERVER_ERROR} - 500: The server encountered an unexpected condition.</li>
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
     * 403: The client does not have access rights to the content.
     */
    FORBIDDEN(403),

    /**
     * 404: The server cannot find the requested resource.
     */
    NOT_FOUND(404),

    /**
     * 500: The server encountered an unexpected condition.
     */
    INTERNAL_SERVER_ERROR(500);

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
