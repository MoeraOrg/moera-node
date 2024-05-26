package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Result {

    public static final Result OK = new Result("ok", "OK");
    public static final Result FROZEN = new Result("frozen", "Frozen");

    private String errorCode;
    private String message;

    public Result() {
    }

    public Result(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @JsonIgnore
    public boolean isOk() {
        return errorCode.equals("ok");
    }

    @JsonIgnore
    public boolean isFrozen() {
        return errorCode.equals("frozen");
    }

}
