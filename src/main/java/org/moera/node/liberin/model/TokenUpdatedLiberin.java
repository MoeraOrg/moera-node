package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.data.Token;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.TokenInfo;

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

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("token", new TokenInfo(token, false));
    }

}
