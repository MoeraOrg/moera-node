package org.moera.node.option.exception;

public class TransactionAbsentException extends RuntimeException {

    public TransactionAbsentException() {
        super("Options transaction is absent");
    }

}
