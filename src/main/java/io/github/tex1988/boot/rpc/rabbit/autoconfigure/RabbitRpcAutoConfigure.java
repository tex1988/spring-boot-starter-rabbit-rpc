package io.github.tex1988.boot.rpc.rabbit.autoconfigure;

import com.rabbitmq.client.Channel;
import io.github.tex1988.boot.rpc.rabbit.annotation.EnableRabbitRpc;
import io.github.tex1988.boot.rpc.rabbit.annotation.RabbitRpc;
import io.github.tex1988.boot.rpc.rabbit.annotation.RabbitRpcInterface;
import io.github.tex1988.boot.rpc.rabbit.converter.ForyMessageConverter;
import io.github.tex1988.boot.rpc.rabbit.model.RabbitRpcErrorMapping;
import io.github.tex1988.boot.rpc.rabbit.rabbit.RabbitRpcBeanExpressionResolver;
import io.github.tex1988.boot.rpc.rabbit.rabbit.RabbitRpcClientProxyFactory;
import io.github.tex1988.boot.rpc.rabbit.rabbit.RabbitRpcErrorHandler;
import io.github.tex1988.boot.rpc.rabbit.rabbit.RabbitRpcMessageHandler;
import io.github.tex1988.boot.rpc.rabbit.validator.RabbitRpcValidator;
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
import org.springframework.amqp.rabbit.config.RabbitListenerConfigUtils;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.MethodRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.util.ClassUtils;

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

import static io.github.tex1988.boot.rpc.rabbit.constant.Constants.HANDLER_METHOD_NAME;
import static io.github.tex1988.boot.rpc.rabbit.constant.Constants.RPC_RABBIT_TEMPLATE_BEAN_NAME;

@Configuration
@RequiredArgsConstructor
@ConditionalOnClass(EnableRabbitRpc.class)
class RabbitRpcAutoConfigure {

    private final ApplicationContext applicationContext;
    private final ConnectionFactory connectionFactory;
    private final SimpleRabbitListenerContainerFactoryConfigurer configurer;
    private final DefaultMessageHandlerMethodFactory messageHandlerMethodFactory = new DefaultMessageHandlerMethodFactory();
    private final AmqpAdmin amqpAdmin;
    private final Validator validator;
    private final Map<Class<?>, Map<Method, MethodHandle>> methodHandles = new ConcurrentHashMap<>();
    private final RabbitRpcBeanExpressionResolver expressionResolver;

    private MessageConverter messageConverter;
    private SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory;
    private RabbitListenerErrorHandler errorHandler;

    @PostConstruct
    public void init() {
        EnableRabbitRpc annotation = getEnableRabbitRpc();
        if (annotation != null && (annotation.enableClient() || annotation.enableServer())) {
            messageConverter = getRpcMessageConverter(annotation);
            initRabbitTemplate(annotation);
        }
        if (annotation != null && annotation.enableServer()) {

            List<Object> beanList = applicationContext
                    .getBeansWithAnnotation(RabbitRpc.class).values().stream().toList();
            if (!beanList.isEmpty()) {
                ((RabbitAdmin) amqpAdmin).setRedeclareManualDeclarations(true);
                initRabbitListenerContainerFactory(annotation);
                createMethodHandles(beanList);
                errorHandler = getErrorHandler(annotation, methodHandles);
                initServers(beanList);
            }
        }
    }

