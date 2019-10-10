package org.moera.node.option.exception;

public class DeserializeOptionValueException extends OptionValueException {

    public DeserializeOptionValueException(String typeName, String value) {
        super(String.format("Invalid value of type '%s' for option: %s", typeName, value),
                "setting.deserialization-failed");
    }

}
