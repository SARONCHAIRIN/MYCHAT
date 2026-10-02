package com.rindev.chat.enums;

import jakarta.persistence.EnumeratedValue;

public enum AttachmentType {

    IMAGE("image"),
    VIDEO("video"),
    AUDIO("audio"),
    FILE("file");

    @EnumeratedValue
    private final String databaseValue;

    AttachmentType(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }
}
