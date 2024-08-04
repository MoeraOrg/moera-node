package org.moera.node.model.event;

import java.util.List;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Scope;
import org.moera.node.auth.principal.Principal;
import org.moera.node.model.TokenInfo;
import org.springframework.data.util.Pair;

public class TokenUpdatedEvent extends Event {

    private TokenInfo token;

    public TokenUpdatedEvent(TokenInfo token) {
        super(EventType.TOKEN_UPDATED, Scope.MANAGE_TOKENS, Principal.ADMIN);
        this.token = token;
    }

    public TokenInfo getToken() {
        return token;
    }

    public void setToken(TokenInfo token) {
        this.token = token;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("id", LogUtil.format(token.getId())));
    }

}
