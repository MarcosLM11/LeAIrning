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
@Transactional(readOnly = true)
public class UsersService {

    private final UsersRepository usersRepository;
    private final UsersMapper usersMapper;

    public UsersService(UsersRepository usersRepository, UsersMapper usersMapper) {
        this.usersRepository = usersRepository;
        this.usersMapper = usersMapper;
    }

    public List<UserResponseDto> findAll() {
        log.debug("Fetching all users");
        return usersRepository.findAll()
                .stream()
                .map(usersMapper::toDto)
                .toList();
    }

    public Page<UserResponseDto> findAll(Pageable pageable) {
        log.debug("Fetching users with pagination: page {}, size {}", pageable.getPageNumber(), pageable.getPageSize());
        return usersRepository.findAll(pageable)
                .map(usersMapper::toDto);
    }

    public UserResponseDto getUserById(UUID userId) {
        log.debug("Fetching user with ID: {}", userId);
        return usersRepository.findById(userId)
                .map(usersMapper::toDto)
                .orElseThrow(() -> new RuntimeException(""+userId));
    }

    @Transactional
    public UserResponseDto createUser(CreateUserRequestDto request) {
        log.debug("Creating user with email: {}", request.email());
        UserEntity userEntity = usersMapper.toEntity(request);
        userEntity.setPassword(request.password());
        UserEntity savedUser = usersRepository.save(userEntity);
        log.info("User created with ID: {}", savedUser.getId());
        return usersMapper.toDto(savedUser);
    }

    @Transactional
    public UserResponseDto updateUser(UUID userId, UpdateUserRequestDto request) {
        log.debug("Updating user with ID: {}", userId);
        UserEntity existingUser = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException(""+userId));
        usersMapper.updateEntityFromDto(request, existingUser);
        UserEntity updatedUser = usersRepository.save(existingUser);
        log.info("User updated with ID: {}", updatedUser.getId());
        return usersMapper.toDto(updatedUser);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        log.debug("Deleting user with ID: {}", userId);
        if (!usersRepository.existsById(userId)) {
            throw new RuntimeException("User not found with ID: " + userId);
        }
        usersRepository.deleteById(userId);
        log.info("User deleted with ID: {}", userId);
    }


}
