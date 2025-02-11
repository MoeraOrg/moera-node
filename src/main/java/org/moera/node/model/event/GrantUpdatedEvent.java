package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.Scope;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.principal.Principal;
import org.moera.node.model.GrantInfo;
import org.springframework.data.util.Pair;

public class GrantUpdatedEvent extends Event {

    private GrantInfo grant;

    public GrantUpdatedEvent(GrantInfo grant) {
        super(EventType.GRANT_UPDATED, Scope.OTHER, Principal.ADMIN);
        this.grant = grant;
    }

    public GrantInfo getGrant() {
        return grant;
    }

    public void setGrant(GrantInfo grant) {
        this.grant = grant;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("nodeName", LogUtil.format(grant.getNodeName())));
        parameters.add(Pair.of("scope", LogUtil.format(String.join(",", grant.getScope()))));
    }

}
