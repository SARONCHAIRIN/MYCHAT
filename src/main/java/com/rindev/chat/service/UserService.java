package com.rindev.chat.service;

import com.rindev.chat.dto.request.UpdateAvatarRequest;
import com.rindev.chat.dto.request.UpdateUserRequest;
import com.rindev.chat.dto.response.UserPublicResponse;
import com.rindev.chat.dto.response.UserResponse;
import org.springframework.data.domain.Page;

public interface UserService {

    Page<UserPublicResponse> getUsers(int page, int size);

    UserPublicResponse getUserById(Long id);

    Page<UserPublicResponse> searchUsers(
            String query,
            int page,
            int size);

    UserResponse getMe(Long userId);

    UserResponse updateMe(
            Long userId,
            UpdateUserRequest request);

    UserResponse updateAvatar(
            Long userId,
            UpdateAvatarRequest request);
}