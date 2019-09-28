package org.moera.node.option;

import javax.annotation.Nonnull;

public interface OptionConsumer {

    void consume(String name, Object value, @Nonnull OptionTypeBase optionType);

}
