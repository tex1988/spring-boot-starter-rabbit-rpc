package com.github.tex1988.boot.rpc.rabbit.rabbit;

import com.github.tex1988.boot.rpc.rabbit.annotation.FireAndForget;
import com.github.tex1988.boot.rpc.rabbit.model.VoidRabbitResponse;
import com.github.tex1988.boot.rpc.rabbit.validator.RabbitRpcValidator;
import com.rabbitmq.client.Channel;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.messaging.support.MessageBuilder;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.Map;

import static com.github.tex1988.boot.rpc.rabbit.constant.Constants.METHOD_HEADER;
import static com.github.tex1988.boot.rpc.rabbit.constant.Constants.SERVICE_HEADER;
import static com.github.tex1988.boot.rpc.rabbit.constant.Constants.TYPE_ID_HEADER;

/**
 * Handles incoming Rabbit RPC messages and invokes the corresponding service method.
 * <p>
 * This class is responsible for:
 * <ul>
 *     <li>Converting RabbitMQ messages into method arguments.</li>
 *     <li>Validating method arguments against defined constraints.</li>
 *     <li>Invoking the appropriate service method using {@link MethodHandle}.</li>
 *     <li>Returning the result or handling fire-and-forget messages.</li>
 * </ul>
 *
 * <p>Messages must include headers specifying the method and service name.</p>
 *
 * @author tex1988
 * @since 2024-04-12
 */
@Slf4j
@AllArgsConstructor
public class RabbitRpcMessageHandler {

    /**
     * Validator for validating method arguments and constraints.
     */
    private final RabbitRpcValidator validator;

    /**
     * Converter for transforming RabbitMQ messages to method arguments.
     */
    private final MessageConverter converter;

    /**
     * A mapping between service interfaces and their methods, associated with {@link MethodHandle}s for invocation.
     */
    private final Map<Class<?>, Map<Method, MethodHandle>> methodHandles;

    /**
     * Processes a RabbitMQ message and invokes the appropriate service method.
     *
     * @param message           the incoming RabbitMQ message
     * @param channel           the RabbitMQ channel (unused in this method)
     * @param messageProperties the properties of the RabbitMQ message
     * @return a {@link Message} object containing the method's return value
     * or {@code null}if the method is annotated with {@link FireAndForget}
     * @throws IllegalStateException if the method specified in the headers is not found
     */
    @SneakyThrows
    public Object handleMessage(Message message, Channel channel, MessageProperties messageProperties) {
        log.debug("Received Rabbit RPC message {}", message);

        // Extract method and service names from message headers
        String methodName = messageProperties.getHeaders().get(METHOD_HEADER).toString();
        String serviceName = messageProperties.getHeaders().get(SERVICE_HEADER).toString();

        // Convert message payload to method arguments
        Object[] args = (Object[]) converter.fromMessage(message);

        // Load the service class and retrieve its method handles
        Class<?> iClazz = this.getClass().getClassLoader().loadClass(serviceName);
        Map<Method, MethodHandle> iMethodHandles = methodHandles.get(iClazz);

        // Find the target method by name
        Map.Entry<Method, MethodHandle> methodEntry = iMethodHandles.entrySet().stream()
                .filter(e -> e.getKey().getName().equals(methodName))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Method: " + methodName + " not found"));

        Method method = methodEntry.getKey();
        MethodHandle methodHandle = methodEntry.getValue();
        Class<?> returnType = method.getReturnType();

        // Validate method arguments
        validator.validate(args, method, iClazz);

        // Invoke the target method
        Object result = methodHandle.invokeWithArguments(args);

        // Handle fire-and-forget methods
        if (method.isAnnotationPresent(FireAndForget.class)) {
            return null;
        } else {
            // Build and return a response message
            return MessageBuilder.withPayload(returnType.equals(Void.TYPE) ? new VoidRabbitResponse() : result)
                    .setHeader(TYPE_ID_HEADER, returnType.getCanonicalName())
                    .build();
        }
    }
}
