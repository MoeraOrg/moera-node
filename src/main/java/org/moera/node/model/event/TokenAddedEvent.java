package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.util.LogUtil;
import org.moera.node.model.TokenInfo;
import org.springframework.data.util.Pair;

public class TokenAddedEvent extends Event {

    private TokenInfo token;

    public TokenAddedEvent(TokenInfo token) {
        super(EventType.TOKEN_ADDED, Scope.TOKENS, Principal.ADMIN);
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
