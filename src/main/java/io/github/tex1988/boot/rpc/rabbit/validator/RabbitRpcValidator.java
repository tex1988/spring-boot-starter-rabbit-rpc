package io.github.tex1988.boot.rpc.rabbit.validator;

import io.github.tex1988.boot.rpc.rabbit.constant.ErrorStatusCode;
import io.github.tex1988.boot.rpc.rabbit.exception.RabbitRpcServiceValidationException;
import io.github.tex1988.boot.rpc.rabbit.util.Utils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A utility class for validating method arguments and objects in Rabbit RPC services.
 * <p>
 * This class leverages the {@link Validator} framework to perform constraint validation
 * on method parameters and objects, ensuring that they comply with specified validation rules.
 * </p>
 *
 * <p>If validation fails, a {@link RabbitRpcServiceValidationException} is thrown,
 * containing details about the violations.</p>
 *
 * @author tex1988
 * @since 2024-04-12
 */
@AllArgsConstructor
public class RabbitRpcValidator {

    /**
     * The validator used for performing validation operations.
     */
    private final Validator validator;
    private final String serviceName;

    /**
     * Validates the arguments of a method and its associated constraints.
     *
     * @param args   the method arguments to validate
     * @param method the method being invoked
     * @param iClazz the interface class that declares the method
     * @throws RabbitRpcServiceValidationException if any validation constraints are violated
     */
    public void validate(Object[] args, Method method, Class<?> iClazz) {
        List<ConstraintViolation<Object>> violations = new ArrayList<>();
        Annotation[][] annotationsArr = method.getParameterAnnotations();

        // Validate constraints defined on the method arguments
        validateArgConstraints(iClazz, method, args, violations);

        // Validate constraints defined on objects in the arguments
        validateObjectConstraints(args, annotationsArr, violations);

        // If there are validation errors, throw an exception
        if (!violations.isEmpty()) {
            Map<String, String> bindingResult = new HashMap<>();
            violations.forEach(violation -> {
                String validationMessage = violation.getMessage();
                String fieldName = getFieldName(violation);
                bindingResult.put(fieldName, validationMessage);
            });
            String errorMessage = "Validation failed for fields: " + String.join(", ",
                    bindingResult.keySet().stream().sorted().toList());
            throw new RabbitRpcServiceValidationException(Utils.getTimestamp(), serviceName,
                    ErrorStatusCode.BAD_REQUEST.getCode(),
                    errorMessage, bindingResult);
        }
    }

    private String getFieldName(ConstraintViolation<Object> violation) {
        String name = violation.getPropertyPath().toString();
        if (violation.getRootBean() instanceof Proxy) {
            name = name.split("\\.")[1];
        } else {
            name = violation.getRootBean().getClass().getSimpleName() + "." + name;
        }
        return name;
    }

    private void validateArgConstraints(Class<?> serviceInterface, Method method, Object[] args,
                                        List<ConstraintViolation<Object>> violations) {
        Object proxyInstance = Proxy.newProxyInstance(
                serviceInterface.getClassLoader(),
                new Class<?>[]{serviceInterface},
                (proxy, proxyMethod, methodArgs) -> null
        );

        violations.addAll(validator.forExecutables()
                .validateParameters(proxyInstance, method, args));
    }

    private void validateObjectConstraints(Object[] args, Annotation[][] annotationsArr,
                                           List<ConstraintViolation<Object>> violations) {
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            Annotation[] annotations = annotationsArr[i];

            Arrays.stream(annotations)
                    .filter(annotation -> annotation.annotationType() == Validated.class ||
                            annotation.annotationType() == Valid.class)
                    .findFirst().ifPresent(a -> {
                        if (a instanceof Validated validated) {
                            violations.addAll(validator.validate(arg, validated.value()));
                        } else if (a instanceof Valid) {
                            violations.addAll(validator.validate(arg));
                        }
                    });
        }
    }
}
