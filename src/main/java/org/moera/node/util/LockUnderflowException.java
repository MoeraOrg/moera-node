package org.moera.node.util;

public class LockUnderflowException extends RuntimeException {

    public LockUnderflowException(String key) {
        super(String.format("Lock underflow for key %s", key));
    }

}
