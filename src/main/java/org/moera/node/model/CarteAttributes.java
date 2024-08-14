package org.moera.node.model;

import java.util.List;

public class CarteAttributes {

    private List<String> clientScope;

    private List<String> adminScope;

    private String nodeName;

    private Integer limit;

    public CarteAttributes() {
    }

    public List<String> getClientScope() {
        return clientScope;
    }

    public void setClientScope(List<String> clientScope) {
        this.clientScope = clientScope;
    }

    public List<String> getAdminScope() {
        return adminScope;
    }

    public void setAdminScope(List<String> adminScope) {
        this.adminScope = adminScope;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

}
