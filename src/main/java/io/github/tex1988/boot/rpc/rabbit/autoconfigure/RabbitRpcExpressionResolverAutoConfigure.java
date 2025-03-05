package io.github.tex1988.boot.rpc.rabbit.autoconfigure;

import io.github.tex1988.boot.rpc.rabbit.rabbit.RabbitRpcBeanExpressionResolver;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
public class RabbitRpcExpressionResolverAutoConfigure {

    private final ApplicationContext applicationContext;

    @Bean
    public RabbitRpcBeanExpressionResolver expressionResolver() {
        return new RabbitRpcBeanExpressionResolver(applicationContext);
    }
}
