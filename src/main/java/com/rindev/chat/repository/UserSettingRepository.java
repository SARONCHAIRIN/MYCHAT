package com.rindev.chat.repository;

import com.rindev.chat.entity.UserSetting;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {

    Optional<UserSetting> findByUserId(Long userId);
}