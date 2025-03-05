package io.github.tex1988.boot.rpc.rabbit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a RabbitMQ RPC service.
 * <p>
 * Classes annotated with this annotation are intended to handle
 * Remote Procedure Call (RPC) requests over RabbitMQ.
 * This annotation serves as a marker and does not automatically
 * register the class as a Spring bean.
 * </p>
 *
 * <p>
 * To enable functionality, ensure that the class is manually
 * registered as a Spring bean, either through component scanning
 * (e.g., {@code @Component}) or explicit bean registration
 * in a configuration class.
 * </p>
 *
 * <p>
 * Use this annotation to organize and document service classes
 * that process RPC requests and respond to clients using RabbitMQ
 * as the transport layer.
 * </p>
 *
 * @author tex1988
 * @since 2024-04-12
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RabbitRpc {
}
