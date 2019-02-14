package org.moera.node.model;

public class TokenCreated {

    private String token;
    private String[] permissions;

    public TokenCreated(String token) {
        this.token = token;
    }

    public TokenCreated(String token, String... permissions) {
        this.token = token;
        this.permissions = permissions;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String[] getPermissions() {
        return permissions;
    }

    public void setPermissions(String[] permissions) {
        this.permissions = permissions;
    }

}
