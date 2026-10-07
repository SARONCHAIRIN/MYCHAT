package com.rindev.chat.repository;

import com.rindev.chat.entity.Conversation;
import com.rindev.chat.enums.ConversationType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository
        extends JpaRepository<Conversation, Long> {

    @Query("""
            SELECT c
            FROM Conversation c
            WHERE c.type = :type
              AND (
                    SELECT COUNT(cm)
                    FROM ConversationMember cm
                    WHERE cm.conversation = c
                      AND cm.leftAt IS NULL
                  ) = 2
              AND EXISTS (
                    SELECT cm1.id
                    FROM ConversationMember cm1
                    WHERE cm1.conversation = c
                      AND cm1.user.id = :firstUserId
                      AND cm1.leftAt IS NULL
                  )
              AND EXISTS (
                    SELECT cm2.id
                    FROM ConversationMember cm2
                    WHERE cm2.conversation = c
                      AND cm2.user.id = :secondUserId
                      AND cm2.leftAt IS NULL
                  )
            """)
    Optional<Conversation> findDirectConversation(
            @Param("type") ConversationType type,
            @Param("firstUserId") Long firstUserId,
            @Param("secondUserId") Long secondUserId);
}