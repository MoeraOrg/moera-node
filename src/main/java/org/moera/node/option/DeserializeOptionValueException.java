package org.moera.node.option;

public class DeserializeOptionValueException extends OptionValueException {

    public DeserializeOptionValueException(String message) {
        super(message, "setting.deserialization-failed");
    }

}
