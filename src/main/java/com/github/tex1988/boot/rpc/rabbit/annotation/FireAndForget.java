package com.github.tex1988.boot.rpc.rabbit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method in an interface annotated with {@link RabbitRpcInterface}
 * to be invoked as a fire-and-forget method.
 * <p>
 * A fire-and-forget method does not send the result of the invocation back to the client.
 * Methods annotated with this annotation must have a {@code void} return type,
 * as no response is expected.
 * </p>
 *
 * <p>Use this annotation to define methods where acknowledgment or response
 * from the server is unnecessary, focusing solely on the execution of the method.
 * </p>
 *
 * @author tex1988
 * @since 2024-04-12
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FireAndForget {
}
