package org.moera.node.option;

public interface OptionConsumer {

    void consume(String name, Object value, OptionTypeBase optionType);

}
