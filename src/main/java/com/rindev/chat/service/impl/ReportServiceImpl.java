package com.rindev.chat.service.impl;

import com.rindev.chat.dto.request.CreateReportRequest;
import com.rindev.chat.dto.response.ReportResponse;
import com.rindev.chat.entity.Conversation;
import com.rindev.chat.entity.Message;
import com.rindev.chat.entity.Report;
import com.rindev.chat.entity.User;
import com.rindev.chat.enums.ReportStatus;
import com.rindev.chat.exception.BadRequestException;
import com.rindev.chat.exception.ForbiddenException;
import com.rindev.chat.exception.ResourceNotFoundException;
import com.rindev.chat.repository.ConversationMemberRepository;
import com.rindev.chat.repository.ConversationRepository;
import com.rindev.chat.repository.MessageRepository;
import com.rindev.chat.repository.ReportRepository;
import com.rindev.chat.repository.UserRepository;
import com.rindev.chat.service.ReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final MessageRepository messageRepository;

    public ReportServiceImpl(
            ReportRepository reportRepository,
            UserRepository userRepository,
            ConversationRepository conversationRepository,
            ConversationMemberRepository memberRepository,
            MessageRepository messageRepository) {

        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.memberRepository = memberRepository;
        this.messageRepository = messageRepository;
    }

    @Override
    @Transactional
    public ReportResponse createReport(
            Long reporterId,
            CreateReportRequest request) {

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Reporter not found"));

        validateTargetExists(request);

        User reportedUser = null;
        Conversation conversation = null;
        Message message = null;

        if (request.reportedUserId() != null) {

            if (request.reportedUserId().equals(reporterId)) {
                throw new BadRequestException(
                        "You cannot report yourself");
            }

            reportedUser = userRepository
                    .findById(request.reportedUserId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Reported user not found"));
        }

        if (request.conversationId() != null) {

            conversation = conversationRepository
                    .findById(request.conversationId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Conversation not found"));

            requireConversationAccess(
                    reporterId,
                    conversation.getId());
        }

        if (request.messageId() != null) {

            message = messageRepository
                    .findById(request.messageId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Message not found"));

            Long messageConversationId = message.getConversation().getId();

            requireConversationAccess(
                    reporterId,
                    messageConversationId);

            if (conversation != null
                    && !conversation.getId()
                            .equals(messageConversationId)) {

                throw new BadRequestException(
                        "Message does not belong to the reported conversation");
            }

            if (reportedUser != null
                    && !message.getSender().getId()
                            .equals(reportedUser.getId())) {

                throw new BadRequestException(
                        "Reported user is not the sender of the reported message");
            }
        }

        Report report = new Report();

        report.setReporter(reporter);
        report.setReportedUser(reportedUser);
        report.setConversation(conversation);
        report.setMessage(message);
        report.setReason(request.reason());

        if (request.description() != null) {
            String description = request.description().trim();

            report.setDescription(
                    description.isEmpty()
                            ? null
                            : description);
        }

        // Never trust moderation status from client.
        report.setStatus(ReportStatus.PENDING);

        Report saved = reportRepository.save(report);

        return map(saved);
    }

    private void validateTargetExists(
            CreateReportRequest request) {

        if (request.reportedUserId() == null
                && request.conversationId() == null
                && request.messageId() == null) {

            throw new BadRequestException(
                    "At least one report target is required");
        }
    }

    private void requireConversationAccess(
            Long userId,
            Long conversationId) {

        boolean member = memberRepository
                .existsByConversationIdAndUserIdAndLeftAtIsNull(
                        conversationId,
                        userId);

        if (!member) {
            throw new ForbiddenException(
                    "You cannot report content from this conversation");
        }
    }

    private ReportResponse map(Report report) {

        return new ReportResponse(
                report.getId(),
                report.getReporter().getId(),

                report.getReportedUser() == null
                        ? null
                        : report.getReportedUser().getId(),

                report.getConversation() == null
                        ? null
                        : report.getConversation().getId(),

                report.getMessage() == null
                        ? null
                        : report.getMessage().getId(),

                report.getReason(),
                report.getDescription(),
                report.getStatus(),
                report.getCreatedAt());
    }
}