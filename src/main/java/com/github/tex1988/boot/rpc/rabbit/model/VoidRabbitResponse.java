package com.github.tex1988.boot.rpc.rabbit.model;

import java.io.Serializable;

/**
 * A mock response class used for methods that have a {@code void} return type
 * in Rabbit RPC, where the method is not annotated with {@link FireAndForget}.
 * <p>
 * This class is used as a placeholder response for void methods, where no actual response
 * is expected to be returned, but a valid message structure is required for the Rabbit RPC
 * communication.
 * </p>
 *
 * @author tex1988
 * @since 2024-04-12
 */
public class VoidRabbitResponse implements Serializable {
    // No fields or methods are required as this class serves as a marker for void responses.
}
