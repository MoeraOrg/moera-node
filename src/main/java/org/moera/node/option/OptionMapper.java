package org.moera.node.option;

import org.moera.node.option.type.OptionTypeBase;

public interface OptionMapper<T> {

    T map(Object value, OptionTypeBase optionType);

}
