package com.kim.tudu_api.todo.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TodoItemDto {

    private Long id;
    private String description;
    private boolean completed;
}
