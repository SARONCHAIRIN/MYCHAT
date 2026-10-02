package com.rindev.chat.repository;

import com.rindev.chat.entity.ConversationMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationMemberRepository
        extends JpaRepository<ConversationMember, Long> {

    boolean existsByConversationIdAndUserIdAndLeftAtIsNull(
            Long conversationId,
            Long userId);

    Optional<ConversationMember> findByConversationIdAndUserIdAndLeftAtIsNull(
            Long conversationId,
            Long userId);

    List<ConversationMember> findByConversationIdAndLeftAtIsNull(
            Long conversationId);

    List<ConversationMember> findByUserIdAndLeftAtIsNull(
            Long userId);
}