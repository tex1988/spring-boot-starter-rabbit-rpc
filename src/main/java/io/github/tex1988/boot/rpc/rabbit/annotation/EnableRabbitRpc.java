package io.github.tex1988.boot.rpc.rabbit.annotation;

import io.github.tex1988.boot.rpc.rabbit.autoconfigure.RabbitRpcClientRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables Rabbit RPC for Spring Boot applications.
 *
 * <p>By default, no Rabbit RPC clients or servers are created.
 * To enable server functionality, set {@link #enableServer()} to {@code true}.
 * To enable client functionality, set {@link #enableClient()} to {@code true}.
 * Use {@link #scanBasePackages()} to specify the packages to scan for classes annotated with {@link RabbitRpcInterface}.
 *
 * <p>This annotation allows for additional configurations such as defining serialization patterns,
 * setting concurrency levels for message listener containers, and specifying a custom task executor.
 *
 * @author tex1988
 * @since 2024-04-12
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(RabbitRpcClientRegistrar.class)
public @interface EnableRabbitRpc {

    /**
     * @return {@code true} to enable server functionality, {@code false} otherwise. Default is {@code false}.
     */
    boolean enableServer() default false;

    /**
     * @return {@code true} to enable client functionality, {@code false} otherwise. Default is {@code false}.
     */
    boolean enableClient() default false;

    /**
     * @return the base packages to scan for {@link RabbitRpcInterface} annotated classes.
     * Default is an empty array.
     */
    String[] scanBasePackages() default {};

    /**
     * @return the allowed serialization patterns for the application.
     * Default is an empty array.
     */
    String[] allowedSerializationPatterns() default {};

    /**
     * @return the concurrency configuration for the message listener container, if {@link #enableServer()} is {@code true}.
     * Accepts values such as {@code "5"} or {@code "5-10"} corresponding to `concurrentConsumers` and `maxConcurrentConsumers`.
     * By default, the values defined by `spring-boot-starter-amqp` will be used.
     */
    String concurrency() default "";

    /**
     * @return the bean name of the task executor to use for the message listener container,
     * if {@link #enableServer()} is {@code true}. By default, the executor provided by `spring-boot-starter-amqp` will be used.
     */
    String executor() default "";

    /**
     * @return the bean name of message converter for the message listener container,
     * By default, {@code KryoMessageConverter} will be used.
     */
    String messageConverter() default "";

    /**
     * @return the reply timeout for the RPC client, if {@link #enableClient()} is {@code true}.
     * Default is {@code 5000} milliseconds.
     */
    long replyTimeout() default 5000L;

    /**
     * @return the name of the error handler bean to use.
     * By default, an instance of {@code RabbitRpcErrorHandler} will be used.
     */
    String errorHandler() default "";
}
