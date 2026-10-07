package com.rindev.chat.service.impl;

import com.rindev.chat.dto.request.CreateConversationRequest;
import com.rindev.chat.dto.request.UpdateConversationRequest;
import com.rindev.chat.dto.response.ConversationResponse;
import com.rindev.chat.entity.Conversation;
import com.rindev.chat.entity.ConversationMember;
import com.rindev.chat.entity.User;
import com.rindev.chat.enums.ConversationType;
import com.rindev.chat.enums.MemberRole;
import com.rindev.chat.exception.BadRequestException;
import com.rindev.chat.exception.ForbiddenException;
import com.rindev.chat.exception.ResourceNotFoundException;
import com.rindev.chat.mapper.ConversationMapper;
import com.rindev.chat.repository.ConversationMemberRepository;
import com.rindev.chat.repository.ConversationRepository;
import com.rindev.chat.repository.UserRepository;
import com.rindev.chat.service.ConversationService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ConversationServiceImpl implements ConversationService {

        private final ConversationRepository conversationRepository;
        private final ConversationMemberRepository memberRepository;
        private final UserRepository userRepository;
        private final ConversationMapper conversationMapper;

        public ConversationServiceImpl(
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
        @Transactional
        public ConversationResponse createConversation(
                        Long authenticatedUserId,
                        CreateConversationRequest request) {
                User creator = findUser(authenticatedUserId);

                if (request.type() == ConversationType.DIRECT) {
                        return createDirectConversation(
                                        creator,
                                        request.memberId());
                }

                if (request.type() == ConversationType.GROUP) {
                        return createGroupConversation(
                                        creator,
                                        request);
                }

                throw new BadRequestException(
                                "Unsupported conversation type");
        }

        private ConversationResponse createDirectConversation(
                        User creator,
                        Long memberId) {
                if (memberId == null) {
                        throw new BadRequestException(
                                        "memberId is required for direct conversation");
                }

                if (creator.getId().equals(memberId)) {
                        throw new BadRequestException(
                                        "You cannot create a direct conversation with yourself");
                }

                User otherUser = findUser(memberId);

                return conversationRepository
                                .findDirectConversation(
                                                ConversationType.DIRECT,
                                                creator.getId(),
                                                otherUser.getId())
                                .map(this::toResponse)
                                .orElseGet(() -> saveDirectConversation(
                                                creator,
                                                otherUser));
        }

        private ConversationResponse saveDirectConversation(
                        User creator,
                        User otherUser) {
                Conversation conversation = new Conversation();
                conversation.setType(ConversationType.DIRECT);
                conversation.setCreatedBy(creator);

                Conversation saved = conversationRepository.save(conversation);

                ConversationMember creatorMember = createMember(
                                saved,
                                creator,
                                MemberRole.MEMBER);

                ConversationMember otherMember = createMember(
                                saved,
                                otherUser,
                                MemberRole.MEMBER);

                memberRepository.saveAll(
                                List.of(
                                                creatorMember,
                                                otherMember));

                return toResponse(saved);
        }

        private ConversationResponse createGroupConversation(
                        User creator,
                        CreateConversationRequest request) {
                if (request.name() == null
                                || request.name().isBlank()) {
                        throw new BadRequestException(
                                        "Group name is required");
                }

                if (request.memberIds() == null
                                || request.memberIds().isEmpty()) {
                        throw new BadRequestException(
                                        "At least one group member is required");
                }

                Conversation conversation = new Conversation();
                conversation.setType(ConversationType.GROUP);
                conversation.setName(request.name().trim());
                conversation.setDescription(
                                normalizeNullable(request.description()));
                conversation.setAvatarUrl(
                                normalizeNullable(request.avatarUrl()));
                conversation.setCreatedBy(creator);

                Conversation saved = conversationRepository.save(conversation);

                memberRepository.save(
                                createMember(
                                                saved,
                                                creator,
                                                MemberRole.OWNER));

                Set<Long> uniqueMemberIds = new LinkedHashSet<>(request.memberIds());

                uniqueMemberIds.remove(creator.getId());

                for (Long memberId : uniqueMemberIds) {
                        if (memberId == null) {
                                throw new BadRequestException(
                                                "Member ID must not be null");
                        }

                        User user = findUser(memberId);

                        memberRepository.save(
                                        createMember(
                                                        saved,
                                                        user,
                                                        MemberRole.MEMBER));
                }

                return toResponse(saved);
        }

        @Override
        @Transactional(readOnly = true)
        public List<ConversationResponse> getConversations(
                        Long authenticatedUserId) {

                List<ConversationMember> memberships = memberRepository.findByUserIdAndLeftAtIsNull(
                                authenticatedUserId);

                if (memberships.isEmpty()) {
                        return List.of();
                }

                List<Conversation> conversations = memberships.stream()
                                .map(ConversationMember::getConversation)
                                .toList();

                List<Long> conversationIds = conversations.stream()
                                .map(Conversation::getId)
                                .toList();

                Map<Long, List<ConversationMember>> membersByConversation = memberRepository
                                .findActiveMembersByConversationIds(
                                                conversationIds)
                                .stream()
                                .collect(Collectors.groupingBy(
                                                member -> member.getConversation().getId()));

                return conversations.stream()
                                .map(conversation -> conversationMapper.toResponse(
                                                conversation,
                                                membersByConversation.getOrDefault(
                                                                conversation.getId(),
                                                                List.of())))
                                .toList();
        }

        @Override
        public ConversationResponse getConversation(
                        Long authenticatedUserId,
                        Long conversationId) {
                Conversation conversation = findConversation(conversationId);

                requireMembership(
                                conversationId,
                                authenticatedUserId);

                return toResponse(conversation);
        }

        @Override
        @Transactional
        public ConversationResponse updateConversation(
                        Long authenticatedUserId,
                        Long conversationId,
                        UpdateConversationRequest request) {
                Conversation conversation = findConversation(conversationId);

                ConversationMember membership = requireMembership(
                                conversationId,
                                authenticatedUserId);

                if (conversation.getType() == ConversationType.DIRECT) {
                        throw new BadRequestException(
                                        "Direct conversations cannot be updated");
                }

                if (membership.getRole() != MemberRole.OWNER
                                && membership.getRole() != MemberRole.ADMIN) {
                        throw new ForbiddenException(
                                        "Only group owners or admins can update the conversation");
                }

                if (request.name() != null) {
                        String name = request.name().trim();

                        if (name.isEmpty()) {
                                throw new BadRequestException(
                                                "Group name must not be blank");
                        }

                        conversation.setName(name);
                }

                if (request.description() != null) {
                        conversation.setDescription(
                                        normalizeNullable(
                                                        request.description()));
                }

                if (request.avatarUrl() != null) {
                        conversation.setAvatarUrl(
                                        normalizeNullable(
                                                        request.avatarUrl()));
                }

                Conversation saved = conversationRepository.save(conversation);

                return toResponse(saved);
        }

        private ConversationMember createMember(
                        Conversation conversation,
                        User user,
                        MemberRole role) {
                ConversationMember member = new ConversationMember();

                member.setConversation(conversation);
                member.setUser(user);
                member.setRole(role);

                return member;
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

        private Conversation findConversation(Long id) {
                return conversationRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Conversation not found with id: " + id));
        }

        private User findUser(Long id) {
                return userRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "User not found with id: " + id));
        }

        private ConversationResponse toResponse(
                        Conversation conversation) {
                List<ConversationMember> members = memberRepository
                                .findByConversationIdAndLeftAtIsNull(
                                                conversation.getId());

                return conversationMapper.toResponse(
                                conversation,
                                members);
        }

        private String normalizeNullable(String value) {
                if (value == null) {
                        return null;
                }

                String trimmed = value.trim();

                return trimmed.isEmpty()
                                ? null
                                : trimmed;
        }
}