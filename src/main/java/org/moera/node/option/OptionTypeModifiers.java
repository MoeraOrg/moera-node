package org.moera.node.option;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OptionTypeModifiers {

    private String format;
    private String min;
    private String max;
    private String multiline;
    private String never;
    private String always;
    private String[] principals;

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

    public String getMultiline() {
        return multiline;
    }

    public void setMultiline(String multiline) {
        this.multiline = multiline;
    }

    public String getNever() {
        return never;
    }

    public void setNever(String never) {
        this.never = never;
    }

    public String getAlways() {
        return always;
    }

    public void setAlways(String always) {
        this.always = always;
    }

    public String[] getPrincipals() {
        return principals;
    }

    public void setPrincipals(String[] principals) {
        this.principals = principals;
    }

}
