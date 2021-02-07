package org.moera.node.model.event;

public class NodeNameChangedEvent extends Event {

    private String name;
    private String fullName;

    public NodeNameChangedEvent() {
        super(EventType.NODE_NAME_CHANGED);
    }

    public NodeNameChangedEvent(String name, String fullName) {
        this();
        this.name = name;
        this.fullName = fullName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

}
