package com.kim.tudu_api.auth.service;

import com.kim.tudu_api.auth.domain.AuthenticatedUser;
import com.kim.tudu_api.user.model.UserEntity;
import com.kim.tudu_api.user.repository.UserRepository;
import com.kim.tudu_api.util.Authorities;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository repository;

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {

        UserEntity user = repository.getByUsernameEqualsIgnoreCase(username);
        if (user == null) {
            throw new UsernameNotFoundException("Could not find user: " + username);
        }

        List<String> authorityList = user.isAdmin()
                ? List.of(Authorities.ADMIN, Authorities.USER)
                : List.of(Authorities.USER);

        List<SimpleGrantedAuthority> authorities = authorityList.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        return new AuthenticatedUser(
                user.getId(),
                user.getPassword(),
                user.getPassword(),
                authorities);
    }
}
