package org.moera.node.option;

public class InvalidOptionTypeException extends RuntimeException {

    public InvalidOptionTypeException(String optionName, String correctType, String askedType) {
        super(String.format("Option %s has type '%s', but '%s' was asked", optionName, correctType, askedType));
    }

}
