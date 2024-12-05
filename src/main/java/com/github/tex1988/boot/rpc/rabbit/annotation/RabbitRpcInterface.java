package com.github.tex1988.boot.rpc.rabbit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an interface as a Rabbit RPC contract.
 * <p>
 * Interfaces annotated with this annotation define the structure and behavior of RPC services
 * using RabbitMQ as the transport layer. The annotation allows configuration of key RabbitMQ
 * parts such as the service name, exchange, queue, and routing key.
 * </p>
 *
 * <p>All properties support SpEL (Spring Expression Language) expressions,
 * enabling dynamic configuration.</p>
 *
 * @author tex1988
 * @since 2024-04-12
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RabbitRpcInterface {

    /**
     * Specifies the name of the exchange to which messages will be sent.
     * <p>
     * The exchange is responsible for routing messages to the appropriate queue(s).
     * </p>
     *
     * @return the exchange name
     */
    String exchange();

    /**
     * Specifies the name of the queue that will receive messages.
     * <p>
     * This queue is bound to the specified exchange. A single queue can be shared across
     * multiple RPC interfaces if needed.
     * </p>
     *
     * @return the queue name
     */
    String queue();

    /**
     * Specifies the routing key used for message routing.
     * <p>
     * Routing keys are used to determine how messages are routed from the exchange to the queue(s).
     * </p>
     *
     * @return the routing key
     */
    String routing();
}
