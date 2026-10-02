package com.rindev.chat.enums;

import jakarta.persistence.EnumeratedValue;

public enum DevicePlatform {

    ANDROID("android"),
    IOS("ios"),
    WEB("web");

    @EnumeratedValue
    private final String databaseValue;

    DevicePlatform(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }
}
