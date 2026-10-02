package com.rindev.chat.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Authenticated identity copied from persistence; never exposes the User entity. */
public final class ChatUserDetails implements UserDetails, CredentialsContainer {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String username;
    private transient String passwordHash;

    public ChatUserDetails(Long id, String username, String passwordHash) {
        this.id = Objects.requireNonNull(id, "User ID is required");
        this.username = Objects.requireNonNull(username, "Username is required");
        this.passwordHash = Objects.requireNonNull(passwordHash, "Password hash is required");
    }

    public Long getId() {
        return id;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // The schema has no global roles. Conversation roles are resource-specific.
        return List.of();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // UserStatus tracks presence, not account eligibility.
        return true;
    }

    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }

    @Override
    public String toString() {
        return "ChatUserDetails[id=" + id + "]";
    }
}
