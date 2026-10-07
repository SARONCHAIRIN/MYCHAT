package com.rindev.chat.service;

import com.rindev.chat.dto.response.ReactionResponse;
import java.util.List;

public interface ReactionService {

    ReactionResponse addReaction(
            Long userId,
            Long messageId,
            String emoji);

    List<ReactionResponse> getReactions(
            Long userId,
            Long messageId);

    void removeReaction(
            Long userId,
            Long messageId,
            String emoji);
}