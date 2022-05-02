package org.moera.node.model;

import org.moera.node.auth.AuthCategory;
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
        permissions = AuthCategory.toStrings(tokenData.getAuthCategory());
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
