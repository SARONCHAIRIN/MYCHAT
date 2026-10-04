package com.rindev.chat.service;

import com.rindev.chat.dto.response.PinnedMessageResponse;
import java.util.List;

public interface PinnedMessageService {

    PinnedMessageResponse pin(
            Long userId,
            Long conversationId,
            Long messageId);

    void unpin(
            Long userId,
            Long conversationId,
            Long messageId);

    List<PinnedMessageResponse> list(
            Long userId,
            Long conversationId);
}