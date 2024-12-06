package com.github.tex1988.boot.rpc.rabbit.example.server.service;

import com.github.tex1988.boot.rpc.rabbit.annotation.RabbitRpcService;
import com.github.tex1988.boot.rpc.rabbit.example.common.model.User;
import com.github.tex1988.boot.rpc.rabbit.example.common.service.IUserService;
import lombok.SneakyThrows;

@RabbitRpcService
public class UserService implements IUserService {

    @Override
    public User get(Long id) {
        return new User("testFirstName", "testLastName",
                "user1", "user1@test.com", "+123456789", true);
    }

    @Override
    public User create(User user) {
        return user;
    }

    @Override
    public User update(User user) {
        return user;
    }

    @Override
    @SneakyThrows
    public void delete(Long id) {
        Thread.sleep(1000);
    }
}
