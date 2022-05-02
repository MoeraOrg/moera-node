package org.moera.node.model;

import org.moera.node.auth.AuthCategory;
import org.moera.node.data.Token;

public class TokenCreated {

    private String token;
    private String[] permissions;

    public TokenCreated(Token tokenData) {
        this.token = tokenData.getToken();
        this.permissions = AuthCategory.toStrings(tokenData.getAuthCategory());
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
