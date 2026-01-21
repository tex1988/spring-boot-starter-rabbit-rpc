package io.github.tex1988.boot.rpc.rabbit.integration.unregistered;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Test class intentionally placed outside the allowed serialization patterns.
 * Used to test serialization security and error handling for unregistered classes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnregisteredClass implements Serializable {

    private Long id;
    private String data;
    private boolean sensitive;
}
