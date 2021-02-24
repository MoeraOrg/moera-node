package org.moera.node.model.event;

public class RemoteNodeFullNameChangedEvent extends Event {

    private String name;
    private String fullName;

    public RemoteNodeFullNameChangedEvent() {
        super(EventType.REMOTE_NODE_FULL_NAME_CHANGED);
    }

    public RemoteNodeFullNameChangedEvent(String name, String fullName) {
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
