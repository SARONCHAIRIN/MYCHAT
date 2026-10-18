package com.rindev.chat.service;

import com.rindev.chat.dto.request.UpdateAvatarRequest;
import com.rindev.chat.dto.request.UpdateUserRequest;
import com.rindev.chat.dto.response.UserPublicResponse;
import com.rindev.chat.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

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

    UserResponse uploadAvatar(Long userId, MultipartFile file);

    UserResponse updateAvatar(
            Long userId,
            UpdateAvatarRequest request);
}