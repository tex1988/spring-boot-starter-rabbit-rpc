package com.github.tex1988.boot.rpc.rabbit.example.client.web;

import com.github.tex1988.boot.rpc.rabbit.exception.RabbitRpcServiceException;
import com.github.tex1988.boot.rpc.rabbit.exception.RabbitRpcServiceValidationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.Objects;

@Slf4j
@RestControllerAdvice
@AllArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DefaultExceptionHandler {

    private final HttpServletRequest request;

    @ExceptionHandler(RabbitRpcServiceException.class)
    public ResponseEntity<Map<String, ?>> rpcRabbitException(RabbitRpcServiceException e) {
        log.warn("Error from service: {}, timestamp: {}, statusCode: {}, message: {}",
                e.getServiceName(), e.getTimestamp(), e.getStatusCode(), e.getMessage());

        HttpStatus status = Objects.requireNonNull(HttpStatus.resolve(e.getStatusCode()), "Unknown status code: " + e.getStatusCode());
        Map<String, Object> body = Map.of("timestamp", e.getTimestamp(),
                "error", status.getReasonPhrase(),
                "status", status.value(),
                "serviceName", e.getServiceName(),
                "message", e.getMessage(),
                "path", request.getServletPath());
        return ResponseEntity
                .status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @ExceptionHandler(RabbitRpcServiceValidationException.class)
    public ResponseEntity<Map<String, ?>> rpcRabbitValidationException(RabbitRpcServiceValidationException e) {
        log.warn("Error from service: {}, timestamp: {}, statusCode: {}, message: {}",
                e.getServiceName(), e.getTimestamp(), e.getStatusCode(), e.getMessage());

        HttpStatus status = Objects.requireNonNull(HttpStatus.resolve(e.getStatusCode()), "Unknown status code: " + e.getStatusCode());
        Map<String, Object> body = Map.of("timestamp", e.getTimestamp(),
                "error", status.getReasonPhrase(),
                "status", status.value(),
                "serviceName", e.getServiceName(),
                "message", e.getMessage(),
                "violations", e.getBindingResult(),
                "path", request.getServletPath());
        return ResponseEntity
                .status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }
}
