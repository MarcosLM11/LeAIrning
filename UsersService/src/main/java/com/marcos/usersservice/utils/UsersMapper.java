package com.marcos.usersservice.utils;

import com.marcos.usersservice.dto.CreateUserRequestDto;
import com.marcos.usersservice.dto.UpdateUserRequestDto;
import com.marcos.usersservice.dto.UserResponseDto;
import com.marcos.usersservice.entity.UserEntity;
import org.mapstruct.*;
import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface UsersMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    UserEntity toEntity(CreateUserRequestDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(UpdateUserRequestDto dto, @MappingTarget UserEntity entity);

    @Mapping(target = "roles", ignore = true)
    UserResponseDto toDto(UserEntity entity);

    List<UserResponseDto> toDtoList(List<UserEntity> entities);
}
