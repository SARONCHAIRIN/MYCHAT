package com.rindev.chat.enums;

import jakarta.persistence.EnumeratedValue;

public enum MemberRole {

    OWNER("owner"),
    ADMIN("admin"),
    MEMBER("member");

    @EnumeratedValue
    private final String databaseValue;

    MemberRole(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }
}
