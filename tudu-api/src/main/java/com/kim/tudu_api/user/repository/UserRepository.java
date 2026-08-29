package com.kim.tudu_api.user.repository;

import com.kim.tudu_api.user.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    UserEntity getByUsernameEqualsIgnoreCase(String username);

    List<UserEntity> findByEmailEqualsIgnoreCaseOrUsernameEqualsIgnoreCase(String email, String username);
}
