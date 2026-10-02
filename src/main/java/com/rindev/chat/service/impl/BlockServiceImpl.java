package com.rindev.chat.service.impl;

import com.rindev.chat.dto.response.BlockedUserResponse;
import com.rindev.chat.entity.BlockedUser;
import com.rindev.chat.entity.User;
import com.rindev.chat.exception.BadRequestException;
import com.rindev.chat.exception.ConflictException;
import com.rindev.chat.exception.ResourceNotFoundException;
import com.rindev.chat.repository.BlockedUserRepository;
import com.rindev.chat.repository.UserRepository;
import com.rindev.chat.service.BlockService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BlockServiceImpl implements BlockService {

    private final BlockedUserRepository blockedRepository;
    private final UserRepository userRepository;

    public BlockServiceImpl(
            BlockedUserRepository blockedRepository,
            UserRepository userRepository) {

        this.blockedRepository = blockedRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public BlockedUserResponse block(
            Long blockerId,
            Long blockedId) {

        if (blockerId.equals(blockedId)) {
            throw new BadRequestException(
                    "You cannot block yourself");
        }

        User blocker = userRepository.findById(blockerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found"));

        User blocked = userRepository.findById(blockedId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User to block not found"));

        if (blockedRepository
                .existsByBlockerIdAndBlockedId(
                        blockerId,
                        blockedId)) {

            throw new ConflictException(
                    "User is already blocked");
        }

        BlockedUser relation = new BlockedUser();

        relation.setBlocker(blocker);
        relation.setBlocked(blocked);

        return map(
                blockedRepository.save(relation));
    }

    @Override
    @Transactional
    public void unblock(
            Long blockerId,
            Long blockedId) {

        BlockedUser relation = blockedRepository
                .findByBlockerIdAndBlockedId(
                        blockerId,
                        blockedId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Block relationship not found"));

        blockedRepository.delete(relation);
    }

    @Override
    public List<BlockedUserResponse> getBlockedUsers(
            Long blockerId) {

        return blockedRepository
                .findByBlockerIdOrderByCreatedAtDesc(
                        blockerId)
                .stream()
                .map(this::map)
                .toList();
    }

    private BlockedUserResponse map(
            BlockedUser relation) {

        User blocked = relation.getBlocked();

        return new BlockedUserResponse(
                relation.getId(),
                blocked.getId(),
                blocked.getUsername(),
                relation.getCreatedAt());
    }
}