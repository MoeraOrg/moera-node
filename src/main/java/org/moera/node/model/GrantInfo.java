package org.moera.node.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.auth.Scope;
import org.moera.node.data.Grant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GrantInfo {

    private String nodeName;
    private List<String> scope;

    public GrantInfo() {
    }

    public GrantInfo(String nodeName, long scope) {
        this.nodeName = nodeName;
        this.scope = Scope.toValues(scope);
    }

    public GrantInfo(Grant grant) {
        this.nodeName = grant.getNodeName();
        this.scope = Scope.toValues(grant.getAuthScope());
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public List<String> getScope() {
        return scope;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
    }

}
