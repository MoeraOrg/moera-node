package org.moera.node.event.model;

public class RegisteredNameChangedEvent extends Event {

    private String name;

    public RegisteredNameChangedEvent() {
        super(EventType.REGISTERED_NAME_CHANGED);
    }

    public RegisteredNameChangedEvent(String name) {
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
