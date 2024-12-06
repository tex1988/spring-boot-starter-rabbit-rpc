package com.github.tex1988.boot.rpc.rabbit.example.common.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@AllArgsConstructor
@ConfigurationProperties(prefix = "com.github.tex1988.rpc")
public class RabbitRpcProperties {

    @NotNull
    private String exchange;

    @NotBlank
    private String queue;

    @NotBlank
    private String routing;
}
