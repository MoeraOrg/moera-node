package org.moera.node.option;

import java.util.UUID;

public class OptionValueChange {

    private UUID nodeId;
    private String name;
    private Object previousValue;
    private Object newValue;

    public OptionValueChange(UUID nodeId, String name, Object previousValue, Object newValue) {
        this.nodeId = nodeId;
        this.name = name;
        this.previousValue = previousValue;
        this.newValue = newValue;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getPreviousValue() {
        return previousValue;
    }

    public void setPreviousValue(Object previousValue) {
        this.previousValue = previousValue;
    }

    public Object getNewValue() {
        return newValue;
    }

    public void setNewValue(Object newValue) {
        this.newValue = newValue;
    }

    public boolean isTangible() {
        return !previousValue.equals(newValue);
    }

}
