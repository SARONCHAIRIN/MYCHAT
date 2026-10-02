package com.rindev.chat.enums;

import jakarta.persistence.EnumeratedValue;

public enum ReportReason {

    SPAM("spam"),
    HARASSMENT("harassment"),
    FAKE_ACCOUNT("fake_account"),
    INAPPROPRIATE("inappropriate"),
    OTHER("other");

    @EnumeratedValue
    private final String databaseValue;

    ReportReason(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }
}
