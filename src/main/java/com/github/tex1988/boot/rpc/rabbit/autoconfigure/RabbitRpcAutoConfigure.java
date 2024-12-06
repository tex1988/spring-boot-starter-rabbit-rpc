package com.github.tex1988.boot.rpc.rabbit.autoconfigure;

import com.github.tex1988.boot.rpc.rabbit.annotation.EnableRabbitRpc;
import com.github.tex1988.boot.rpc.rabbit.annotation.RabbitRpcInterface;
import com.github.tex1988.boot.rpc.rabbit.annotation.RabbitRpcService;
import com.github.tex1988.boot.rpc.rabbit.model.RabbitRpcErrorMapping;
import com.github.tex1988.boot.rpc.rabbit.rabbit.RabbitRpcBeanExpressionResolver;
import com.github.tex1988.boot.rpc.rabbit.rabbit.RabbitRpcErrorHandler;
import com.github.tex1988.boot.rpc.rabbit.rabbit.RabbitRpcMessageHandler;
import com.github.tex1988.boot.rpc.rabbit.validator.RabbitRpcValidator;
import com.rabbitmq.client.Channel;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.MethodRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.amqp.RabbitRetryTemplateCustomizer;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.retry.support.RetryTemplate;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.tex1988.boot.rpc.rabbit.constant.Constants.DEFAULT_ALLOWED_SERIALIZATION_PATTERNS;
import static com.github.tex1988.boot.rpc.rabbit.constant.Constants.HANDLER_METHOD_NAME;
import static com.github.tex1988.boot.rpc.rabbit.constant.Constants.RPC_RABBIT_TEMPLATE_BEAN_NAME;

@Configuration
@RequiredArgsConstructor
@ConditionalOnClass(EnableRabbitRpc.class)
class RabbitRpcAutoConfigure {

    private final ApplicationContext applicationContext;
    private final ConnectionFactory connectionFactory;
    private final SimpleRabbitListenerContainerFactoryConfigurer configurer;
    private final DefaultMessageHandlerMethodFactory messageHandlerMethodFactory = new DefaultMessageHandlerMethodFactory();
    private final RabbitRetryTemplateCustomizer customizeRetryPolicy;
    private final AmqpAdmin amqpAdmin;
    private final Validator validator;
    private final Map<Class<?>, Map<Method, MethodHandle>> methodHandles = new ConcurrentHashMap<>();
    private final RabbitRpcBeanExpressionResolver expressionResolver;

    private SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory;
    private SimpleMessageConverter messageConverter;
    private RabbitListenerErrorHandler errorHandler;

    @Bean
    public RabbitRpcBeanExpressionResolver rabbitRpcBeanExpressionResolver() {
        return expressionResolver;
    }

    @PostConstruct
    public void init() {
        EnableRabbitRpc annotation = getEnableRabbitRpc();
        if (annotation != null && (annotation.enableClient() || annotation.enableServer())) {
            initRabbitTemplate(annotation);
        }
        if (annotation != null && annotation.enableServer()) {

            List<Object> beanList = applicationContext
                    .getBeansWithAnnotation(RabbitRpcService.class).values().stream().toList();
            if (!beanList.isEmpty()) {
                initRabbitListenerContainerFactory(annotation);
                errorHandler = getErrorHandler(annotation);
                createMethodHandles(beanList);
                initServers(beanList);
            }
        }
    }

