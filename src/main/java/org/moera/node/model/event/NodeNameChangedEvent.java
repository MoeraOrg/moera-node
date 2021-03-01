package org.moera.node.model.event;

import org.moera.node.option.Options;

public class NodeNameChangedEvent extends Event {

    private String name;
    private String fullName;
    private String gender;
    private String title;

    public NodeNameChangedEvent() {
        super(EventType.NODE_NAME_CHANGED);
    }

    public NodeNameChangedEvent(String name, Options options) {
        this();
        this.name = name;
        fullName = options.getString("profile.full-name");
        gender = options.getString("profile.gender");
        title = options.getString("profile.title");
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

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

}
