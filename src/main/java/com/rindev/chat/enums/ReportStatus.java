package com.rindev.chat.enums;

import jakarta.persistence.EnumeratedValue;

public enum ReportStatus {

    PENDING("pending"),
    REVIEWED("reviewed"),
    RESOLVED("resolved"),
    REJECTED("rejected");

    @EnumeratedValue
    private final String databaseValue;

    ReportStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }
}
