package com.kim.tudu_api.user.service;

import com.kim.tudu_api.user.controller.dto.UserDto;
import com.kim.tudu_api.user.exception.UserAlreadyExistsException;
import com.kim.tudu_api.user.exception.UserNotFoundException;
import com.kim.tudu_api.user.mapper.UserMapper;
import com.kim.tudu_api.user.model.UserEntity;
import com.kim.tudu_api.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    private final UserMapper mapper;

    public UserDto getUserById(Long id) {
        UserEntity userEntity = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Cannot find user with id: " + id));

        return mapper.toUserDto(userEntity);
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(mapper::toUserDto)
                .toList();
    }

    public UserDto createUser(UserDto userDto) {
        List<UserEntity> entities = userRepository.findByEmailEqualsIgnoreCaseOrUsernameEqualsIgnoreCase(
                userDto.email(),
                userDto.username());

        if (!entities.isEmpty()) {
            throw new UserAlreadyExistsException("Username and/or email already in use");
        }

        UserEntity savedUser = userRepository.save(mapper.toUserEntity(userDto));
        log.debug("savedUser: {}", savedUser);

        return mapper.toUserDto(savedUser);
    }

    public UserDto updateUser(UserDto userDto) {
        UserEntity userEntity = userRepository.findById(userDto.id())
                .orElseThrow(() -> new UserNotFoundException("Cannot find user with id: " + userDto.id()));
        log.debug("userEntity to update: {}", userEntity);

        mapper.updateEntity(userDto, userEntity);
        log.debug("updated userEntity: {}", userEntity);

        return mapper.toUserDto(userRepository.save(userEntity));
    }

    public void deleteUser(Long id) {
        UserEntity userEntity = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Could not find user with id: " + id));

        log.debug("userEntity to delete: {}", userEntity);

        userRepository.delete(userEntity);
    }
}
