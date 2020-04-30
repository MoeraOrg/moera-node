package org.moera.node.notification.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class Notification {

    @JsonIgnore
    private String nodeName;

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

}
