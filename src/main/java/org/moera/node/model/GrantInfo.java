package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GrantInfo {

    private String nodeName;
    private long scope;

    public GrantInfo() {
    }

    public GrantInfo(String nodeName, long scope) {
        this.nodeName = nodeName;
        this.scope = scope;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public long getScope() {
        return scope;
    }

    public void setScope(long scope) {
        this.scope = scope;
    }

}
