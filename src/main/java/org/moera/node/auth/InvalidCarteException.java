package org.moera.node.auth;

public class InvalidCarteException extends RuntimeException {

    private String errorCode;

    public InvalidCarteException(String errorCode) {
        super("Invalid carte: " + errorCode);
        this.errorCode = errorCode;
    }

    public InvalidCarteException(String errorCode, Throwable e) {
        super("Invalid carte: " + errorCode, e);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

}
