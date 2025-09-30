package com.marcos.usersservice.service;

import com.marcos.usersservice.dto.CreateUserRequestDto;
import com.marcos.usersservice.dto.UpdateUserRequestDto;
import com.marcos.usersservice.dto.UserResponseDto;
import com.marcos.usersservice.entity.UserEntity;
import com.marcos.usersservice.repository.UsersRepository;
import com.marcos.usersservice.utils.UsersMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class UsersService {

    private final UsersRepository usersRepository;
    private final UsersMapper usersMapper;

    public UsersService(UsersRepository usersRepository, UsersMapper usersMapper) {
        this.usersRepository = usersRepository;
        this.usersMapper = usersMapper;
    }

    @Transactional(readOnly = true)
    public List<UserResponseDto> findAll() {
        log.debug(" Fetching all users");
        List<UserEntity> users = usersRepository.findAll();
        return usersMapper.toDtoList(users);
    }

    public Page<UserResponseDto> findAll(Pageable pageable) {
        log.debug(" Fetching users with pagination: page {}, size {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<UserEntity> usersPage = usersRepository.findAll(pageable);
        return usersPage.map(usersMapper::toDto);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUserById(UUID userId) {
        log.debug(" Fetching user with ID: {}", userId);
        UserEntity userEntity = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        return usersMapper.toDto(userEntity);
    }

    @Transactional
    public UserResponseDto createUser(CreateUserRequestDto request) {
        log.debug(" Creating user with email: {}", request.email());
        UserEntity userEntity = usersMapper.toEntity(request);
        userEntity.setPassword(request.password());
        UserEntity savedUser = usersRepository.save(userEntity);
        return usersMapper.toDto(savedUser);
    }

    @Transactional
    public UserResponseDto updateUser(UUID userId, UpdateUserRequestDto request) {
        log.debug(" Updating user with ID: {}", userId);
        UserEntity existingUser = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        usersMapper.updateEntityFromDto(request, existingUser);
        UserEntity updatedUser = usersRepository.save(existingUser);
        return usersMapper.toDto(updatedUser);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        log.debug(" Deleting user with ID: {}", userId);
        if (!usersRepository.existsById(userId)) {
            throw new RuntimeException("User not found with ID: " + userId);
        }
        usersRepository.deleteById(userId);
    }


}
