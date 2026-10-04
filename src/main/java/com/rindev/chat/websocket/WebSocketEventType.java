package com.rindev.chat.websocket;

public enum WebSocketEventType {

    MESSAGE_NEW("message:new"),
    MESSAGE_EDITED("message:edited"),
    MESSAGE_DELETED("message:deleted"),

    MESSAGE_DELIVERED("message:delivered"),
    MESSAGE_READ("message:read"),

    REACTION_ADDED("reaction:added"),
    REACTION_REMOVED("reaction:removed"),

    TYPING_START("typing:start"),
    TYPING_STOP("typing:stop"),

    USER_ONLINE("user:online"),
    USER_OFFLINE("user:offline");

    private final String value;

    WebSocketEventType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}