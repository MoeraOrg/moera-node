package org.moera.node.util;

public class LockUnderflowException extends RuntimeException {

    public LockUnderflowException(String key) {
        super("Lock underflow for key %s".formatted(key));
    }

}
