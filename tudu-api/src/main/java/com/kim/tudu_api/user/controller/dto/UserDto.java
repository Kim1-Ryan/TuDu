package com.kim.tudu_api.user.controller.dto;

import lombok.Builder;

@Builder
public record UserDto(Long id, String username, String email, String password, boolean admin) {
}
