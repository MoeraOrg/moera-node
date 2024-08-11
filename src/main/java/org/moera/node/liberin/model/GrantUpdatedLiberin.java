package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.liberin.Liberin;

public class GrantUpdatedLiberin extends Liberin {

    private String nodeName;
    private long scope;

    public GrantUpdatedLiberin(String nodeName, long scope) {
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

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("nodeName", nodeName);
        model.put("scope", scope);
    }

}
