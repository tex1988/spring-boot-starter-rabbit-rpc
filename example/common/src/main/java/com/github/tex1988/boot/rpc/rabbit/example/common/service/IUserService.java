package io.github.tex1988.boot.rpc.rabbit.example.common.service;

import io.github.tex1988.boot.rpc.rabbit.annotation.RabbitRpcInterface;
import io.github.tex1988.boot.rpc.rabbit.example.common.model.User;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

@RabbitRpcInterface(exchange = "#{properties.getExchange()}",
        queue = "#{properties.getQueue()}",
        routing = "#{properties.getRouting()}")
public interface IUserService {

    User get(@Max(100) Long id);

    User create(@Validated(User.OnCreate.class) User user);

    User update(@Validated({User.OnCreate.class, User.OnUpdate.class}) User user);

    void delete(@NotNull Long id);
}
