package com.rindev.chat.storage;

/** Private file bytes returned only after controller authorization. */
public record StoredObject(byte[] bytes, String contentType) {}
