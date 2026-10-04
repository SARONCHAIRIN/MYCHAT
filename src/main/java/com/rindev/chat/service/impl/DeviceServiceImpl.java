package com.rindev.chat.service.impl;

import com.rindev.chat.dto.request.RegisterDeviceRequest;
import com.rindev.chat.dto.response.DeviceResponse;
import com.rindev.chat.entity.Device;
import com.rindev.chat.entity.User;
import com.rindev.chat.exception.ResourceNotFoundException;
import com.rindev.chat.repository.DeviceRepository;
import com.rindev.chat.repository.UserRepository;
import com.rindev.chat.service.DeviceService;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;

    public DeviceServiceImpl(
            DeviceRepository deviceRepository,
            UserRepository userRepository) {

        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public DeviceResponse register(
            Long userId,
            RegisterDeviceRequest request,
            String ip) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found"));

        Device device = deviceRepository
                .findByFcmToken(request.fcmToken())
                .orElseGet(Device::new);

        device.setUser(user);
        device.setDeviceName(request.deviceName());
        device.setPlatform(request.platform());
        device.setFcmToken(request.fcmToken());
        device.setLastIp(ip);
        device.setLastActiveAt(LocalDateTime.now());

        return map(deviceRepository.save(device));
    }

    @Override
    @Transactional
    public void delete(
            Long userId,
            Long deviceId) {

        Device device = deviceRepository
                .findByIdAndUserId(
                        deviceId,
                        userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Device not found"));

        deviceRepository.delete(device);
    }

    private DeviceResponse map(Device device) {

        return new DeviceResponse(
                device.getId(),
                device.getDeviceName(),
                device.getPlatform(),
                device.getLastActiveAt(),
                device.getCreatedAt());
    }
}