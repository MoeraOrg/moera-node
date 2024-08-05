package org.moera.node.model.event;

import java.util.List;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Scope;
import org.moera.node.auth.principal.Principal;
import org.springframework.data.util.Pair;

public class TokenDeletedEvent extends Event {

    private String id;

    public TokenDeletedEvent(String id) {
        super(EventType.TOKEN_DELETED, Scope.TOKENS, Principal.ADMIN);
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("id", LogUtil.format(id)));
    }

}
