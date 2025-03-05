package io.github.tex1988.boot.rpc.rabbit.example.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Group implements Serializable {

    private String name;
    private List<User> users = new ArrayList<>();
}
