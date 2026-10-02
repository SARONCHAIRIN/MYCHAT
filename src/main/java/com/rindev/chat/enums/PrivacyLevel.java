package com.rindev.chat.enums;

import jakarta.persistence.EnumeratedValue;

public enum PrivacyLevel {

    EVERYONE("everyone"),
    CONTACTS("contacts"),
    NOBODY("nobody");

    @EnumeratedValue
    private final String databaseValue;

    PrivacyLevel(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }
}