    private void initRabbitTemplate(EnableRabbitRpc annotation) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(getRpcMessageConverter(annotation));
        rabbitTemplate.setReplyTimeout(annotation.replyTimeout());
        rabbitTemplate.setRetryTemplate(getSenderRetryTemplate());
        ((ConfigurableApplicationContext) applicationContext).getBeanFactory()
                .registerSingleton(RPC_RABBIT_TEMPLATE_BEAN_NAME, rabbitTemplate);
    }

    private void initRabbitListenerContainerFactory(EnableRabbitRpc annotation) {
        rabbitListenerContainerFactory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(rabbitListenerContainerFactory, connectionFactory);
        rabbitListenerContainerFactory.setMessageConverter(messageConverter);
        rabbitListenerContainerFactory.setRetryTemplate(getReceiverRetryTemplate());
        List<Integer> concurrency = getConcurrency(annotation);
        if (!concurrency.isEmpty()) {
            rabbitListenerContainerFactory.setConcurrentConsumers(concurrency.get(0));
            if (concurrency.size() > 1) {
                rabbitListenerContainerFactory.setMaxConcurrentConsumers(concurrency.get(1));
            }
        }
        if (annotation.executor() != null) {
            rabbitListenerContainerFactory.setTaskExecutor(getTaskExecutor(annotation));
        }
    }

    private void createMethodHandles(List<Object> beanList) {
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        Map<Class<?>, List<Object>> beanMap = beanList.stream()
                .collect(Collectors.groupingBy(bean -> getRabbitRpcInterface(bean.getClass()), Collectors.toList()));
        beanMap.forEach((iClass, beans) -> beans.forEach(bean ->
                createMethodHandles(bean, iClass, lookup)));
    }

    @SneakyThrows
    private void createMethodHandles(Object bean, Class<?> iClazz, MethodHandles.Lookup lookup) {
        Map<Method, MethodHandle> beanMethodHandles = new ConcurrentHashMap<>();
        Arrays.stream(iClazz.getMethods()).forEach(m -> {
            Class<?>[] argTypes = m.getParameterTypes();
            MethodType mt = MethodType.methodType(m.getReturnType(), argTypes);
            try {
                MethodHandle mh = lookup.findVirtual(bean.getClass(), m.getName(), mt).bindTo(bean);
                beanMethodHandles.put(m, mh);
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
        methodHandles.put(iClazz, beanMethodHandles);
    }

    private void initServers(List<Object> beanList) {
        Map<String, Map<String, List<Object>>> beanMap = getBeanMap(beanList);
        beanMap.forEach(this::processExchange);
    }

    private Map<String, Map<String, List<Object>>> getBeanMap(List<Object> beanList) {
        return beanList.stream()
                .collect(Collectors.groupingBy(bean -> Objects.requireNonNull(
                                expressionResolver.resolveValue(getRabbitRpcInterface(bean.getClass()).getAnnotation(RabbitRpcInterface.class).exchange())),
                        Collectors.groupingBy(bean -> Objects.requireNonNull(
                                        expressionResolver.resolveValue(getRabbitRpcInterface(bean.getClass()).getAnnotation(RabbitRpcInterface.class).queue())),
                                Collectors.toList())));
    }

    private void processExchange(String exchange, Map<String, List<Object>> queues) {
        createOrConnectExchange(exchange, amqpAdmin);
        queues.forEach((queueName, beans) -> {
            RabbitRpcInterface annotation = getRabbitRpcInterface(beans.getFirst().getClass()).getAnnotation(RabbitRpcInterface.class);
            String routing = expressionResolver.resolveValue(annotation.routing());
            Queue queue = createQueue(queueName, exchange, routing, amqpAdmin);
            MessageListenerContainer container = getMessageListenerContainer(queue);
            container.start();
        });
    }

    private void createOrConnectExchange(String exchangeName, AmqpAdmin amqpAdmin) {
        try (Connection connection = connectionFactory.createConnection(); Channel channel = connection.createChannel(true)) {
            channel.exchangeDeclarePassive(exchangeName);
        } catch (Exception e) {
            if (e.getCause() != null && e.getCause().getMessage().contains("404")) {
                DirectExchange exchange = new DirectExchange(exchangeName, false, true);
                amqpAdmin.declareExchange(exchange);
            } else {
                throw new RuntimeException("Failed to check exchange existence: " + e.getMessage(), e);
            }
        }
    }

    private Queue createQueue(String queueName, String exchangeName, String routing, AmqpAdmin amqpAdmin) {
        QueueInformation queueInfo = amqpAdmin.getQueueInfo(queueName);
        if (queueInfo == null || queueInfo.getName() == null) {
            Queue queue = new Queue(queueName, false);
            amqpAdmin.declareQueue(queue);
            Binding binding = BindingBuilder.bind(queue)
                    .to(new DirectExchange(exchangeName, false, true))
                    .with(routing);
            amqpAdmin.declareBinding(binding);
            return queue;
        } else {
            return new Queue(queueName, false);
        }
    }

    private Class<?> getRabbitRpcInterface(Class<?> clazz) {
        List<Class<?>> interfaces = Arrays.stream(clazz.getInterfaces())
                .filter(i -> i.isAnnotationPresent(RabbitRpcInterface.class))
                .toList();
        if (interfaces.isEmpty()) {
            throw new IllegalStateException("No implementations of RabbitRpcInterface found on class " + clazz.getName());
        }
        if (interfaces.size() > 1) {
            throw new IllegalStateException("Multiple implementations of RabbitRpcInterface found on class " + clazz.getName());
        }
        return interfaces.getFirst();
    }

    @SneakyThrows
    private SimpleMessageListenerContainer getMessageListenerContainer(Queue queue) {
        MethodRabbitListenerEndpoint endpoint = new MethodRabbitListenerEndpoint();
        RabbitRpcValidator rpcValidator = new RabbitRpcValidator(validator, getServiceName());
        RabbitRpcMessageHandler handler = new RabbitRpcMessageHandler(rpcValidator, messageConverter, methodHandles);
        Method handleMethod = handler.getClass().getMethod(HANDLER_METHOD_NAME, Message.class, Channel.class, MessageProperties.class);
        endpoint.setQueues(queue);
        endpoint.setBean(handler);
        endpoint.setMethod(handleMethod);
        endpoint.setErrorHandler(errorHandler);
        endpoint.setMessageHandlerMethodFactory(messageHandlerMethodFactory);
        return rabbitListenerContainerFactory.createListenerContainer(endpoint);
    }

    private RabbitListenerErrorHandler getErrorHandler(EnableRabbitRpc annotation) {
        String errorHandlerBeanName = expressionResolver.resolveValue(annotation.errorHandler());
        if (errorHandlerBeanName != null && !errorHandlerBeanName.isBlank()) {
            return applicationContext.getBean(errorHandlerBeanName, RabbitListenerErrorHandler.class);
        } else {
            RabbitRpcErrorMapping errorMapping = getErrorMapping();
            return new RabbitRpcErrorHandler(getServiceName(), errorMapping);
        }
    }

    private SimpleMessageConverter getRpcMessageConverter(EnableRabbitRpc annotation) {
        if (messageConverter == null) {
            String[] allowedSerializationPatterns;
            if (annotation.allowedSerializationPatterns() != null) {
                allowedSerializationPatterns = annotation.allowedSerializationPatterns();
            } else {
                allowedSerializationPatterns = new String[0];
            }
            SimpleMessageConverter simpleMessageConverter = new SimpleMessageConverter();
            simpleMessageConverter.setAllowedListPatterns(Stream.concat(Arrays.stream(allowedSerializationPatterns),
                    DEFAULT_ALLOWED_SERIALIZATION_PATTERNS.stream()).toList());
            messageConverter = simpleMessageConverter;
            return simpleMessageConverter;
        } else {
            return messageConverter;
        }
    }

    private Executor getTaskExecutor(EnableRabbitRpc annotation) {
        String executorBeanName = expressionResolver.resolveValue(annotation.executor());
        if (executorBeanName != null && !executorBeanName.isBlank()) {
            return applicationContext.getBean(executorBeanName, Executor.class);
        } else {
            return null;
        }
    }

    private RetryTemplate getSenderRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        customizeRetryPolicy.customize(RabbitRetryTemplateCustomizer.Target.SENDER, retryTemplate);
        return retryTemplate;

    }

    private RetryTemplate getReceiverRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        customizeRetryPolicy.customize(RabbitRetryTemplateCustomizer.Target.LISTENER, retryTemplate);
        return retryTemplate;
    }

    private EnableRabbitRpc getEnableRabbitRpc() {
        String beanName = Arrays.stream(applicationContext
                .getBeanNamesForAnnotation(EnableRabbitRpc.class)).findFirst().orElse(null);
        if (beanName != null) {
            return applicationContext.findAnnotationOnBean(beanName, EnableRabbitRpc.class);
        } else {
            return null;
        }
    }

    private List<Integer> getConcurrency(EnableRabbitRpc annotation) {
        String concurrency = expressionResolver.resolveValue(annotation.concurrency());
        if (concurrency != null && !concurrency.isBlank()) {
            return Arrays.stream(concurrency.split("-")).map(Integer::parseInt).toList();
        } else {
            return Collections.emptyList();
        }
    }

    @SneakyThrows
    private String getServiceName() {
        String hostName = InetAddress.getLocalHost().getHostName();
        if (hostName != null) {
            return hostName;
        } else {
            return InetAddress.getLocalHost().getHostAddress();
        }
    }

    private RabbitRpcErrorMapping getErrorMapping() {
        RabbitRpcErrorMapping errorMapping;
        try {
            errorMapping = applicationContext.getBean(RabbitRpcErrorMapping.class);
        } catch (NoSuchBeanDefinitionException e) {
            errorMapping = null;
        }
        return errorMapping;
    }
}