    private void initRabbitTemplate(EnableRabbitRpc annotation) {
        ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext) applicationContext).getBeanFactory();
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        rabbitTemplate.setReplyTimeout(annotation.replyTimeout());
        beanFactory.registerSingleton(RPC_RABBIT_TEMPLATE_BEAN_NAME, rabbitTemplate);
        beanFactory.getBeansOfType(RabbitRpcClientProxyFactory.class).forEach((name, factory) ->
                factory.setMessageTtl(String.valueOf(annotation.replyTimeout())));
    }

    private void initRabbitListenerContainerFactory(EnableRabbitRpc annotation) {
        rabbitListenerContainerFactory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(rabbitListenerContainerFactory, connectionFactory);
        rabbitListenerContainerFactory.setMessageConverter(messageConverter);
        rabbitListenerContainerFactory.setFailedDeclarationRetryInterval(10000L);
        rabbitListenerContainerFactory.setMissingQueuesFatal(false);
        rabbitListenerContainerFactory.setDefaultRequeueRejected(true);
        rabbitListenerContainerFactory.setApplicationEventPublisher(applicationContext);
        rabbitListenerContainerFactory.setApplicationContext(applicationContext);
        List<Integer> concurrency = getConcurrency(annotation);
        if (!concurrency.isEmpty()) {
            rabbitListenerContainerFactory.setConcurrentConsumers(concurrency.get(0));
            if (concurrency.size() > 1) {
                rabbitListenerContainerFactory.setMaxConcurrentConsumers(concurrency.get(1));
            }
        }
        if (annotation.executor() != null && !annotation.executor().isEmpty()) {
            rabbitListenerContainerFactory.setTaskExecutor(getTaskExecutor(annotation));
        }
    }

    private void createMethodHandles(List<Object> beanList) {
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        Map<Class<?>, List<Object>> beanMap = beanList.stream()
                .collect(Collectors.groupingBy(this::getRabbitRpcInterface, Collectors.toList()));
        beanMap.forEach((iClass, beans) -> beans.forEach(bean ->
                createMethodHandles(bean, iClass, lookup)));
    }

    @SneakyThrows
    private void createMethodHandles(Object bean, Class<?> iClazz, MethodHandles.Lookup lookup) {
        Map<Method, MethodHandle> beanMethodHandles = new ConcurrentHashMap<>();
        for (Method method : iClazz.getMethods()) {
            Class<?>[] argTypes = method.getParameterTypes();
            MethodType mt = MethodType.methodType(method.getReturnType(), argTypes);
            MethodHandle mh = lookup.findVirtual(bean.getClass(), method.getName(), mt).bindTo(bean);
            beanMethodHandles.put(method, mh);
        }
        methodHandles.put(iClazz, beanMethodHandles);
    }

    private void initServers(List<Object> beanList) {
        Map<String, Map<String, List<Object>>> beanMap = getBeanMap(beanList);
        beanMap.forEach(this::processExchange);
    }

    private Map<String, Map<String, List<Object>>> getBeanMap(List<Object> beanList) {
        return beanList.stream()
                .collect(Collectors.groupingBy(this::resolveExchangeName,
                        Collectors.groupingBy(this::resolveQueueName, Collectors.toList())));
    }

    private void processExchange(String exchange, Map<String, List<Object>> queues) {
        createOrConnectExchange(exchange, amqpAdmin);
        queues.forEach((queueName, beans) -> {
            String routing = resolveRouting(beans.get(0));
            Queue queue = createQueue(queueName, exchange, routing, amqpAdmin);
            createMessageListenerContainer(queue);
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
                throw new IllegalStateException("Failed to check exchange existence: " + e.getMessage(), e);
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

    private Class<?> getRabbitRpcInterface(Object candidate) {
        Class<?> clazz = ClassUtils.getUserClass(candidate);
        List<Class<?>> interfaces = Arrays.stream(clazz.getInterfaces())
                .filter(i -> i.isAnnotationPresent(RabbitRpcInterface.class))
                .toList();
        if (interfaces.isEmpty()) {
            throw new IllegalStateException("No implementations of RabbitRpcInterface found for class " + clazz.getName());
        }
        if (interfaces.size() > 1) {
            throw new IllegalStateException("Multiple implementations of RabbitRpcInterface found for class " + clazz.getName());
        }
        return interfaces.get(0);
    }

    private String resolveExchangeName(Object bean) {
        Class<?> iClass = getRabbitRpcInterface(bean);
        RabbitRpcInterface annotation = Objects.requireNonNull(iClass.getAnnotation(RabbitRpcInterface.class));
        return Objects.requireNonNull(expressionResolver.resolveValue(annotation.exchange()),
                "Exchange name cannot be null. Define exchange property of @RabbitRpcInterface annotation for " + iClass.getName());
    }

    private String resolveQueueName(Object bean) {
        Class<?> iClass = getRabbitRpcInterface(bean);
        RabbitRpcInterface annotation = Objects.requireNonNull(iClass.getAnnotation(RabbitRpcInterface.class));
        return Objects.requireNonNull(expressionResolver.resolveValue(annotation.queue()),
                "Queue name cannot be null. Define queue property of @RabbitRpcInterface annotation for " + iClass.getName());
    }

    private String resolveRouting(Object bean) {
        Class<?> iClass = getRabbitRpcInterface(bean);
        RabbitRpcInterface annotation = Objects.requireNonNull(iClass.getAnnotation(RabbitRpcInterface.class));
        return Objects.requireNonNull(expressionResolver.resolveValue(annotation.routing()),
                "Routing cannot be null. Define routing property of @RabbitRpcInterface annotation for " + iClass.getName());
    }

    @SneakyThrows
    private void createMessageListenerContainer(Queue queue) {
        MethodRabbitListenerEndpoint endpoint = new MethodRabbitListenerEndpoint();
        RabbitRpcValidator rpcValidator = new RabbitRpcValidator(validator, getServiceName());
        RabbitRpcMessageHandler handler = new RabbitRpcMessageHandler(rpcValidator, messageConverter, methodHandles);
        Method handleMethod = handler.getClass().getMethod(HANDLER_METHOD_NAME, Message.class, Channel.class, MessageProperties.class);
        endpoint.setId(queue.getName() + "-" + getServiceName());
        endpoint.setQueues(queue);
        endpoint.setBean(handler);
        endpoint.setMethod(handleMethod);
        endpoint.setErrorHandler(errorHandler);
        endpoint.setMessageHandlerMethodFactory(messageHandlerMethodFactory);
        endpoint.setBeanFactory(applicationContext);
        endpoint.setAdmin(amqpAdmin);
        RabbitListenerEndpointRegistry registry = applicationContext.getBean(
                RabbitListenerConfigUtils.RABBIT_LISTENER_ENDPOINT_REGISTRY_BEAN_NAME,
                RabbitListenerEndpointRegistry.class);
        registry.registerListenerContainer(endpoint, rabbitListenerContainerFactory);
    }

    private RabbitListenerErrorHandler getErrorHandler(EnableRabbitRpc annotation, Map<Class<?>, Map<Method, MethodHandle>> methodHandles) {
        String errorHandlerBeanName = expressionResolver.resolveValue(annotation.errorHandler());
        if (errorHandlerBeanName != null && !errorHandlerBeanName.isBlank()) {
            return applicationContext.getBean(errorHandlerBeanName, RabbitListenerErrorHandler.class);
        } else {
            RabbitRpcErrorMapping errorMapping = getErrorMapping();
            return new RabbitRpcErrorHandler(getServiceName(), errorMapping, methodHandles);
        }
    }

    private MessageConverter getRpcMessageConverter(EnableRabbitRpc annotation) {
        String converterBeanName = expressionResolver.resolveValue(annotation.messageConverter());
        if (converterBeanName != null && !converterBeanName.isBlank()) {
            return applicationContext.getBean(converterBeanName, MessageConverter.class);
        } else {
            ForyMessageConverter converter;
            List<Integer> concurrency = getConcurrency(annotation);
            if (concurrency.isEmpty()) {
                converter = new ForyMessageConverter();
            } else if (concurrency.size() == 1) {
                converter = new ForyMessageConverter(concurrency.get(0));
            } else {
                converter = new ForyMessageConverter(concurrency.get(0), concurrency.get(1));
            }
            return converter;
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
