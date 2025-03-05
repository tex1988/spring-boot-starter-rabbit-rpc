package io.github.tex1988.boot.rpc.rabbit.example.server;

import io.github.tex1988.boot.rpc.rabbit.constant.ErrorStatusCode;
import io.github.tex1988.boot.rpc.rabbit.example.server.exception.NotFoundException;
import io.github.tex1988.boot.rpc.rabbit.model.RabbitRpcErrorMapping;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

    @Bean
    public RabbitRpcErrorMapping rabbitRpcErrorMapping() {
        return new RabbitRpcErrorMapping() {{
            put(NotFoundException.class, ErrorStatusCode.NOT_FOUND);
        }};
    }
}
