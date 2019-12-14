package org.moera.node.event.model;

public class NodeNameChangedEvent extends Event {

    private String name;

    public NodeNameChangedEvent() {
        super(EventType.NODE_NAME_CHANGED);
    }

    public NodeNameChangedEvent(String name) {
        this();
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
