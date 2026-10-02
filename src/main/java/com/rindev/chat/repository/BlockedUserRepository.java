package com.rindev.chat.repository;

import com.rindev.chat.entity.BlockedUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockedUserRepository extends JpaRepository<BlockedUser, Long> {

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
}
