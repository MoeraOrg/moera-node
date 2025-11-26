package org.moera.node.liberin.model;

import org.moera.node.liberin.Liberin;

public class EmailVerificationLiberin extends Liberin {

    private String nodeName;
    private String token;

    public EmailVerificationLiberin(String nodeName, String token) {
        this.nodeName = nodeName;
        this.token = token;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

}
