package com.kim.tudu_api.todo.controller.dto;

import com.kim.tudu_api.todo.model.BoardPermission;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class BoardUserDto {

    private Long id;
    private String username;
    private boolean admin;
    private BoardPermission permissionLevel;
}
