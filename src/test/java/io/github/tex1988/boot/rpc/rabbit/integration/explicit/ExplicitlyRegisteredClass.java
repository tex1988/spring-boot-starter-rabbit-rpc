package io.github.tex1988.boot.rpc.rabbit.integration.explicit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Test model class for testing explicit full-name registration.
 * This class should be registered by its full qualified name, not by package pattern.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExplicitlyRegisteredClass implements Serializable {
    private Long id;
    private String data;
    private Integer value;
}
