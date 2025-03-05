package io.github.tex1988.boot.rpc.rabbit.example.server;

import io.github.tex1988.boot.rpc.rabbit.annotation.EnableRabbitRpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@EnableRabbitRpc(enableServer = true,
        scanBasePackages = {"io.github.tex1988.boot.rpc.rabbit.example.common.service"},
        allowedSerializationPatterns = {"io.github.tex1988.boot.rpc.rabbit.example.common.model.*"},
        concurrency = "5-10"
)
@SpringBootApplication(scanBasePackages = {"io.github.tex1988"})
@PropertySource({
        "classpath:application.properties",
})
public class ServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }
}
