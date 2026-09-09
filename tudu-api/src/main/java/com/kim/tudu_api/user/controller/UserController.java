package com.kim.tudu_api.user.controller;

import com.kim.tudu_api.user.controller.dto.UserDto;
import com.kim.tudu_api.user.service.UserService;
import com.kim.tudu_api.util.Authorities;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    @Secured(Authorities.USER)
    public UserDto getUserById(@PathVariable Long id) {
        log.info("Request to get user with id: {}", id);

        return userService.getUserById(id);
    }

    @GetMapping
    @Secured(Authorities.USER)
    public List<UserDto> getUsers() {
        log.info("Request to get all users");

        return userService.getAllUsers();
    }

    @PutMapping
    @Secured(Authorities.USER)
    public UserDto updateUser(@RequestBody UserDto userDto) {
        log.info("Request to update user: {}", userDto);

        return userService.updateUser(userDto);
    }

    @DeleteMapping("/{id}")
    @Secured(Authorities.ADMIN)
    public void deleteUser(@PathVariable Long id) {
        log.info("Request to delete user with id: {}", id);

        userService.deleteUser(id);
    }
}
