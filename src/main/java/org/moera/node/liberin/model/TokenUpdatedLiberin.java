package org.moera.node.liberin.model;

import org.moera.node.data.Token;
import org.moera.node.liberin.Liberin;

public class TokenUpdatedLiberin extends Liberin {

    private Token token;

    public TokenUpdatedLiberin(Token token) {
        this.token = token;
    }

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
    }

}
