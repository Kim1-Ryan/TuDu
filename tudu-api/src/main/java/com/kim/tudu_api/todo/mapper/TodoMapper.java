package com.kim.tudu_api.todo.mapper;

import com.kim.tudu_api.todo.controller.dto.*;
import com.kim.tudu_api.todo.model.BoardEntity;
import com.kim.tudu_api.todo.model.TodoItemEntity;
import com.kim.tudu_api.todo.model.TodoListEntity;
import com.kim.tudu_api.todo.model.UserBoardLinkEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TodoMapper {

    @Mapping(target = "lists", source = "todoLists")
    @Mapping(target = "users", source = "userLinks")
    BoardDto toDto(BoardEntity entity);

    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "admin", source = "user.admin")
    BoardUserDto toDto(UserBoardLinkEntity entity);

    @Mapping(target = "items", source = "todoItems")
    TodoListDto toDto(TodoListEntity entity);

    TodoItemDto toDto(TodoItemEntity entity);

    @Mapping(target = "userLinks", ignore = true)
    @Mapping(target = "todoLists", ignore = true)
    void updateBoardEntity(@MappingTarget BoardEntity entity, UpdateBoardRequest request);
}
