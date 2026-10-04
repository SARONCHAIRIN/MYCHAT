package com.rindev.chat.service;

import com.rindev.chat.dto.response.BlockedUserResponse;
import java.util.List;

public interface BlockService {

    BlockedUserResponse block(
            Long blockerId,
            Long blockedId);

    void unblock(
            Long blockerId,
            Long blockedId);

    List<BlockedUserResponse> getBlockedUsers(
            Long blockerId);
}