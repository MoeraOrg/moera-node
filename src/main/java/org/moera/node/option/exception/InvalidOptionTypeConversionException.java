package org.moera.node.option.exception;

public class InvalidOptionTypeConversionException extends OptionValueException {

    public InvalidOptionTypeConversionException(String optionType, Class<?> askedType) {
        super(
            "Value of option of type '%s' cannot be converted to %s".formatted(optionType, askedType.getSimpleName()),
            "setting.cannot-convert"
        );
    }

}
