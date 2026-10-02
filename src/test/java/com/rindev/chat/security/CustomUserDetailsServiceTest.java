package com.rindev.chat.security;

import com.rindev.chat.entity.User;
import com.rindev.chat.enums.UserStatus;
import com.rindev.chat.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    private CustomUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new CustomUserDetailsService(userRepository);
    }

    @Test
    void loadsIdentityAndPasswordFromDatabaseByUsername() {
        var user = existingUser();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        var principal = userDetailsService.loadUserByUsername("alice");

        assertThat(principal.getId()).isEqualTo(42L);
        assertThat(principal.getUsername()).isEqualTo("alice");
        assertThat(principal.getPassword()).isEqualTo(user.getPasswordHash());
        assertThat(principal.getAuthorities()).isEmpty();
        verify(userRepository).findByUsername("alice");
    }

    @Test
    void loadsCurrentUsernameByStableUserId() {
        var user = existingUser();
        user.setUsername("renamed-alice");
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));

        var principal = userDetailsService.loadUserById(42L);

        assertThat(principal.getId()).isEqualTo(42L);
        assertThat(principal.getUsername()).isEqualTo("renamed-alice");
        verify(userRepository).findById(42L);
    }

    @ParameterizedTest
    @EnumSource(UserStatus.class)
    void presenceDoesNotDisableAuthentication(UserStatus status) {
        var user = existingUser();
        user.setStatus(status);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));

        var principal = userDetailsService.loadUserById(42L);

        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.isAccountNonExpired()).isTrue();
        assertThat(principal.isAccountNonLocked()).isTrue();
        assertThat(principal.isCredentialsNonExpired()).isTrue();
    }

    @Test
    void unknownUsernameReturnsSafeFailure() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void deletedUserIdReturnsSafeFailure() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserById(42L))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void invalidUsernameIsRejectedWithoutDatabaseAccess(String username) {
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(username))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");
        verifyNoInteractions(userRepository);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    void invalidUserIdIsRejectedWithoutDatabaseAccess(Long id) {
        assertThatThrownBy(() -> userDetailsService.loadUserById(id))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");
        verifyNoInteractions(userRepository);
    }

    @Test
    void credentialsCanBeErasedWithoutChangingPersistentUser() {
        var user = existingUser();
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        var principal = userDetailsService.loadUserById(42L);

        principal.eraseCredentials();

        assertThat(principal.getPassword()).isNull();
        assertThat(user.getPasswordHash()).isEqualTo("test-password-hash");
        assertThat(principal.getId()).isEqualTo(42L);
        assertThat(principal.getUsername()).isEqualTo("alice");
    }

    @Test
    void principalNeverExposesHashInToStringOrJson() {
        var principal = new ChatUserDetails(42L, "alice", "test-password-hash");
        var json = JsonMapper.builder().build().writeValueAsString(principal);

        assertThat(principal.toString()).doesNotContain("test-password-hash", "password");
        assertThat(json).doesNotContain("test-password-hash", "password", "passwordHash");
    }

    @Test
    void globalAuthoritiesCannotBeAddedToPrincipal() {
        var principal = new ChatUserDetails(42L, "alice", "test-password-hash");

        assertThat(principal.getAuthorities()).isEmpty();
        assertThatThrownBy(() -> principal.getAuthorities().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static User existingUser() {
        var user = new User();
        user.setId(42L);
        user.setUsername("alice");
        user.setPasswordHash("test-password-hash");
        user.setStatus(UserStatus.OFFLINE);
        return user;
    }
}
