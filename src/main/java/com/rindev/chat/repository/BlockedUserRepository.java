package com.rindev.chat.repository;

import com.rindev.chat.entity.BlockedUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockedUserRepository extends JpaRepository<BlockedUser, Long> {

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    Optional<BlockedUser> findByBlockerIdAndBlockedId(

            Long blockerId,

            Long blockedId

    );

    List<BlockedUser>

            findByBlockerIdOrderByCreatedAtDesc(

                    Long blockerId

    );
}
