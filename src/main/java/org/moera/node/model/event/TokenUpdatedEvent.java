package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.TokenInfo;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.util.LogUtil;
import org.springframework.data.util.Pair;

public class TokenUpdatedEvent extends Event {

    private TokenInfo token;

    public TokenUpdatedEvent(TokenInfo token) {
        super(EventType.TOKEN_UPDATED, Scope.TOKENS, Principal.ADMIN);
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
