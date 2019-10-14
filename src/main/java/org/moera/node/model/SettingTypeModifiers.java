package org.moera.node.model;

import org.moera.node.option.OptionTypeModifiers;

public class SettingTypeModifiers {

    private String min;
    private String max;

    public SettingTypeModifiers() {
    }

    public SettingTypeModifiers(OptionTypeModifiers modifiers) {
        min = modifiers.getMin();
        max = modifiers.getMax();
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

}