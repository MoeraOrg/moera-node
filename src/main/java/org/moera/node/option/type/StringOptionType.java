package org.moera.node.option.type;

@OptionType("string")
public class StringOptionType extends OptionTypeBase {

    @Override
    public String getString(Object value) {
        return (String) value;
    }

    public Object accept(Object value) {
        return value.toString();
    }

}
