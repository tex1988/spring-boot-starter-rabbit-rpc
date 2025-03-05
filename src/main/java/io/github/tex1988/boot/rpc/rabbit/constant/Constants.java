package io.github.tex1988.boot.rpc.rabbit.constant;

import java.util.List;

public class Constants {

    public static final String SERVICE_HEADER = "__Service__";
    public static final String METHOD_HEADER = "__Method__";
    public static final String TYPE_ID_HEADER = "__TypeId__";

    public static final String HANDLER_METHOD_NAME = "handleMessage";
    public static final String RPC_RABBIT_TEMPLATE_BEAN_NAME = "rabbitRpcRabbitTemplate";
    public static final List<String> DEFAULT_ALLOWED_SERIALIZATION_PATTERNS = List.of("java.lang.*",
            "java.util.*",
            "io.github.tex1988.boot.rpc.rabbit.model.*");
}
