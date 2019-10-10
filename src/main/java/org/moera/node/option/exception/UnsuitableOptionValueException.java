package org.moera.node.option.exception;

public class UnsuitableOptionValueException extends OptionValueException {

    public UnsuitableOptionValueException(String optionType, Class<?> valueType) {
        super(String.format("Value of type %s is unsuitable for option of type '%s'",
                valueType.getSimpleName(), optionType), "setting.invalid-value");
    }

}
