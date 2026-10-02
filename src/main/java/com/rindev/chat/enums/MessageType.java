package com.rindev.chat.enums;

import jakarta.persistence.EnumeratedValue;

public enum MessageType {

    TEXT("text"),
    IMAGE("image"),
    VIDEO("video"),
    AUDIO("audio"),
    FILE("file"),
    LOCATION("location"),
    SYSTEM("system");

    @EnumeratedValue
    private final String databaseValue;

    MessageType(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }
}
