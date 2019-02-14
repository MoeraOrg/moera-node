package org.moera.node.model;

import org.moera.node.data.Token;

public class TokenInfo {

    private String token;
    private boolean valid;
    private String[] permissions;

    public TokenInfo() {
    }

    public TokenInfo(String token, boolean valid) {
        this.token = token;
        this.valid = valid;
    }

    public TokenInfo(Token tokenData) {
        token = tokenData.getToken();
        valid = true;
        permissions = tokenData.isAdmin() ? new String[] {"admin"} : new String[0];
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String[] getPermissions() {
        return permissions;
    }

    public void setPermissions(String[] permissions) {
        this.permissions = permissions;
    }

}
