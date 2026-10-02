package com.rindev.chat.enums;

import jakarta.persistence.EnumeratedValue;

public enum NotificationType {

    MESSAGE("message"),
    REACTION("reaction"),
    MENTION("mention"),
    GROUP_INVITE("group_invite"),
    SECURITY("security");

    @EnumeratedValue
    private final String databaseValue;

    NotificationType(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }
}
