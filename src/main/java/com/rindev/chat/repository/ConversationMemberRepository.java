package com.rindev.chat.repository;

import com.rindev.chat.entity.ConversationMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationMemberRepository
                extends JpaRepository<ConversationMember, Long> {

        boolean existsByConversationIdAndUserIdAndLeftAtIsNull(
                        Long conversationId,
                        Long userId);

        Optional<ConversationMember> findByConversationIdAndUserIdAndLeftAtIsNull(
                        Long conversationId,
                        Long userId);

        Optional<ConversationMember> findByConversationIdAndUserId(
                        Long conversationId,
                        Long userId);

        List<ConversationMember> findByConversationIdAndLeftAtIsNull(
                        Long conversationId);

        @EntityGraph(attributePaths = { "conversation" })
        List<ConversationMember> findByUserIdAndLeftAtIsNull(
                        Long userId);

        @EntityGraph(attributePaths = { "user" })
        @Query("""
                        SELECT cm
                        FROM ConversationMember cm
                        WHERE cm.conversation.id IN :conversationIds
                          AND cm.leftAt IS NULL
                        ORDER BY cm.conversation.id, cm.id
                        """)
        List<ConversationMember> findActiveMembersByConversationIds(
                        @Param("conversationIds") Collection<Long> conversationIds);

}