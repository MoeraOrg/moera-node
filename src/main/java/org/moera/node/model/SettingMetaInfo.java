package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.option.OptionDescriptor;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SettingMetaInfo {

    private String name;
    private String type;
    private String defaultValue;
    private boolean privileged;
    private String title;
    private SettingTypeModifiers modifiers;

    public SettingMetaInfo() {
    }

    public SettingMetaInfo(OptionDescriptor descriptor) {
        name = descriptor.getName();
        type = descriptor.getType();
        defaultValue = descriptor.getDefaultValue();
        privileged = descriptor.isPrivileged();
        title = descriptor.getTitle();
        modifiers = new SettingTypeModifiers(descriptor.getModifiers());
    }

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

    public SettingTypeModifiers getModifiers() {
        return modifiers;
    }

    public void setModifiers(SettingTypeModifiers modifiers) {
        this.modifiers = modifiers;
    }

}
