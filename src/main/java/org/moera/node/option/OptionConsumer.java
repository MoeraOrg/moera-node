package org.moera.node.option;

import jakarta.annotation.Nonnull;

import org.moera.node.option.type.OptionTypeBase;

public interface OptionConsumer {

    void consume(String name, Object value, @Nonnull OptionTypeBase optionType);

}
