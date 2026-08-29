package com.kim.tudu_api.util.error;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ErrorResponse(int status, String message, String reasonPhrase, LocalDateTime timestamp) {
}
