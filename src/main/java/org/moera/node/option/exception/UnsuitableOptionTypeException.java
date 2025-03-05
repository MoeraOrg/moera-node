package org.moera.node.option.exception;

public class UnsuitableOptionTypeException extends OptionValueException {

    public UnsuitableOptionTypeException(String optionType, Class<?> valueType) {
        super(
            "Value of type %s is unsuitable for option of type '%s'".formatted(valueType.getSimpleName(), optionType),
            "setting.invalid-value"
        );
    }

}
