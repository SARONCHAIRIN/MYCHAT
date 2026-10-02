package com.rindev.chat.service;

import com.rindev.chat.dto.request.LoginRequest;
import com.rindev.chat.dto.request.RefreshTokenRequest;
import com.rindev.chat.dto.request.RegisterRequest;
import com.rindev.chat.dto.response.AuthResponse;
import com.rindev.chat.dto.response.UserResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    void logout(Long authenticatedUserId, RefreshTokenRequest request);

    UserResponse me(Long authenticatedUserId);
}
