package io.github.tex1988.boot.rpc.rabbit.integration.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Simple test message class for fire-and-forget testing.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestMessage implements Serializable {

    @NotBlank(message = "Content cannot be blank")
    private String content;

    private long timestamp;
}

