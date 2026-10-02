package com.rindev.chat.service.impl;

import com.rindev.chat.dto.request.LoginRequest;
import com.rindev.chat.dto.request.RefreshTokenRequest;
import com.rindev.chat.dto.request.RegisterRequest;
import com.rindev.chat.dto.response.AuthResponse;
import com.rindev.chat.dto.response.UserResponse;
import com.rindev.chat.entity.User;
import com.rindev.chat.entity.UserSetting;
import com.rindev.chat.exception.ConflictException;
import com.rindev.chat.exception.UnauthorizedException;
import com.rindev.chat.mapper.UserMapper;
import com.rindev.chat.repository.UserRepository;
import com.rindev.chat.repository.UserSettingRepository;
import com.rindev.chat.security.ChatUserDetails;
import com.rindev.chat.security.JwtService;
import com.rindev.chat.service.AuthService;
import com.rindev.chat.service.RefreshTokenService;
import java.time.Instant;
import java.util.Locale;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserSettingRepository userSettingRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;

    public AuthServiceImpl(UserRepository userRepository, UserSettingRepository userSettingRepository,
                           PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                           JwtService jwtService, RefreshTokenService refreshTokenService, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userSettingRepository = userSettingRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByUsername(request.username())
                || (email != null && userRepository.existsByEmail(email))) {
            throw new ConflictException("Username or email is already in use");
        }

        var user = new User();
        user.setName(request.name().strip());
        user.setUsername(request.username());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        // Flush here so uniqueness races fail before creating settings or issuing tokens.
        user = userRepository.saveAndFlush(user);

        var settings = new UserSetting();
        settings.setUser(user);
        userSettingRepository.save(settings);

        var refresh = refreshTokenService.issue(user);
        return tokens(user, refresh.token(), refresh.expiresAt());
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        var credentials = UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password());
        Long userId;
        try {
            var authentication = authenticationManager.authenticate(credentials);
            if (!authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof ChatUserDetails principal)) {
                throw new UnauthorizedException("Invalid username or password");
            }
            userId = principal.getId();
        } catch (AuthenticationException exception) {
            // Never retain authentication-provider messages or causes containing credentials.
            throw new UnauthorizedException("Invalid username or password");
        } finally {
            credentials.eraseCredentials();
        }

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));
        var refresh = refreshTokenService.issue(user);
        return tokens(user, refresh.token(), refresh.expiresAt());
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        var refresh = refreshTokenService.rotate(request.refreshToken());
        return tokens(refresh.user(), refresh.token(), refresh.expiresAt());
    }

    @Override
    @Transactional
    public void logout(Long authenticatedUserId, RefreshTokenRequest request) {
        refreshTokenService.revoke(request.refreshToken(), authenticatedUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse me(Long authenticatedUserId) {
        var user = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
        return userMapper.toResponse(user);
    }

    private AuthResponse tokens(User user, String refreshToken, Instant refreshExpiresAt) {
        return new AuthResponse(jwtService.generateAccessToken(user.getId()), refreshToken, "Bearer",
                jwtService.getAccessTokenTtl().toSeconds(), refreshExpiresAt, userMapper.toResponse(user));
    }

    private static String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
