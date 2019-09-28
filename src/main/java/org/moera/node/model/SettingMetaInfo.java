package org.moera.node.model;

import org.moera.node.option.OptionDescriptor;

public class SettingMetaInfo {

    private String name;
    private String type;
    private String defaultValue;

    public SettingMetaInfo() {
    }

    public SettingMetaInfo(OptionDescriptor descriptor) {
        name = descriptor.getName();
        type = descriptor.getType();
        defaultValue = descriptor.getDefaultValue();
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

}
