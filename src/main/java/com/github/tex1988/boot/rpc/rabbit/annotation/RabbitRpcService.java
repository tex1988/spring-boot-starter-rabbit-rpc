package com.github.tex1988.boot.rpc.rabbit.annotation;

import org.springframework.stereotype.Service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a Rabbit RPC service.
 * <p>
 * This annotation is used in combination with Spring's {@link Service} to enable
 * Remote Procedure Call (RPC) functionality over RabbitMQ.
 * Classes annotated with this annotation will be registered as beans
 * and configured to handle incoming RPC calls.
 * </p>
 *
 * <p>Use this annotation to define service classes that process RPC requests
 * and respond to clients using RabbitMQ as the transport layer.</p>
 *
 * @author tex1988
 * @since 2024-04-12
 */
@Service
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RabbitRpcService {
}
