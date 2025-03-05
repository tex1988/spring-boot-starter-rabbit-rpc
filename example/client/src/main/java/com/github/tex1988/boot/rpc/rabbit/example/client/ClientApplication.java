package io.github.tex1988.boot.rpc.rabbit.example.client;

import io.github.tex1988.boot.rpc.rabbit.annotation.EnableRabbitRpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@EnableRabbitRpc(enableClient = true,
        scanBasePackages = {"io.github.tex1988.boot.rpc.rabbit.example.common.service"},
        allowedSerializationPatterns = {"io.github.tex1988.boot.rpc.rabbit.example.common.model.*"},
        replyTimeout = 10000L
)
@SpringBootApplication(scanBasePackages = {"io.github.tex1988"})
@PropertySource({
        "classpath:application.properties",
})
public class ClientApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }
}
