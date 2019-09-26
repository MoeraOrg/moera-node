package org.moera.node.option;

public interface OptionMapper<T> {

    T map(Object value, OptionTypeBase optionType);

}
