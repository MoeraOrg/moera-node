package org.moera.node.option;

public class UnsuitableOptionValueException extends RuntimeException {

    public UnsuitableOptionValueException(String optionType, Class<?> valueType) {
        super(String.format("Value of type %s is unsuitable for option of type '%s'",
                valueType.getSimpleName(), optionType));
    }

}
