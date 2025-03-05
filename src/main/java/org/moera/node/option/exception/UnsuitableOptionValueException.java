package org.moera.node.option.exception;

public class UnsuitableOptionValueException extends OptionValueException {

    public UnsuitableOptionValueException(String value) {
        super("Value '%s' is unsuitable for the option".formatted(value), "setting.invalid-value");
    }

}
