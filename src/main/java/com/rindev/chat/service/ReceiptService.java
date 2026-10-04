package com.rindev.chat.service;

import com.rindev.chat.dto.response.ReceiptResponse;

public interface ReceiptService {

    ReceiptResponse markDelivered(
            Long userId,
            Long messageId);

    ReceiptResponse markRead(
            Long userId,
            Long messageId);
}