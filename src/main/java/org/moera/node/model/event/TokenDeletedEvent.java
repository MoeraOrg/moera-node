package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.util.LogUtil;

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
    public void logParameters(List<LogParameter> parameters) {
        super.logParameters(parameters);
        parameters.add(new LogParameter("id", LogUtil.format(id)));
    }

}
