package org.moera.node.option;

public class TransactionAbsentException extends RuntimeException {

    public TransactionAbsentException() {
        super("Options transaction is absent");
    }

}
