package org.moera.node.model;

public class SettingInfo {

    private String name;
    private String value;

    public SettingInfo() {
    }

    public SettingInfo(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
