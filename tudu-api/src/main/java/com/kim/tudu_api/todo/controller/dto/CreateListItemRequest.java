package com.kim.tudu_api.todo.controller.dto;

import com.kim.tudu_api.util.validation.max_length.MaxLength;
import com.kim.tudu_api.util.validation.min_length.MinLength;
import lombok.NonNull;

public record CreateListItemRequest(
        @NonNull Long listId,
        @NonNull @MinLength(3) @MaxLength(255) String description) {
}
