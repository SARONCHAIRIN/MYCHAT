package com.rindev.chat.repository;

import com.rindev.chat.entity.MessageReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageReceiptRepository extends JpaRepository<MessageReceipt, Long> {
}
