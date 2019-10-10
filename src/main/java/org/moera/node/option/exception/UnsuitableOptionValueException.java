package org.moera.node.option.exception;

public class UnsuitableOptionValueException extends OptionValueException {

    public UnsuitableOptionValueException(String value) {
        super(String.format("Value '%s' is unsuitable for the option", value), "setting.invalid-value");
    }

}
