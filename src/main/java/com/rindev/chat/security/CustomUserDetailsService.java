package com.rindev.chat.security;

import com.rindev.chat.entity.User;
import com.rindev.chat.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public ChatUserDetails loadUserByUsername(String username) {
        if (username == null || username.isBlank()) {
            throw userNotFound();
        }
        return toPrincipal(userRepository.findByUsername(username)
                .orElseThrow(CustomUserDetailsService::userNotFound));
    }

    public ChatUserDetails loadUserById(Long userId) {
        if (userId == null || userId <= 0) {
            throw userNotFound();
        }
        return toPrincipal(userRepository.findById(userId)
                .orElseThrow(CustomUserDetailsService::userNotFound));
    }

    private static ChatUserDetails toPrincipal(User user) {
        return new ChatUserDetails(user.getId(), user.getUsername(), user.getPasswordHash());
    }

    private static UsernameNotFoundException userNotFound() {
        return new UsernameNotFoundException("User not found");
    }
}
