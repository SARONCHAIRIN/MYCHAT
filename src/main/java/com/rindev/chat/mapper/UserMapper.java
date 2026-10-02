package com.rindev.chat.mapper;

import com.rindev.chat.dto.response.UserResponse;
import com.rindev.chat.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getUsername(), user.getEmail(),
                user.getPhone(), user.getAvatarUrl(), user.getBio(), user.getStatus(), user.getLastSeenAt(),
                user.getCreatedAt(), user.getUpdatedAt());
    }
}
