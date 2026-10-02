package com.rindev.chat.repository;

import com.rindev.chat.entity.ConversationMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, Long> {
}
