package org.moera.node.mail.exception;

public class MailServiceException extends Exception {

    public MailServiceException() {
    }

    public MailServiceException(String message) {
        super(message);
    }

    public MailServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public MailServiceException(Throwable cause) {
        super(cause);
    }

}
