package org.moera.node.option.exception;

public class UnsuitableOptionTypeException extends OptionValueException {

    public UnsuitableOptionTypeException(String optionType, Class<?> valueType) {
        super(String.format("Value of type %s is unsuitable for option of type '%s'",
                valueType.getSimpleName(), optionType), "setting.invalid-value");
    }

}
