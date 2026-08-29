package com.kim.tudu_api.user.mapper;

import com.kim.tudu_api.user.controller.dto.UserDto;
import com.kim.tudu_api.user.model.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {

    void updateEntity(UserDto dto, @MappingTarget UserEntity entity);

    UserDto toUserDto(UserEntity entity);

    UserEntity toUserEntity(UserDto dto);
}
