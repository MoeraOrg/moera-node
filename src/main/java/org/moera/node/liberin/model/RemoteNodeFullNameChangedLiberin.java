package org.moera.node.liberin.model;

import org.moera.node.liberin.Liberin;

public class RemoteNodeFullNameChangedLiberin extends Liberin {

    private String nodeName;
    private String fullName;

    public RemoteNodeFullNameChangedLiberin(String nodeName, String fullName) {
        this.nodeName = nodeName;
        this.fullName = fullName;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

}
