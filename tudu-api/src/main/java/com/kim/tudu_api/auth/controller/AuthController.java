package com.kim.tudu_api.auth.controller;

import com.kim.tudu_api.auth.controller.dto.LoginRequest;
import com.kim.tudu_api.auth.controller.dto.LoginResponse;
import com.kim.tudu_api.auth.controller.dto.RegisterRequest;
import com.kim.tudu_api.auth.domain.AuthenticatedUser;
import com.kim.tudu_api.user.controller.dto.UserDto;
import com.kim.tudu_api.user.service.UserService;
import com.kim.tudu_api.util.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final static BCryptPasswordEncoder ENCODER =
            new BCryptPasswordEncoder(BCryptPasswordEncoder.BCryptVersion.$2A, 10);

    private final JwtService jwtService;

    private final UserService userService;

    private final AuthenticationManager manager;

    @GetMapping("/greeting")
    public String greeting() {
        return "Greetings from the API!";
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        log.info("Request to login user: {}", request.username());

        try {
            Authentication authentication = manager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));

            AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
            if (user == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Could not log in user: " + request.username());
            }

            return new LoginResponse(jwtService.generateToken(user));
        }
        catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials for: " + request.username());
        }
    }

    @PostMapping("/register")
    public void register(@RequestBody RegisterRequest request) {
        log.info("Request to register user: {}", request);

        UserDto user = userService.createUser(UserDto.builder()
                .username(request.username())
                .email(request.email())
                .password(ENCODER.encode(request.password()))
                .build());

        log.info("User created with id: {}", user.id());
    }
}
