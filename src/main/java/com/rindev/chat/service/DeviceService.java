package com.rindev.chat.service;

import com.rindev.chat.dto.request.RegisterDeviceRequest;
import com.rindev.chat.dto.response.DeviceResponse;

public interface DeviceService {

    DeviceResponse register(
            Long userId,
            RegisterDeviceRequest request,
            String ip);

    void delete(
            Long userId,
            Long deviceId);
}