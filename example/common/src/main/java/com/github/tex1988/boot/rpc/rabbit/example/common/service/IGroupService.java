package com.github.tex1988.boot.rpc.rabbit.example.common.service;

import com.github.tex1988.boot.rpc.rabbit.annotation.FireAndForget;
import com.github.tex1988.boot.rpc.rabbit.annotation.RabbitRpcInterface;
import com.github.tex1988.boot.rpc.rabbit.example.common.model.Group;
import com.github.tex1988.boot.rpc.rabbit.example.common.model.User;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;

@RabbitRpcInterface(exchange = "#{properties.getExchange()}",
        queue = "#{properties.getQueue()}",
        routing = "#{properties.getRouting()}")
public interface IGroupService {

    Group getGroup(@Min(1) @Max(100) Long id);

    Group addUser(@NotEmpty(message = "name value must not be empty or blank")
                  @Size(min = 3, max = 10, message = "name length '${validatedValue}' is out of range {min} - {max} characters")
                  String name,
                  @Validated({User.OnCreate.class, User.OnUpdate.class}) User user);

    @FireAndForget
    void processGroup(String name);
}
