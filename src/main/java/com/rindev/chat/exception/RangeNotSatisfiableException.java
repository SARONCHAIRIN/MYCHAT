package com.rindev.chat.exception;

public class RangeNotSatisfiableException extends RuntimeException {

    private final long totalLength;

    public RangeNotSatisfiableException(long totalLength) {
        super("Requested range not satisfiable");
        this.totalLength = totalLength;
    }

    public long getTotalLength() {
        return totalLength;
    }
}
