package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.option.OptionTypeModifiers;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SettingTypeModifiers {

    private String format;
    private String min;
    private String max;
    private Boolean multiline;
    private Boolean never;
    private Boolean always;

    public SettingTypeModifiers() {
    }

    public SettingTypeModifiers(OptionTypeModifiers modifiers) {
        format = modifiers.getFormat();
        min = modifiers.getMin();
        max = modifiers.getMax();
        multiline = Util.toBoolean(modifiers.getMultiline());
        never = Util.toBoolean(modifiers.getNever());
        always = Util.toBoolean(modifiers.getAlways());
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
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

    public Boolean getMultiline() {
        return multiline;
    }

    public void setMultiline(Boolean multiline) {
        this.multiline = multiline;
    }

    public Boolean getNever() {
        return never;
    }

    public void setNever(Boolean never) {
        this.never = never;
    }

    public Boolean getAlways() {
        return always;
    }

    public void setAlways(Boolean always) {
        this.always = always;
    }

}
