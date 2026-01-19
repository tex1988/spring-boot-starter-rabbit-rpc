package io.github.tex1988.boot.rpc.rabbit.integration.config;

import io.github.tex1988.boot.rpc.rabbit.converter.ForyMessageConverter;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Custom beans for testing @EnableRabbitRpc configuration parameters.
 * These are NOT annotated with @Component - they must be registered as @Bean methods.
 */
public class CustomConfigBeans {

    /**
     * Custom task executor that tracks how many tasks were executed.
     */
    @Getter
    public static class CustomTaskExecutor extends ThreadPoolTaskExecutor {

        private final AtomicInteger taskCount = new AtomicInteger(0);
        private final ExecutorService executorService;

        public CustomTaskExecutor() {
            executorService = Executors.newCachedThreadPool();
        }

        @Override
        public void execute(@NotNull Runnable task) {
            taskCount.incrementAndGet();
            executorService.execute(task);
        }

        public int getExecutedTaskCount() {
            return taskCount.get();
        }
    }

    /**
     * Custom message converter that tracks conversions.
     * Uses composition since ForyMessageConverter methods are final.
     */
    public static class CustomMessageConverter implements MessageConverter {

        private final AtomicInteger toMessageCount = new AtomicInteger(0);
        private final AtomicInteger fromMessageCount = new AtomicInteger(0);
        private final ForyMessageConverter delegate;

        public CustomMessageConverter() {
            // Create delegate internally to avoid circular dependency
            this.delegate = new ForyMessageConverter(2, 4);
        }

        @Override
        public @NotNull Message toMessage(@NotNull Object object, org.springframework.amqp.core.@NotNull MessageProperties messageProperties) throws org.springframework.amqp.support.converter.MessageConversionException {
            toMessageCount.incrementAndGet();
            return delegate.toMessage(object, messageProperties);
        }

        @Override
        public @NotNull Object fromMessage(@NotNull Message message) throws org.springframework.amqp.support.converter.MessageConversionException {
            fromMessageCount.incrementAndGet();
            return delegate.fromMessage(message);
        }

        public int getToMessageCount() {
            return toMessageCount.get();
        }

        public int getFromMessageCount() {
            return fromMessageCount.get();
        }
    }

    /**
     * Custom error handler that tracks errors.
     */
    public static class CustomErrorHandler implements RabbitListenerErrorHandler {

        private final AtomicInteger errorCount = new AtomicInteger(0);

        @Getter
        private volatile Throwable lastError;

        @Override
        public Object handleError(Message amqpMessage, org.springframework.messaging.Message<?> message,
                                  ListenerExecutionFailedException exception) {
            errorCount.incrementAndGet();
            lastError = exception;
            // Return null to indicate error was handled
            return null;
        }

        public int getErrorCount() {
            return errorCount.get();
        }
    }
}
