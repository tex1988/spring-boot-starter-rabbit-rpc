package io.github.tex1988.boot.rpc.rabbit.example.common.model;

import io.github.tex1988.boot.rpc.rabbit.example.common.validation.ValidUser;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
@ValidUser(groups = {User.OnCreate.class, User.OnUpdate.class})
public class User implements Serializable {

    @NotNull(groups = {OnCreate.class, OnUpdate.class}, message = "firstName value must not be null")
    @Size(groups = {OnCreate.class, OnUpdate.class}, min = 3, max = 10,
            message = "firstName value '${validatedValue}' length is out of range {min} - {max} symbols")
    private String firstName;

    @NotNull(groups = {OnCreate.class, OnUpdate.class}, message = "lastName value must not be null")
    @Size(groups = {OnCreate.class, OnUpdate.class}, min = 3, max = 10,
            message = "firstName value '${validatedValue}' length is out of range {min} - {max} symbols")
    private String lastName;

    @NotNull(groups = {OnCreate.class, OnUpdate.class}, message = "username value must not be null")
    @Size(groups = {OnCreate.class, OnUpdate.class}, min = 3, max = 10,
            message = "firstName value '${validatedValue}' length is out of range {min} - {max} symbols")
    private String username;

    @NotNull(groups = {OnUpdate.class}, message = "email value must not be null")
    @Pattern(groups = {OnCreate.class, OnUpdate.class}, regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
            message = "email value '${validatedValue}' is not valid email")
    private String email;

    @NotNull(groups = {OnUpdate.class}, message = "phone value must not be null")
    @Pattern(groups = {OnCreate.class, OnUpdate.class}, regexp = "^\\+?\\d{10,15}$",
            message = "phone value '${validatedValue}' is not valid phone number")
    private String phone;

    private boolean isActive;

    public interface OnCreate {
    }

    public interface OnUpdate {
    }
}
