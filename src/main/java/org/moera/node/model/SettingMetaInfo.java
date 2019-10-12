package org.moera.node.model;

import org.moera.node.option.OptionDescriptor;

public class SettingMetaInfo {

    private String name;
    private String type;
    private String defaultValue;
    private String title;
    private SettingTypeModifiers modifiers;

    public SettingMetaInfo() {
    }

    public SettingMetaInfo(OptionDescriptor descriptor) {
        name = descriptor.getName();
        type = descriptor.getType();
        defaultValue = descriptor.getDefaultValue();
        title = descriptor.getTitle();
        if ("int".equalsIgnoreCase(type) || "Duration".equalsIgnoreCase(type)) {
            modifiers = new SettingTypeModifiers(descriptor.getModifiers());
        }
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
