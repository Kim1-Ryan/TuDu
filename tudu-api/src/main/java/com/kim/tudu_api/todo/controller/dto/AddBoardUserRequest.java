package com.kim.tudu_api.todo.controller.dto;

import com.kim.tudu_api.todo.model.BoardPermission;
import lombok.NonNull;

public record AddBoardUserRequest(@NonNull Long boardId, @NonNull Long userId, @NonNull BoardPermission boardPermission) {
}
