package com.kim.tudu_api.auth;

import com.kim.tudu_api.auth.controller.AuthController;
import com.kim.tudu_api.auth.domain.AuthenticatedUser;
import com.kim.tudu_api.user.model.UserEntity;
import com.kim.tudu_api.user.service.UserService;
import com.kim.tudu_api.util.JwtService;
import com.kim.tudu_api.util.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
public class AuthControllerTest {

    private static final String TEST_TOKEN = "jwt-token";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AuthenticationManager manager;

    @MockitoBean
    UserService userService;

    @MockitoBean
    JwtService jwtService;

    @Test
    void shouldReturnToken() throws Exception {
        UserEntity userEntity = TestUsers.USER1;

        AuthenticatedUser user = new AuthenticatedUser(
                userEntity.getId(),
                userEntity.getUsername(),
                userEntity.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities());

        when(manager.authenticate(any())).thenReturn(authentication);
        when(jwtService.generateToken(user)).thenReturn(TEST_TOKEN);

        mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"username\":\"%s\",\"password\":\"%s\"}",
                        userEntity.getUsername(),
                        userEntity.getPassword())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(TEST_TOKEN));
    }

    @Test
    void shouldRejectInvalidCredentials() throws Exception {

        when(manager.authenticate(any())).thenThrow(new BadCredentialsException("Invalid credentials"));

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "username":"john",
                      "password":"wrong"
                    }
                    """))
                .andExpect(status().isUnauthorized());
    }
}
