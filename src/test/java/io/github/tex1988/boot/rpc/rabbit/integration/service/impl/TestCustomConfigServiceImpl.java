package io.github.tex1988.boot.rpc.rabbit.integration.service.impl;

import io.github.tex1988.boot.rpc.rabbit.annotation.RabbitRpc;
import io.github.tex1988.boot.rpc.rabbit.integration.service.ITestCustomConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementation for custom configuration test service.
 */
@Slf4j
@Service
@RabbitRpc
public class TestCustomConfigServiceImpl implements ITestCustomConfigService {

    @Override
    public String processWithCustomConfig(String input) {
        log.info("Processing with custom config: {}", input);
        return "Custom config result: " + input;
    }
}
