package org.moera.node.option.exception;

public class DeserializeOptionValueException extends OptionValueException {

    public DeserializeOptionValueException(String message) {
        super(message, "setting.deserialization-failed");
    }

}
