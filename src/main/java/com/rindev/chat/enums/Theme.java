package com.rindev.chat.enums;

import jakarta.persistence.EnumeratedValue;

public enum Theme {

    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    @EnumeratedValue
    private final String databaseValue;

    Theme(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }
}
