package org.moera.node.option.exception;

public class DeserializeOptionValueException extends OptionValueException {

    public DeserializeOptionValueException(String typeName, String value) {
        super("Invalid value of type '%s' for option: %s".formatted(typeName, value), "setting.deserialization-failed");
    }

}
