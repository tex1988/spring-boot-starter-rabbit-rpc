package com.github.tex1988.boot.rpc.rabbit.exception;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class RabbitRpcServiceValidationException extends RabbitRpcServiceException {

    private Map<String, String> bindingResult;

    public RabbitRpcServiceValidationException(Long timestamp, String serviceName,
                                               Integer statusCode, String message, Map<String, String> bindingResult) {
        super(timestamp, serviceName, statusCode, message);
        this.bindingResult = bindingResult;
    }

    public RabbitRpcServiceValidationException(Long timestamp, String serviceName, Integer statusCode, String message) {
        super(timestamp, serviceName, statusCode, message);
    }
}
