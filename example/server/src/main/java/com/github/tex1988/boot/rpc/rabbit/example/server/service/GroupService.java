package io.github.tex1988.boot.rpc.rabbit.example.server.service;

import io.github.tex1988.boot.rpc.rabbit.annotation.RabbitRpc;
import io.github.tex1988.boot.rpc.rabbit.example.common.model.Group;
import io.github.tex1988.boot.rpc.rabbit.example.common.model.User;
import io.github.tex1988.boot.rpc.rabbit.example.common.service.IGroupService;
import io.github.tex1988.boot.rpc.rabbit.example.server.exception.NotFoundException;
import org.springframework.stereotype.Service;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Service
@RabbitRpc
public class GroupService implements IGroupService {

    @Override
    public Group getGroup(Long id) {
        if (id > 10) {
            throw new NotFoundException("Group with id:" + id + " not found");
        }
        return new Group();
    }

    @Override
    public Group addUser(String name, User user) {
        return new Group(name, List.of(user));
    }

    @Override
    @SneakyThrows
    public void processGroup(String name) {
        log.info("Start processing group {}", name);
        Thread.sleep(5000);
        log.info("Finished processing group {}", name);
    }
}
