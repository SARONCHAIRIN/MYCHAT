package com.rindev.chat.enums;

import jakarta.persistence.EnumeratedValue;

public enum UserStatus {

    ONLINE("online"),
    OFFLINE("offline");

    @EnumeratedValue
    private final String databaseValue;

    UserStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }
}
