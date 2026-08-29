package com.kim.tudu_api.auth.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@AllArgsConstructor
@Getter
public class AuthenticatedUser implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    public boolean isAccountNonExpired() {
        return true; // TODO
    }

    public boolean isAccountNonLocked() {
        return true; // TODO
    }

    public boolean isCredentialsNonExpired() {
        return true; // TODO
    }

    public boolean isEnabled() {
        return true; // TODO
    }
}
