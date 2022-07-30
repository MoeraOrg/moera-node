package org.moera.node.option;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OptionDescriptor {

    private String name;
    private String type;
    private String defaultValue;
    private boolean internal;
    private boolean privileged;
    private String title;
    private OptionTypeModifiers modifiers = new OptionTypeModifiers();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    public boolean isPrivileged() {
        return privileged;
    }

    public void setPrivileged(boolean privileged) {
        this.privileged = privileged;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public OptionTypeModifiers getModifiers() {
        return modifiers;
    }

    public void setModifiers(OptionTypeModifiers modifiers) {
        this.modifiers = modifiers;
    }

}
