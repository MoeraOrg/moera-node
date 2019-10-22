package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.option.OptionTypeModifiers;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SettingTypeModifiers {

    private String min;
    private String max;
    private String multiline;

    public SettingTypeModifiers() {
    }

    public SettingTypeModifiers(OptionTypeModifiers modifiers) {
        min = modifiers.getMin();
        max = modifiers.getMax();
        multiline = modifiers.getMultiline();
    }

    public String getMin() {
        return min;
    }

    public void setMin(String min) {
        this.min = min;
    }

    public String getMax() {
        return max;
    }

    public void setMax(String max) {
        this.max = max;
    }

    public String getMultiline() {
        return multiline;
    }

    public void setMultiline(String multiline) {
        this.multiline = multiline;
    }

}
