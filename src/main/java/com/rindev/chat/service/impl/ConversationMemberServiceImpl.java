package com.rindev.chat.service.impl;

import com.rindev.chat.dto.response.ConversationMemberResponse;
import com.rindev.chat.entity.Conversation;
import com.rindev.chat.entity.ConversationMember;
import com.rindev.chat.entity.User;
import com.rindev.chat.enums.ConversationType;
import com.rindev.chat.enums.MemberRole;
import com.rindev.chat.exception.BadRequestException;
import com.rindev.chat.exception.ConflictException;
import com.rindev.chat.exception.ForbiddenException;
import com.rindev.chat.exception.ResourceNotFoundException;
import com.rindev.chat.mapper.ConversationMapper;
import com.rindev.chat.repository.ConversationMemberRepository;
import com.rindev.chat.repository.ConversationRepository;
import com.rindev.chat.repository.UserRepository;
import com.rindev.chat.service.ConversationMemberService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ConversationMemberServiceImpl
                implements ConversationMemberService {

        private final ConversationRepository conversationRepository;
        private final ConversationMemberRepository memberRepository;
        private final UserRepository userRepository;
        private final ConversationMapper conversationMapper;

        public ConversationMemberServiceImpl(
                        ConversationRepository conversationRepository,
                        ConversationMemberRepository memberRepository,
                        UserRepository userRepository,
                        ConversationMapper conversationMapper) {
                this.conversationRepository = conversationRepository;
                this.memberRepository = memberRepository;
                this.userRepository = userRepository;
                this.conversationMapper = conversationMapper;
        }

        @Override
        public List<ConversationMemberResponse> getMembers(
                        Long authenticatedUserId,
                        Long conversationId) {

                findConversation(conversationId);
                requireMembership(conversationId, authenticatedUserId);

                return memberRepository
                                .findByConversationIdAndLeftAtIsNull(conversationId)
                                .stream()
                                .map(conversationMapper::toMemberResponse)
                                .toList();
        }

        @Override
        @Transactional
        public ConversationMemberResponse addMember(
                        Long authenticatedUserId,
                        Long conversationId,
                        Long userId) {

                Conversation conversation = requireGroup(conversationId);

                ConversationMember actor = requireMembership(conversationId, authenticatedUserId);

                requireOwnerOrAdmin(actor);

                if (memberRepository
                                .existsByConversationIdAndUserIdAndLeftAtIsNull(
                                                conversationId,
                                                userId)) {
                        throw new ConflictException(
                                        "User is already a member of this conversation");
                }

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "User not found with id: " + userId));

                ConversationMember member = memberRepository
                                .findByConversationIdAndUserId(conversationId, userId)
                                .orElseGet(ConversationMember::new);

                member.setConversation(conversation);
                member.setUser(user);
                member.setRole(MemberRole.MEMBER);
                member.setLeftAt(null);
                member.setMutedUntil(null);

                ConversationMember saved = memberRepository.save(member);

                return conversationMapper.toMemberResponse(saved);
        }

        @Override
        @Transactional
        public void removeMember(
                        Long authenticatedUserId,
                        Long conversationId,
                        Long userId) {

                requireGroup(conversationId);

                ConversationMember actor = requireMembership(conversationId, authenticatedUserId);

                ConversationMember target = requireMembership(conversationId, userId);

                boolean removingSelf = authenticatedUserId.equals(userId);

                // OWNER cannot leave/be removed because Phase 9 has
                // no ownership-transfer operation.
                if (target.getRole() == MemberRole.OWNER) {
                        throw new BadRequestException(
                                        "Conversation owner cannot be removed");
                }

                if (!removingSelf) {
                        if (actor.getRole() == MemberRole.MEMBER) {
                                throw new ForbiddenException(
                                                "Members cannot remove other members");
                        }

                        // ADMIN cannot remove another ADMIN.
                        if (actor.getRole() == MemberRole.ADMIN
                                        && target.getRole() == MemberRole.ADMIN) {
                                throw new ForbiddenException(
                                                "Admins cannot remove other admins");
                        }
                }

                target.setLeftAt(LocalDateTime.now());

                memberRepository.save(target);
        }

        @Override
        @Transactional
        public ConversationMemberResponse updateRole(
                        Long authenticatedUserId,
                        Long conversationId,
                        Long userId,
                        MemberRole newRole) {

                requireGroup(conversationId);

                ConversationMember actor = requireMembership(conversationId, authenticatedUserId);

                if (actor.getRole() != MemberRole.OWNER) {
                        throw new ForbiddenException(
                                        "Only the conversation owner can change member roles");
                }

                ConversationMember target = requireMembership(conversationId, userId);

                if (target.getRole() == MemberRole.OWNER) {
                        throw new BadRequestException(
                                        "Owner role cannot be changed");
                }

                if (newRole == MemberRole.OWNER) {
                        throw new BadRequestException(
                                        "Ownership transfer is not supported");
                }

                if (newRole != MemberRole.ADMIN
                                && newRole != MemberRole.MEMBER) {
                        throw new BadRequestException(
                                        "Role must be ADMIN or MEMBER");
                }

                target.setRole(newRole);

                ConversationMember saved = memberRepository.save(target);

                return conversationMapper.toMemberResponse(saved);
        }

        private Conversation requireGroup(Long conversationId) {
                Conversation conversation = findConversation(conversationId);

                if (conversation.getType() != ConversationType.GROUP) {
                        throw new BadRequestException(
                                        "Member management is only available for group conversations");
                }

                return conversation;
        }

        private Conversation findConversation(Long conversationId) {
                return conversationRepository.findById(conversationId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Conversation not found with id: "
                                                                + conversationId));
        }

        private ConversationMember requireMembership(
                        Long conversationId,
                        Long userId) {

                return memberRepository
                                .findByConversationIdAndUserIdAndLeftAtIsNull(
                                                conversationId,
                                                userId)
                                .orElseThrow(() -> new ForbiddenException(
                                                "You are not a member of this conversation"));
        }

        private void requireOwnerOrAdmin(
                        ConversationMember member) {

                if (member.getRole() != MemberRole.OWNER
                                && member.getRole() != MemberRole.ADMIN) {
                        throw new ForbiddenException(
                                        "Only owners or admins can add members");
                }
        }
}