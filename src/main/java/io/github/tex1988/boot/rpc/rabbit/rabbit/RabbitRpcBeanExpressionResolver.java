package io.github.tex1988.boot.rpc.rabbit.rabbit;

import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

public class RabbitRpcBeanExpressionResolver {

    private final BeanExpressionResolver expressionResolver;
    private final BeanExpressionContext expressionContext;

    public RabbitRpcBeanExpressionResolver(ApplicationContext applicationContext) {
        if (applicationContext instanceof ConfigurableApplicationContext configurableContext) {
            ConfigurableListableBeanFactory beanFactory = configurableContext.getBeanFactory();
            this.expressionResolver = beanFactory.getBeanExpressionResolver();
            this.expressionContext = new BeanExpressionContext(beanFactory, null);
        } else {
            throw new IllegalStateException("Unsupported ApplicationContext type: " + applicationContext.getClass());
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T resolveValue(String value) {
        if (value.startsWith("${")) {
            return resolveProperty(value);
        } else {
            return (T) expressionResolver.evaluate(value, expressionContext);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T resolveProperty(String value) {
        if (value.contains(":#{")) {
            String result = expressionContext.getBeanFactory().resolveEmbeddedValue(value);
            String defaultExpression = value.substring(value.indexOf(":") + 1, value.length() - 1);
            if (result == null || !result.equals(defaultExpression)) {
                return (T) result;
            } else {
                return (T) expressionResolver.evaluate(defaultExpression, expressionContext);
            }
        } else {
            return (T) expressionContext.getBeanFactory().resolveEmbeddedValue(value);
        }
    }
}
