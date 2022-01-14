package org.moera.node.ui.helper;

public class MissingArgumentException extends RuntimeException {

    public MissingArgumentException(String paramName) {
        super(getMessageText(paramName));
    }

    public MissingArgumentException(String paramName, Throwable cause) {
        super(getMessageText(paramName), cause);
    }

    private static String getMessageText(String paramName) {
        return String.format("Missing required parameter '%s'", paramName);
    }

}
