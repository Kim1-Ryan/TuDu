package com.kim.tudu_api.todo.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class TodoListDto {

    private Long id;
    private String name;
    private List<TodoItemDto> items;
}
