package io.github.tex1988.boot.rpc.rabbit.example.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UserValidator.class)
public @interface ValidUser {

    String message() default "Invalid payload";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
