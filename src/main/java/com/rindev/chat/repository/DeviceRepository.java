package com.rindev.chat.repository;

import com.rindev.chat.entity.Device;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository
        extends JpaRepository<Device, Long> {

    Optional<Device> findByIdAndUserId(
            Long id,
            Long userId);

    Optional<Device> findByFcmToken(
            String fcmToken);
}