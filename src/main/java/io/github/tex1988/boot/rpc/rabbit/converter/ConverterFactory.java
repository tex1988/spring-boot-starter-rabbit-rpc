package io.github.tex1988.boot.rpc.rabbit.converter;

import io.github.tex1988.boot.rpc.rabbit.rabbit.RabbitRpcBeanExpressionResolver;
import io.github.tex1988.boot.rpc.rabbit.util.Utils;
import lombok.AllArgsConstructor;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
public class ConverterFactory {

    private static final Integer DEFAULT_MIN_POOL_SIZE = 2;
    private static final Integer DEFAULT_MAX_POOL_SIZE = 200;

    private final ApplicationContext applicationContext;
    private final RabbitRpcBeanExpressionResolver expressionResolver;

    public MessageConverter getConverter(String beanExpression, String[] patterns, List<Integer> concurrency) {
        String converterBeanName = expressionResolver.resolveValue(beanExpression);
        if (converterBeanName != null && !converterBeanName.isBlank()) {
            return applicationContext.getBean(converterBeanName, MessageConverter.class);
        } else {
            List<String> allowedSerializationClasses = Utils.getAllowedClassesNames(patterns);
            Integer minPoolSize = getMinPoolSize(concurrency);
            Integer maxPoolSize = getMaxPoolSize(concurrency);
            return new ForyMessageConverter(minPoolSize, maxPoolSize, allowedSerializationClasses);
        }
    }

    private Integer getMinPoolSize(List<Integer> concurrency) {
        if (concurrency.isEmpty()) {
            return DEFAULT_MIN_POOL_SIZE;
        } else {
            return concurrency.get(0);
        }
    }

    private Integer getMaxPoolSize(List<Integer> concurrency) {
        if (concurrency.size() < 2) {
            return DEFAULT_MAX_POOL_SIZE;
        } else if (concurrency.get(1) > 200) {
            return concurrency.get(1);
        } else {
            return DEFAULT_MAX_POOL_SIZE;
        }
    }
}
