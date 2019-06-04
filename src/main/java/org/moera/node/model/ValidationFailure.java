package org.moera.node.model;

public class ValidationFailure extends OperationFailure {

    public ValidationFailure(String errorCode) {
        super("Validation failed", errorCode);
    }

}
