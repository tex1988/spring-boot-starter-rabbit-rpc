package com.github.tex1988.boot.rpc.rabbit.rabbit;

import com.github.tex1988.boot.rpc.rabbit.annotation.FireAndForget;
import com.github.tex1988.boot.rpc.rabbit.annotation.RabbitRpcInterface;
import com.github.tex1988.boot.rpc.rabbit.exception.RabbitRpcServiceException;
import com.github.tex1988.boot.rpc.rabbit.model.ErrorRabbitResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static com.github.tex1988.boot.rpc.rabbit.constant.Constants.METHOD_HEADER;
import static com.github.tex1988.boot.rpc.rabbit.constant.Constants.RPC_RABBIT_TEMPLATE_BEAN_NAME;
import static com.github.tex1988.boot.rpc.rabbit.constant.Constants.SERVICE_HEADER;

public class RabbitRpcClientProxyFactory<T> implements FactoryBean<T> {

    private static final Method HASH_CODE;
    private static final Method EQUALS;
    private static final Method TO_STRING;

    private final Class<T> interfaceType;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitRpcBeanExpressionResolver expressionResolver;
    private final RabbitRpcInterface annotation;

    private  String serviceName;
    private  String exchange;
    private  String routing;

    static {
        Class<Object> objClass = Object.class;
        try {
            HASH_CODE = objClass.getDeclaredMethod("hashCode");
            EQUALS = objClass.getDeclaredMethod("equals", objClass);
            TO_STRING = objClass.getDeclaredMethod("toString");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Autowired
    public RabbitRpcClientProxyFactory(Class<T> interfaceType,
                                       @Lazy @Qualifier(RPC_RABBIT_TEMPLATE_BEAN_NAME) RabbitTemplate rabbitTemplate,
                                       @Lazy RabbitRpcBeanExpressionResolver expressionResolver) {
        this.interfaceType = interfaceType;
        this.rabbitTemplate = rabbitTemplate;
        this.annotation = interfaceType.getAnnotation(RabbitRpcInterface.class);
        this.expressionResolver = expressionResolver;
    }

    @PostConstruct
    public void init() {
        this.serviceName = expressionResolver.resolveValue(annotation.service());
        this.exchange = expressionResolver.resolveValue(annotation.exchange());
        this.routing = expressionResolver.resolveValue(annotation.routing());
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getObject() {
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.equals(HASH_CODE)) {
                return objectHashCode(proxy);
            }
            if (method.equals(EQUALS)) {
                return objectEquals(proxy, args[0]);
            }
            if (method.equals(TO_STRING)) {
                return objectToString(proxy);
            }

            MessagePostProcessor postProcessor = message -> {
                MessageProperties properties = message.getMessageProperties();
                properties.setHeader(SERVICE_HEADER, interfaceType.getCanonicalName());
                properties.setHeader(METHOD_HEADER, method.getName());
                return message;
            };

            if (method.isAnnotationPresent(FireAndForget.class)) {
                rabbitTemplate.convertAndSend(exchange, routing, args, postProcessor);
                return null;
            } else {
                return unwrapResponse(rabbitTemplate.convertSendAndReceive(exchange, routing, args, postProcessor));
            }
        };

        return (T) Proxy.newProxyInstance(
                interfaceType.getClassLoader(),
                new Class[]{interfaceType},
                handler
        );
    }

    @Override
    public Class<?> getObjectType() {
        return interfaceType;
    }

    @SuppressWarnings("unchecked")
    private <R> R unwrapResponse(Object response) {
        if (response == null) {
            throw new IllegalStateException("No response from " + serviceName);
        }
        if (response instanceof ErrorRabbitResponse errorResponse) {
            throw new RabbitRpcServiceException(errorResponse.getTimestamp(), errorResponse.getServiceName(),
                    errorResponse.getStatusCode(), errorResponse.getMessage());
        }
        return (R) response;
    }

    public String objectClassName(Object obj) {
        return obj.getClass().getName();
    }

    public int objectHashCode(Object obj) {
        return System.identityHashCode(obj);
    }

    public boolean objectEquals(Object obj, Object other) {
        return obj == other;
    }

    public String objectToString(Object obj) {
        return objectClassName(obj) + '@' + Integer.toHexString(objectHashCode(obj));
    }
}
