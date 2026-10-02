package com.rindev.chat.enums;

import jakarta.persistence.EnumeratedValue;

public enum ConversationType {

    DIRECT("direct"),
    GROUP("group");

    @EnumeratedValue
    private final String databaseValue;

    ConversationType(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }
}
