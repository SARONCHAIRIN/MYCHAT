package com.rindev.chat.service;

import com.rindev.chat.dto.request.LoginRequest;
import com.rindev.chat.dto.request.RefreshTokenRequest;
import com.rindev.chat.dto.request.RegisterRequest;
import com.rindev.chat.entity.User;
import com.rindev.chat.entity.UserSetting;
import com.rindev.chat.enums.PrivacyLevel;
import com.rindev.chat.enums.Theme;
import com.rindev.chat.enums.UserStatus;
import com.rindev.chat.exception.ConflictException;
import com.rindev.chat.exception.UnauthorizedException;
import com.rindev.chat.mapper.UserMapper;
import com.rindev.chat.repository.UserRepository;
import com.rindev.chat.repository.UserSettingRepository;
import com.rindev.chat.security.ChatUserDetails;
import com.rindev.chat.security.JwtService;
import com.rindev.chat.service.impl.AuthServiceImpl;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Instant REFRESH_EXPIRY = Instant.parse("2026-11-01T00:00:00Z");

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserSettingRepository userSettingRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(userRepository, userSettingRepository, passwordEncoder,
                authenticationManager, jwtService, refreshTokenService, new UserMapper());
    }

    @Test
    void registrationHashesPasswordCreatesDefaultSettingsAndReturnsTokens() {
        var request = new RegisterRequest(" Alice Example ", "alice", "ALICE@example.test", "secret-password");
        when(passwordEncoder.encode("secret-password")).thenReturn("bcrypt-hash");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });
        when(refreshTokenService.issue(any(User.class)))
                .thenReturn(new RefreshTokenService.IssuedRefreshToken("opaque-refresh-token", REFRESH_EXPIRY));
        stubAccessToken();

        var response = service.register(request);

        var userCapture = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCapture.capture());
        var saved = userCapture.getValue();
        assertThat(saved.getName()).isEqualTo("Alice Example");
        assertThat(saved.getUsername()).isEqualTo("alice");
        assertThat(saved.getEmail()).isEqualTo("alice@example.test");
        assertThat(saved.getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(saved.getStatus()).isEqualTo(UserStatus.OFFLINE);
        var settingsCapture = ArgumentCaptor.forClass(UserSetting.class);
        verify(userSettingRepository).save(settingsCapture.capture());
        var settings = settingsCapture.getValue();
        assertThat(settings.getUser()).isSameAs(saved);
        assertThat(settings.getTheme()).isEqualTo(Theme.SYSTEM);
        assertThat(settings.getMessageNotifications()).isTrue();
        assertThat(settings.getGroupNotifications()).isTrue();
        assertThat(settings.getReactionNotifications()).isTrue();
        assertThat(settings.getReadReceipts()).isTrue();
        assertThat(settings.getLastSeenPrivacy()).isEqualTo(PrivacyLevel.EVERYONE);
        assertThat(settings.getProfilePhotoPrivacy()).isEqualTo(PrivacyLevel.EVERYONE);
        assertThat(settings.getGroupAddPrivacy()).isEqualTo(PrivacyLevel.EVERYONE);
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("opaque-refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900);
        assertThat(response.refreshExpiresAt()).isEqualTo(REFRESH_EXPIRY);
        assertThat(response.user().id()).isEqualTo(42L);
    }

    @Test
    void optionalEmailIsStoredAsNull() {
        when(passwordEncoder.encode(any())).thenReturn("bcrypt-hash");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertThat(user.getEmail()).isNull();
            user.setId(42L);
            return user;
        });
        when(refreshTokenService.issue(any(User.class)))
                .thenReturn(new RefreshTokenService.IssuedRefreshToken("refresh-token", REFRESH_EXPIRY));
        stubAccessToken();

        service.register(new RegisterRequest("Alice", "alice", "", "secret-password"));
    }

    @Test
    void duplicateUsernameIsRejectedBeforeHashingOrWriting() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> service.register(registration()))
                .isInstanceOf(ConflictException.class).hasMessage("Username or email is already in use");

        verifyNoInteractions(passwordEncoder, userSettingRepository, refreshTokenService, jwtService);
    }

    @Test
    void duplicateEmailIsRejectedBeforeHashingOrWriting() {
        when(userRepository.existsByEmail("alice@example.test")).thenReturn(true);

        assertThatThrownBy(() -> service.register(registration()))
                .isInstanceOf(ConflictException.class).hasMessage("Username or email is already in use");

        verifyNoInteractions(passwordEncoder, userSettingRepository, refreshTokenService, jwtService);
    }

    @Test
    void databaseUniquenessRaceStopsBeforeSettingsAndTokens() {
        when(passwordEncoder.encode(any())).thenReturn("bcrypt-hash");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("database uniqueness conflict"));

        assertThatThrownBy(() -> service.register(registration()))
                .isInstanceOf(DataIntegrityViolationException.class);

        verifyNoInteractions(userSettingRepository, refreshTokenService, jwtService);
    }

    @Test
    void loginUsesAuthenticatedIdentityAndErasesSubmittedCredentials() {
        var user = existingUser();
        when(authenticationManager.authenticate(any())).thenAnswer(invocation -> {
            UsernamePasswordAuthenticationToken submitted = invocation.getArgument(0);
            assertThat(submitted.getName()).isEqualTo("alice");
            assertThat(submitted.getCredentials()).isEqualTo("secret-password");
            return UsernamePasswordAuthenticationToken.authenticated(
                    new ChatUserDetails(42L, "alice", "bcrypt-hash"), null, List.of());
        });
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(refreshTokenService.issue(user))
                .thenReturn(new RefreshTokenService.IssuedRefreshToken("login-refresh-token", REFRESH_EXPIRY));
        stubAccessToken();

        var response = service.login(new LoginRequest("alice", "secret-password"));

        var credentialsCapture = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(credentialsCapture.capture());
        assertThat(credentialsCapture.getValue().getCredentials()).isNull();
        assertThat(response.user().id()).isEqualTo(42L);
        assertThat(response.refreshToken()).isEqualTo("login-refresh-token");
    }

    @Test
    void invalidLoginReturnsSafeFailureWithoutIssuingTokens() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("sensitive provider message"));

        assertThatThrownBy(() -> service.login(new LoginRequest("alice", "wrong-password")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid username or password").hasNoCause();

        var credentialsCapture = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(credentialsCapture.capture());
        assertThat(credentialsCapture.getValue().getCredentials()).isNull();
        verifyNoInteractions(userRepository, refreshTokenService, jwtService);
    }

    @Test
    void deletedUserAfterAuthenticationCannotReceiveTokens() {
        when(authenticationManager.authenticate(any())).thenReturn(UsernamePasswordAuthenticationToken.authenticated(
                new ChatUserDetails(42L, "alice", "bcrypt-hash"), null, List.of()));
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("alice", "secret-password")))
                .isInstanceOf(UnauthorizedException.class);

        verifyNoInteractions(refreshTokenService, jwtService);
    }

    @Test
    void refreshIssuesAccessTokenForTheRotatedTokenOwner() {
        when(refreshTokenService.rotate("previous-token"))
                .thenReturn(new RefreshTokenService.RotatedRefreshToken(existingUser(), "replacement-token", REFRESH_EXPIRY));
        stubAccessToken();

        var response = service.refresh(new RefreshTokenRequest("previous-token"));

        assertThat(response.refreshToken()).isEqualTo("replacement-token");
        assertThat(response.user().id()).isEqualTo(42L);
        verifyNoInteractions(authenticationManager, userRepository);
    }

    @Test
    void revokedRefreshNeverIssuesAccessToken() {
        when(refreshTokenService.rotate("revoked-token"))
                .thenThrow(new UnauthorizedException("Invalid refresh token"));

        assertThatThrownBy(() -> service.refresh(new RefreshTokenRequest("revoked-token")))
                .isInstanceOf(UnauthorizedException.class);

        verifyNoInteractions(jwtService);
    }

    @Test
    void logoutUsesPrincipalIdentityForRevocation() {
        service.logout(42L, new RefreshTokenRequest("refresh-token"));

        verify(refreshTokenService).revoke("refresh-token", 42L);
        verifyNoInteractions(authenticationManager, jwtService);
    }

    @Test
    void currentUserComesFromPrincipalIdentity() {
        when(userRepository.findById(42L)).thenReturn(Optional.of(existingUser()));

        var user = service.me(42L);

        assertThat(user.id()).isEqualTo(42L);
        assertThat(user.username()).isEqualTo("alice");
    }

    @Test
    void deletedCurrentUserReturnsUnauthorized() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.me(42L)).isInstanceOf(UnauthorizedException.class);
    }

    private void stubAccessToken() {
        when(jwtService.generateAccessToken(42L)).thenReturn("access-token");
        when(jwtService.getAccessTokenTtl()).thenReturn(Duration.ofMinutes(15));
    }

    private static RegisterRequest registration() {
        return new RegisterRequest("Alice", "alice", "alice@example.test", "secret-password");
    }

    private static User existingUser() {
        var user = new User();
        user.setId(42L);
        user.setName("Alice");
        user.setUsername("alice");
        user.setEmail("alice@example.test");
        user.setPasswordHash("bcrypt-hash");
        return user;
    }
}
