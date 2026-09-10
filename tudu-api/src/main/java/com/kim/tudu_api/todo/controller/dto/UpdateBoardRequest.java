package com.kim.tudu_api.todo.controller.dto;

import com.kim.tudu_api.util.validation.max_length.MaxLength;
import com.kim.tudu_api.util.validation.min_length.MinLength;
import lombok.NonNull;

public record UpdateBoardRequest(
        @NonNull Long id,
        @NonNull @MinLength(3) @MaxLength(30) String name) {
}
