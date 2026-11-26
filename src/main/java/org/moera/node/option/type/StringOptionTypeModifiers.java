package org.moera.node.option.type;

import java.util.List;

import org.moera.lib.node.types.SettingValueChoice;

public class StringOptionTypeModifiers {

    private boolean multiline;
    private List<SettingValueChoice> items;
    private Boolean nodeName;
    private String separator;

    public boolean isMultiline() {
        return multiline;
    }

    public void setMultiline(boolean multiline) {
        this.multiline = multiline;
    }

    public List<SettingValueChoice> getItems() {
        return items;
    }

    public void setItems(List<SettingValueChoice> items) {
        this.items = items;
    }

    public Boolean getNodeName() {
        return nodeName;
    }

    public void setNodeName(Boolean nodeName) {
        this.nodeName = nodeName;
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

}
