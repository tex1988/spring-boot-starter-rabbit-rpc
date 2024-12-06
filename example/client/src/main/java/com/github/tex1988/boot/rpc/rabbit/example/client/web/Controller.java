package com.github.tex1988.boot.rpc.rabbit.example.client.web;

import com.github.tex1988.boot.rpc.rabbit.example.common.model.Group;
import com.github.tex1988.boot.rpc.rabbit.example.common.model.User;
import com.github.tex1988.boot.rpc.rabbit.example.common.service.IGroupService;
import com.github.tex1988.boot.rpc.rabbit.example.common.service.IUserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@AllArgsConstructor
@RestController()
public class Controller {

    private final IUserService userService;
    private final IGroupService groupService;

    @GetMapping("/user/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.get(id);
    }

    @PostMapping("/user")
    public User createUser(@RequestBody User user) {
        return userService.create(user);
    }

    @PutMapping("/user")
    public User updateUser(@RequestBody User user) {
        return userService.update(user);
    }

    @DeleteMapping("/user/{id}")
    public void deleteUser(@PathVariable Long id) {
        userService.delete(id);
    }

    @GetMapping("/group/{id}")
    public Group getGroup(@PathVariable Long id) {
        return groupService.getGroup(id);
    }

    @PostMapping("/group/{name}/add")
    public void addUser(@PathVariable String name, @RequestBody User user) {
        groupService.addUser(name, user);
    }

    @PostMapping("/group/{name}/process")
    public void processGroup(@PathVariable String name) {
        groupService.processGroup(name);
        log.info("Group {} processed", name);
    }
}
