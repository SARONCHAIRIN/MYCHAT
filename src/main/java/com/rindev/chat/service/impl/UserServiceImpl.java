package com.rindev.chat.service.impl;

import com.rindev.chat.dto.request.UpdateAvatarRequest;
import com.rindev.chat.dto.request.UpdateUserRequest;
import com.rindev.chat.dto.response.UserPublicResponse;
import com.rindev.chat.dto.response.UserResponse;
import com.rindev.chat.entity.User;
import com.rindev.chat.exception.BadRequestException;
import com.rindev.chat.exception.ConflictException;
import com.rindev.chat.exception.ResourceNotFoundException;
import com.rindev.chat.mapper.UserMapper;
import com.rindev.chat.repository.UserRepository;
import com.rindev.chat.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserServiceImpl(
            UserRepository userRepository,
            UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    public Page<UserPublicResponse> getUsers(int page, int size) {
        Pageable pageable = createPageable(page, size);

        return userRepository
                .findAll(pageable)
                .map(userMapper::toPublicResponse);
    }

    @Override
    public UserPublicResponse getUserById(Long id) {
        User user = findUser(id);

        return userMapper.toPublicResponse(user);
    }

    @Override
    public Page<UserPublicResponse> searchUsers(
            String query,
            int page,
            int size) {
        if (query == null || query.isBlank()) {
            throw new BadRequestException("Search query is required");
        }

        Pageable pageable = createPageable(page, size);

        String normalizedQuery = query.trim();

        return userRepository
                .findByNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(
                        normalizedQuery,
                        normalizedQuery,
                        pageable)
                .map(userMapper::toPublicResponse);
    }

    @Override
    public UserResponse getMe(Long userId) {
        return userMapper.toResponse(findUser(userId));
    }

    @Override
    @Transactional
    public UserResponse updateMe(
            Long userId,
            UpdateUserRequest request) {
        User user = findUser(userId);

        if (request.name() != null) {
            String name = request.name().trim();

            if (name.isEmpty()) {
                throw new BadRequestException("Name must not be blank");
            }

            user.setName(name);
        }

        if (request.username() != null) {
            String username = request.username().trim();

            if (username.isEmpty()) {
                throw new BadRequestException("Username must not be blank");
            }

            if (userRepository.existsByUsernameAndIdNot(username, userId)) {
                throw new ConflictException("Username is already in use");
            }

            user.setUsername(username);
        }

        if (request.email() != null) {
            String email = normalizeNullable(request.email());

            if (email != null
                    && userRepository.existsByEmailAndIdNot(email, userId)) {
                throw new ConflictException("Email is already in use");
            }

            user.setEmail(email);
        }

        if (request.phone() != null) {
            String phone = normalizeNullable(request.phone());

            if (phone != null
                    && userRepository.existsByPhoneAndIdNot(phone, userId)) {
                throw new ConflictException("Phone is already in use");
            }

            user.setPhone(phone);
        }

        if (request.bio() != null) {
            user.setBio(normalizeNullable(request.bio()));
        }

        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse updateAvatar(
            Long userId,
            UpdateAvatarRequest request) {
        User user = findUser(userId);

        user.setAvatarUrl(request.avatarUrl().trim());

        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + id));
    }

    private Pageable createPageable(int page, int size) {
        if (page < 0) {
            throw new BadRequestException(
                    "Page must be greater than or equal to 0");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException(
                    "Size must be between 1 and " + MAX_PAGE_SIZE);
        }

        return PageRequest.of(page, size);
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }
}