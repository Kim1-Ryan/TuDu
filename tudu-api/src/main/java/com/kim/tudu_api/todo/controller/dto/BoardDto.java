package com.kim.tudu_api.todo.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class BoardDto {

    private Long id;
    private String name;
    private List<BoardUserDto> users;
    private List<TodoListDto> lists;
}
