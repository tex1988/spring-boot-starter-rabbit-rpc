package com.github.tex1988.boot.rpc.rabbit.model;

import com.github.tex1988.boot.rpc.rabbit.constant.ErrorStatusCode;

import java.util.LinkedHashMap;

/**
 * A specialized {@link LinkedHashMap} for mapping exception types to {@link ErrorStatusCode} values.
 * <p>
 * This class is used to define custom mappings between specific exception types
 * and their corresponding {@link ErrorStatusCode} values. The order of entries in the map
 * is maintained, allowing for predictable iteration.
 * </p>
 * <p>
 * Use this class to configure how exceptions are translated into error codes
 * in Rabbit RPC error handling.
 * </p>
 *
 * @author tex1988
 * @see ErrorStatusCode
 * @see com.github.tex1988.boot.rpc.rabbit.rabbit.RabbitRpcErrorHandler
 * @since 2024-05-12
 */
public class RabbitRpcErrorMapping extends LinkedHashMap<Class<? extends Throwable>, ErrorStatusCode> {
}
