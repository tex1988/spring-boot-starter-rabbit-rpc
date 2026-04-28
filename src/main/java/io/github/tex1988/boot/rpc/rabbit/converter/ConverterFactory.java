package io.github.tex1988.boot.rpc.rabbit.converter;

import io.github.tex1988.boot.rpc.rabbit.rabbit.RabbitRpcBeanExpressionResolver;
import io.github.tex1988.boot.rpc.rabbit.util.Utils;
import lombok.AllArgsConstructor;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.ApplicationContext;

import java.util.List;

@AllArgsConstructor
public class ConverterFactory {

    private static final Integer DEFAULT_POOL_SIZE = 4;

    private final ApplicationContext applicationContext;
    private final RabbitRpcBeanExpressionResolver expressionResolver;

    public MessageConverter getConverter(String beanExpression, String[] patterns, List<Integer> concurrency, int poolSize) {
        String converterBeanName = expressionResolver.resolveValue(beanExpression);
        if (converterBeanName != null && !converterBeanName.isBlank()) {
            return applicationContext.getBean(converterBeanName, MessageConverter.class);
        } else {
            List<String> allowedSerializationClasses = Utils.getAllowedClassesNames(patterns);
            int actualPoolSize = poolSize == 0 ? getPoolSize(concurrency) : poolSize;
            return new ForyMessageConverter(actualPoolSize, allowedSerializationClasses);
        }
    }

    private static Integer getPoolSize(List<Integer> concurrency) {
        if (concurrency.isEmpty() || concurrency.get(0) < DEFAULT_POOL_SIZE) {
            return DEFAULT_POOL_SIZE;
        }

        if (concurrency.size() == 1 || concurrency.get(0) > DEFAULT_POOL_SIZE) {
            return concurrency.get(0) + 1;
        }

        if (concurrency.size() == 2 || concurrency.get(1) < DEFAULT_POOL_SIZE) {
            return DEFAULT_POOL_SIZE;
        }

        return concurrency.get(1) + 1;
    }
}
