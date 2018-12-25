package org.moera.node.option;

public class InvalidOptionTypeConversionException extends RuntimeException {

    public InvalidOptionTypeConversionException(String optionType, Class<?> askedType) {
        super(String.format("Value of option of type '%s' cannot be converted to %s",
                optionType, askedType.getSimpleName()));
    }

}
