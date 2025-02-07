package org.moera.node.util;

public class DurationFormatException extends RuntimeException {

    public DurationFormatException(String value) {
        super("Invalid duration value: " + value);
    }

}
