package org.moera.node.operations;

public class ContactUpdateException extends RuntimeException {

    public ContactUpdateException() {
        super("Contact updating error");
    }

    public ContactUpdateException(Throwable cause) {
        super("Contact updating error", cause);
    }

}
