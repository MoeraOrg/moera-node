package org.moera.node.liberin.model;

import org.moera.node.liberin.Liberin;

public class RemoteNodeFullNameChangedLiberin extends Liberin {

    private String nodeName;
    private String fullName;
    private String nodeSourceUri;
    private String title;

    public RemoteNodeFullNameChangedLiberin(String nodeName, String fullName, String nodeSourceUri, String title) {
        this.nodeName = nodeName;
        this.fullName = fullName;
        this.nodeSourceUri = nodeSourceUri;
        this.title = title;
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

    public String getNodeSourceUri() {
        return nodeSourceUri;
    }

    public void setNodeSourceUri(String nodeSourceUri) {
        this.nodeSourceUri = nodeSourceUri;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

}
