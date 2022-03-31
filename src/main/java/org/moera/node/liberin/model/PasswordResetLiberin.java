package org.moera.node.liberin.model;

import org.moera.node.liberin.Liberin;

public class PasswordResetLiberin extends Liberin {

    private String token;

    public PasswordResetLiberin(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

}
