package io.github.tex1988.boot.rpc.rabbit.example.common.validation;

import io.github.tex1988.boot.rpc.rabbit.example.common.model.User;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UserValidator implements ConstraintValidator<ValidUser, User> {

    @Override
    public boolean isValid(User user, ConstraintValidatorContext context) {

        if (user == null) {
            return false;
        }

        context.disableDefaultConstraintViolation();

        if (user.isActive()) {
            if (user.getFirstName() == null || user.getLastName() == null ||
                    user.getUsername() == null || user.getEmail() == null || user.getPhone() == null) {
                context.buildConstraintViolationWithTemplate("isActive cannot be true without firstName, lastName, username, email, phone")
                        .addPropertyNode("isActive").addConstraintViolation();
                return false;
            }
        }
        return true;
    }
}
